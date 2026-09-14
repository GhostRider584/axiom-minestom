package fr.ghostrider584.axiom.world;

import fr.ghostrider584.axiom.util.CompressedBlockEntity;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Map;

import static net.minestom.server.network.NetworkBuffer.*;
import static org.junit.jupiter.api.Assertions.*;

class EditBufferApplicationTest {
    private InstanceContainer instance;
    private static final int OFFSET = (1 << 8) | (3 << 4) | 2;
    private static final CompoundBinaryTag DATA = CompoundBinaryTag.builder().putString("owner", "A").build();

    @BeforeAll static void start() { MinecraftServer.init(); }
    @AfterAll static void stop() { MinecraftServer.stopCleanly(); }
    @BeforeEach void createInstance() { instance = MinecraftServer.getInstanceManager().createInstanceContainer(); }
    @AfterEach void removeInstance() { MinecraftServer.getInstanceManager().unregisterInstance(instance); }

    @Test void nbtBelongsOnlyToItsChunkAndSection() throws IOException {
        instance.loadChunk(-1, -1).join();
        instance.loadChunk(0, -1).join();
        var buffer = NetworkBuffer.resizableBuffer();
        writeSection(buffer, -1, 0, -1, sparseChest(), Map.of(OFFSET, DATA));
        writeSection(buffer, 0, 0, -1, sparseChest(), Map.of());
        writeSection(buffer, -1, 1, -1, sparseChest(), Map.of());
        process(buffer, true);
        assertEquals(DATA, instance.getBlock(-15, 2, -13).nbt());
        assertNull(instance.getBlock(1, 2, -13).nbt());
        assertNull(instance.getBlock(-15, 18, -13).nbt());
    }

    @Test void uniformFillAppliesNbt() throws IOException {
        instance.loadChunk(0, 0).join();
        var palette = Palette.blocks();
        palette.fill(Block.CHEST.stateId());
        apply(palette, true, Map.of(OFFSET, DATA));
        assertEquals(DATA, instance.getBlock(1, 2, 3).nbt());
        assertEquals(Block.CHEST.id(), instance.getBlock(15, 15, 15).id());
    }

    @Test void denseMixedPaletteAppliesNbtAndClearsOldBlockEntityEntries() throws IOException {
        instance.loadChunk(0, 0).join();
        instance.setBlock(4, 5, 6, Block.CHEST.withNbt(DATA));
        var palette = Palette.blocks();
        palette.fill(Block.STONE.stateId());
        palette.set(1, 2, 3, Block.CHEST.stateId());
        apply(palette, true, Map.of(OFFSET, DATA));
        assertEquals(DATA, instance.getBlock(1, 2, 3).nbt());
        assertEquals(Block.STONE, instance.getBlock(4, 5, 6));
    }

    @Test void airIsAnEditButVoidAirIsNot() throws IOException {
        instance.loadChunk(0, 0).join();
        instance.setBlock(1, 2, 3, Block.CHEST.withNbt(DATA));
        var palette = Palette.blocks();
        palette.fill(Block.VOID_AIR.stateId());
        apply(palette, true, Map.of());
        assertEquals(DATA, instance.getBlock(1, 2, 3).nbt());
        palette.fill(Block.AIR.stateId());
        apply(palette, true, Map.of());
        assertEquals(Block.AIR, instance.getBlock(1, 2, 3));
    }

    @Test void deniedNbtIsConsumedWithoutReplacingExistingDataOrMisaligningNextSection() throws IOException {
        instance.loadChunk(0, 0).join();
        instance.loadChunk(1, 0).join();
        instance.setBlock(1, 2, 3, Block.CHEST.withNbt(DATA));
        var buffer = NetworkBuffer.resizableBuffer();
        var forbidden = CompoundBinaryTag.builder().putString("owner", "forbidden").build();
        writeSection(buffer, 0, 0, 0, sparseChest(), Map.of(OFFSET, forbidden));
        writeSection(buffer, 1, 0, 0, sparseChest(), Map.of(OFFSET, forbidden));
        process(buffer, false);
        assertEquals(DATA, instance.getBlock(1, 2, 3).nbt());
        assertEquals(Block.CHEST, instance.getBlock(17, 2, 3));
        assertEquals(0, buffer.readableBytes());
    }

    @Test void sparseReplacementClearsPreviousNbt() throws IOException {
        instance.loadChunk(0, 0).join();
        instance.setBlock(1, 2, 3, Block.CHEST.withNbt(DATA));
        var palette = Palette.blocks();
        palette.fill(Block.VOID_AIR.stateId());
        palette.set(1, 2, 3, Block.STONE.stateId());
        apply(palette, true, Map.of());
        assertEquals(Block.STONE, instance.getBlock(1, 2, 3));
    }

    private void apply(Palette palette, boolean allowNbt, Map<Integer, CompoundBinaryTag> nbt) throws IOException {
        var buffer = NetworkBuffer.resizableBuffer();
        writeSection(buffer, 0, 0, 0, palette, nbt);
        process(buffer, allowNbt);
    }

    private void process(NetworkBuffer buffer, boolean allowNbt) {
        buffer.write(LONG, EditBufferDecoder.SENTINEL);
        buffer.write(VAR_INT, 0);
        var batch = EditBufferDecoder.decode(buffer, (byte) 0);
        var ticks = new java.util.ArrayDeque<Runnable>();
        var done = new ScheduledEditTarget(ticks::add).apply(
                new EditTarget.Request(null, instance, () -> true, allowNbt, null), batch).toCompletableFuture();
        while (!ticks.isEmpty()) ticks.remove().run();
        done.join();
    }

    private static Palette sparseChest() {
        var palette = Palette.blocks();
        palette.fill(Block.VOID_AIR.stateId());
        palette.set(1, 2, 3, Block.CHEST.stateId());
        return palette;
    }

    private static void writeSection(NetworkBuffer buffer, int x, int y, int z, Palette palette,
                                     Map<Integer, CompoundBinaryTag> nbt) throws IOException {
        buffer.write(LONG, ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | y & 0xFFFL);
        buffer.write(Palette.BLOCK_SERIALIZER, palette);
        buffer.write(VAR_INT, nbt.size());
        for (var entry : nbt.entrySet()) {
            var compressed = CompressedBlockEntity.compress(entry.getValue());
            buffer.write(SHORT, entry.getKey().shortValue());
            buffer.write(VAR_INT, compressed.originalSize());
            buffer.write(BYTE, compressed.compressionDict());
            buffer.write(BYTE_ARRAY, compressed.compressedBytes());
        }
    }
}
