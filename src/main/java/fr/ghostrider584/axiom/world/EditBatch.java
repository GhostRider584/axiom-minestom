package fr.ghostrider584.axiom.world;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;

import java.util.List;
import java.util.Map;

/** Immutable, compact decoded edits. A slice may contain skipped cells; no live world is retained. */
public record EditBatch(List<Segment> segments) {
    public EditBatch { segments = List.copyOf(segments); }
    public sealed interface Change permits BlockChange, BiomeChange { BlockVec position(); }
    public record BlockChange(BlockVec position, Block block) implements Change {}
    /** Position is in block coordinates, aligned to the 4x4x4 biome cell. */
    public record BiomeChange(BlockVec position, String biome) implements Change {}
    public sealed interface Segment permits Blocks, Biomes, Individual {
        int size();
        /** null means the client did not edit this cell. */
        Change change(int index);
    }
    public record Individual(List<BlockChange> changes) implements Segment {
        public Individual { changes = List.copyOf(changes); }
        public int size() { return changes.size(); }
        public Change change(int index) { return changes.get(index); }
    }
    public static final class Blocks implements Segment {
        private final int x, y, z;
        private final int[] states;
        private final Map<Integer, CompoundBinaryTag> nbt;
        public Blocks(int x, int y, int z, int[] states, Map<Integer, CompoundBinaryTag> nbt) {
            if (states.length != 4096) throw new IllegalArgumentException("Wrong block section size");
            this.x = x; this.y = y; this.z = z; this.states = states.clone(); this.nbt = Map.copyOf(nbt);
        }
        public int size() { return 4096; }
        public Change change(int index) {
            int state = states[index];
            if (state == Block.VOID_AIR.stateId()) return null;
            int lx = index & 15, ly = index >> 8, lz = (index >> 4) & 15;
            Block block = Block.fromStateId(state).withNbt(nbt.get((lx << 8) | (lz << 4) | ly));
            return new BlockChange(new BlockVec(x * 16 + lx, y * 16 + ly, z * 16 + lz), block);
        }
    }
    public static final class Biomes implements Segment {
        private final int x, y, z;
        private final byte[] indices;
        private final List<String> palette;
        public Biomes(int x, int y, int z, byte[] indices, List<String> palette) {
            if (indices.length != 4096) throw new IllegalArgumentException("Wrong biome section size");
            this.x = x; this.y = y; this.z = z; this.indices = indices.clone(); this.palette = List.copyOf(palette);
        }
        public int size() { return 4096; }
        public Change change(int index) {
            int value = Byte.toUnsignedInt(indices[index]);
            if (value == 0) return null;
            // Axiom's biome map stores 16^3 QUART positions with x, y, z array order.
            return new BiomeChange(new BlockVec((x * 16 + (index & 15)) * 4,
                    (y * 16 + ((index >> 4) & 15)) * 4, (z * 16 + (index >> 8)) * 4), palette.get(value - 1));
        }
    }
}
