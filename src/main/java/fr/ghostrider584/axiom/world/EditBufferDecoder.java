package fr.ghostrider584.axiom.world;

import fr.ghostrider584.axiom.util.CompressedBlockEntity;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;

import java.io.IOException;
import java.util.*;

import static net.minestom.server.network.NetworkBuffer.*;

/** Strict 26.2 wire decoder. Validate the entire bounded request before touching any world. */
public final class EditBufferDecoder {
    public static final int MAX_WIRE_BYTES = 2 * 1024 * 1024;
    public static final int MAX_SECTIONS = 32;
    public static final int MAX_TOTAL_NBT_BYTES = 4 * 1024 * 1024;
    public static final long SENTINEL = 0b1000000000000000000000000010000000000000000000000000100000000000L;
    private EditBufferDecoder() {}

    public static EditBatch decode(NetworkBuffer buffer, byte type) {
        if (buffer.readableBytes() > MAX_WIRE_BYTES) throw new IllegalArgumentException("Edit buffer exceeds wire budget");
        if (type != 0 && type != 1) throw new IllegalArgumentException("Unknown buffer type");
        var segments = new ArrayList<EditBatch.Segment>();
        var positions = new HashSet<Long>();
        var biomes = new ArrayList<String>();
        if (type == 1) {
            int count = Byte.toUnsignedInt(buffer.read(BYTE));
            for (int i = 0; i < count; i++) {
                String key = buffer.read(STRING);
                if (key.length() > 256 || !key.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || biomes.contains(key))
                    throw new IllegalArgumentException("Invalid/duplicate biome key");
                biomes.add(key);
            }
            if (buffer.read(BYTE) != 0) throw new IllegalArgumentException("Biome edit default must be unmodified");
        }
        int totalNbt = 0;
        while (true) {
            long section = buffer.read(LONG); // Missing sentinel is malformed, not a successful partial edit.
            if (section == SENTINEL) break;
            if (segments.size() == MAX_SECTIONS || !positions.add(section))
                throw new IllegalArgumentException("Too many or duplicate edit sections");
            int x = (int) (section >> 38), y = (int) (section << 52 >> 52), z = (int) (section << 26 >> 38);
            // Bound before multiplication (biome sections represent 64 block coordinates).
            int scale = type == 0 ? 16 : 64;
            if (Math.abs((long) x * scale) > 29_999_936 || Math.abs((long) z * scale) > 29_999_936)
                throw new IllegalArgumentException("Edit section outside world bounds");
            if (type == 0) {
                int[] states = readPalette(buffer);
                int count = bounded(buffer.read(VAR_INT), 0, 4096, "block entity count");
                var nbt = new HashMap<Integer, CompoundBinaryTag>();
                for (int i = 0; i < count; i++) {
                    int offset = bounded(Short.toUnsignedInt(buffer.read(SHORT)), 0, 4095, "block entity offset");
                    int original = bounded(buffer.read(VAR_INT), 1, CompressedBlockEntity.MAX_NBT_BYTES, "NBT size");
                    totalNbt = Math.addExact(totalNbt, original);
                    if (totalNbt > MAX_TOTAL_NBT_BYTES) throw new IllegalArgumentException("Decoded NBT exceeds request budget");
                    byte dictionary = buffer.read(BYTE);
                    int length = bounded(buffer.read(VAR_INT), 1, CompressedBlockEntity.MAX_NBT_BYTES, "compressed NBT size");
                    if (length > buffer.readableBytes()) throw new IllegalArgumentException("Truncated NBT");
                    byte[] bytes = new byte[length];
                    buffer.copyTo(buffer.readIndex(), bytes, 0, length); buffer.advanceRead(length);
                    try {
                        var value = CompressedBlockEntity.decompress(new CompressedBlockEntity(original, dictionary, bytes));
                        if (nbt.put(offset, value) != null) throw new IllegalArgumentException("Duplicate block entity");
                    } catch (IOException e) { throw new IllegalArgumentException("Invalid block entity", e); }
                }
                segments.add(new EditBatch.Blocks(x, y, z, states, nbt));
            } else {
                if (buffer.readableBytes() < 4096) throw new IllegalArgumentException("Truncated biome section");
                byte[] indices = new byte[4096];
                buffer.copyTo(buffer.readIndex(), indices, 0, 4096); buffer.advanceRead(4096);
                for (byte value : indices) if (Byte.toUnsignedInt(value) > biomes.size())
                    throw new IllegalArgumentException("Unknown biome palette index");
                segments.add(new EditBatch.Biomes(x, y, z, indices, biomes));
            }
        }
        bounded(buffer.read(VAR_INT), 0, Integer.MAX_VALUE, "client dispatch credit");
        if (buffer.readableBytes() != 0) throw new IllegalArgumentException("Trailing edit buffer data");
        return new EditBatch(segments);
    }

    private static int[] readPalette(NetworkBuffer buffer) {
        int bits = Byte.toUnsignedInt(buffer.read(BYTE));
        int[] states = new int[4096];
        if (bits == 0) { Arrays.fill(states, state(buffer.read(VAR_INT))); return states; }
        if ((bits < 4 || bits > 8) && bits != 15) throw new IllegalArgumentException("Invalid block palette bits");
        int[] palette = null;
        if (bits <= 8) {
            int count = bounded(buffer.read(VAR_INT), 1, 1 << bits, "palette size");
            palette = new int[count];
            for (int i = 0; i < count; i++) palette[i] = state(buffer.read(VAR_INT));
        }
        int perLong = 64 / bits, mask = (1 << bits) - 1;
        for (int i = 0; i < 4096;) {
            long packed = buffer.read(LONG);
            for (int j = 0; j < perLong && i < 4096; j++, i++) {
                int value = (int) ((packed >>> (j * bits)) & mask);
                states[i] = palette == null ? state(value) : palette[bounded(value, 0, palette.length - 1, "palette index")];
            }
        }
        return states;
    }
    public static int state(int value) {
        try {
            if (Block.fromStateId(value) == null) throw new IllegalArgumentException("Unknown block state: " + value);
        } catch (IndexOutOfBoundsException e) { throw new IllegalArgumentException("Unknown block state: " + value, e); }
        return value;
    }
    private static int bounded(int value, int min, int max, String label) {
        if (value < min || value > max) throw new IllegalArgumentException("Invalid " + label + ": " + value);
        return value;
    }
}
