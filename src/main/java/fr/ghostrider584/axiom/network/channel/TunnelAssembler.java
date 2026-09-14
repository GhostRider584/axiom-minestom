package fr.ghostrider584.axiom.network.channel;

import com.github.luben.zstd.Zstd;
import fr.ghostrider584.axiom.world.EditBufferDecoder;
import net.minestom.server.network.NetworkBuffer;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.function.LongSupplier;
import static net.minestom.server.network.NetworkBuffer.*;

/** Axiom API 10 fragments. Four assemblies, bounded bytes/fragments and a ten-second lifetime. */
public final class TunnelAssembler<K> {
    private static final int MAX_SIZE = EditBufferDecoder.MAX_WIRE_BYTES + 512;
    private final Map<K, Pending> pending = new HashMap<>();
    private final LongSupplier clock;
    public TunnelAssembler() { this(System::nanoTime); }
    public TunnelAssembler(LongSupplier clock) { this.clock = clock; }
    private static final class Pending {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final long started;
        int fragments;
        Pending(long started) { this.started = started; }
    }
    public synchronized void clear(K key) { pending.remove(key); }
    public synchronized void clear() { pending.clear(); }
    /** null until the final fragment. Returned bytes are owned by the caller. */
    public synchronized byte[] accept(K key, byte[] fragment) {
        long now = clock.getAsLong();
        pending.values().removeIf(item -> now - item.started > 10_000_000_000L);
        try {
            if (fragment.length < 2 || fragment.length > 31_000 || (fragment[0] & ~3) != 0)
                throw new IllegalArgumentException("Invalid Axiom tunnel fragment");
            boolean first = (fragment[0] & 1) != 0, last = (fragment[0] & 2) != 0;
            if (first) {
                if (pending.containsKey(key)) throw new IllegalArgumentException("Previous tunnel message is incomplete");
                if (pending.size() == 4) throw new IllegalStateException("Axiom tunnel is busy");
                pending.put(key, new Pending(now));
            }
            Pending item = pending.get(key);
            if (item == null) throw new IllegalArgumentException("Missing first tunnel fragment");
            if (++item.fragments > 128 || item.bytes.size() + fragment.length - 1 > MAX_SIZE)
                throw new IllegalArgumentException("Axiom tunnel exceeds assembly budget");
            item.bytes.write(fragment, 1, fragment.length - 1);
            if (!last) return null;
            pending.remove(key);
            return item.bytes.toByteArray();
        } catch (RuntimeException e) { pending.remove(key); throw e; }
    }
    public record Packet(String channel, byte[] payload) {}
    /** Run off the tick path. No dictionary is used for tunnel-level compression. */
    public static Packet decode(byte[] bytes) {
        if (bytes.length > MAX_SIZE) throw new IllegalArgumentException("Axiom tunnel exceeds wire budget");
        var buffer = NetworkBuffer.wrap(bytes, 0, bytes.length);
        String channel = buffer.read(STRING);
        if (channel.length() > 128 || !channel.startsWith("axiom:") || channel.equals("axiom:tunnel"))
            throw new IllegalArgumentException("Invalid inner Axiom channel");
        int size = buffer.read(INT), flags = Byte.toUnsignedInt(buffer.read(BYTE));
        if (size < 0 || size > EditBufferDecoder.MAX_WIRE_BYTES || flags > 1)
            throw new IllegalArgumentException("Invalid tunnel expansion size/flags");
        int remaining = Math.toIntExact(buffer.readableBytes());
        byte[] payload = new byte[remaining]; buffer.copyTo(buffer.readIndex(), payload, 0, remaining);
        if (flags == 1) {
            byte[] decoded = new byte[size];
            long length = Zstd.decompressByteArray(decoded, 0, size, payload, 0, payload.length);
            if (Zstd.isError(length) || length != size) throw new IllegalArgumentException("Invalid compressed Axiom payload");
            return new Packet(channel, decoded);
        }
        if (remaining != size) throw new IllegalArgumentException("Wrong Axiom payload size");
        return new Packet(channel, payload);
    }
}
