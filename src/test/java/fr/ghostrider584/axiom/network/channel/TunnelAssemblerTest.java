package fr.ghostrider584.axiom.network.channel;

import com.github.luben.zstd.Zstd;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import static net.minestom.server.network.NetworkBuffer.*;
import static org.junit.jupiter.api.Assertions.*;

class TunnelAssemblerTest {
    @Test void reassemblesAndInflatesExactly() {
        byte[] data = {1, 2, 3, 4}; byte[] compressed = Zstd.compress(data);
        byte[] envelope = NetworkBuffer.makeArray(buffer -> {
            buffer.write(STRING, "axiom:set_buffer"); buffer.write(INT, data.length); buffer.write(BYTE, (byte) 1);
            for (byte value : compressed) buffer.write(BYTE, value);
        });
        var assembler = new TunnelAssembler<String>();
        assertNull(assembler.accept("a", fragment(1, Arrays.copyOfRange(envelope, 0, 12))));
        var packet = TunnelAssembler.decode(assembler.accept("a", fragment(2, Arrays.copyOfRange(envelope, 12, envelope.length))));
        assertEquals("axiom:set_buffer", packet.channel()); assertArrayEquals(data, packet.payload());
    }
    @Test void rejectsOrphanDuplicateExpiredAndTooManyAssemblies() {
        var clock = new AtomicLong(); var assembler = new TunnelAssembler<String>(clock::get);
        assertThrows(IllegalArgumentException.class, () -> assembler.accept("a", new byte[]{2, 1}));
        assembler.accept("a", new byte[]{1, 1});
        assertThrows(IllegalArgumentException.class, () -> assembler.accept("a", new byte[]{1, 1}));
        assembler.accept("a", new byte[]{1, 1}); clock.set(11_000_000_000L);
        assertThrows(IllegalArgumentException.class, () -> assembler.accept("a", new byte[]{2, 1}));
        for (int i = 0; i < 4; i++) assembler.accept("p" + i, new byte[]{1, 1});
        assertThrows(IllegalStateException.class, () -> assembler.accept("fifth", new byte[]{1, 1}));
    }
    @Test void rejectsWrongExpansionSizeUnknownFlagsAndRecursiveTunnel() {
        for (String channel : new String[]{"axiom:set_buffer", "axiom:tunnel"}) {
            var bytes = NetworkBuffer.makeArray(buffer -> {
                buffer.write(STRING, channel); buffer.write(INT, 10); buffer.write(BYTE, (byte) 0); buffer.write(BYTE, (byte) 1);
            });
            assertThrows(IllegalArgumentException.class, () -> TunnelAssembler.decode(bytes));
        }
        var bomb = NetworkBuffer.makeArray(buffer -> {
            buffer.write(STRING, "axiom:set_buffer"); buffer.write(INT, Integer.MAX_VALUE); buffer.write(BYTE, (byte) 1);
        });
        assertThrows(IllegalArgumentException.class, () -> TunnelAssembler.decode(bomb));
        assertThrows(IllegalArgumentException.class, () -> new TunnelAssembler<>().accept("p", new byte[]{7, 1}));
    }
    @Test void clearsOnTransferAndEnforcesFragmentAndByteBudgets() {
        var assembler = new TunnelAssembler<String>();
        assembler.accept("p", new byte[]{1, 1}); assembler.clear("p");
        assertThrows(IllegalArgumentException.class, () -> assembler.accept("p", new byte[]{2, 1}));
        assembler.accept("p", new byte[]{1, 1});
        for (int i = 1; i < 128; i++) assembler.accept("p", new byte[]{0, 1});
        assertThrows(IllegalArgumentException.class, () -> assembler.accept("p", new byte[]{2, 1}));
        assertThrows(IllegalArgumentException.class, () -> assembler.accept("p", new byte[31_001]));
    }
    private static byte[] fragment(int flags, byte[] data) {
        var result = new byte[data.length + 1]; result[0] = (byte) flags;
        System.arraycopy(data, 0, result, 1, data.length); return result;
    }
}
