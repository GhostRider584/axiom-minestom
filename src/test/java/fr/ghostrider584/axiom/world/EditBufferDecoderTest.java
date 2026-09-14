package fr.ghostrider584.axiom.world;

import fr.ghostrider584.axiom.util.CompressedBlockEntity;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static net.minestom.server.network.NetworkBuffer.*;
import static org.junit.jupiter.api.Assertions.*;

class EditBufferDecoderTest {
    @BeforeAll static void start() { MinecraftServer.init(); }
    @AfterAll static void stop() { MinecraftServer.stopCleanly(); }

    @Test void requiresSentinelCreditAndNoTrailingBytes() {
        var buffer = blockHeader(); buffer.write(VAR_INT, 0);
        var missingSentinel = buffer;
        assertThrows(Exception.class, () -> EditBufferDecoder.decode(missingSentinel, (byte) 0));
        buffer = blockHeader(); buffer.write(VAR_INT, 0); buffer.write(LONG, EditBufferDecoder.SENTINEL);
        var missingCredit = buffer;
        assertThrows(Exception.class, () -> EditBufferDecoder.decode(missingCredit, (byte) 0));
        buffer = blockHeader(); buffer.write(VAR_INT, 0); finish(buffer); buffer.write(BYTE, (byte) 1);
        var trailing = buffer;
        assertThrows(IllegalArgumentException.class, () -> EditBufferDecoder.decode(trailing, (byte) 0));
    }
    @Test void rejectsAllocationBombBeforeReadingPalette() {
        var buffer = NetworkBuffer.resizableBuffer(); buffer.write(LONG, 0L);
        buffer.write(BYTE, (byte) 4); buffer.write(VAR_INT, Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> EditBufferDecoder.decode(buffer, (byte) 0));
    }
    @Test void rejectsNbtBudgetBeforeAllocating() {
        var buffer = blockHeader(); buffer.write(VAR_INT, 1); buffer.write(SHORT, (short) 0);
        buffer.write(VAR_INT, CompressedBlockEntity.MAX_NBT_BYTES + 1);
        assertThrows(IllegalArgumentException.class, () -> EditBufferDecoder.decode(buffer, (byte) 0));
    }
    @Test void rejectsUnknownStatesAndDuplicateSections() {
        var unknown = NetworkBuffer.resizableBuffer(); unknown.write(LONG, 0L);
        unknown.write(BYTE, (byte) 0); unknown.write(VAR_INT, Integer.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> EditBufferDecoder.decode(unknown, (byte) 0));
        var duplicate = blockHeader(); duplicate.write(VAR_INT, 0); duplicate.write(LONG, 0L);
        assertThrows(IllegalArgumentException.class, () -> EditBufferDecoder.decode(duplicate, (byte) 0));
    }
    @Test void rejectsTooManySections() {
        var buffer = NetworkBuffer.resizableBuffer();
        for (int i = 0; i <= EditBufferDecoder.MAX_SECTIONS; i++) {
            buffer.write(LONG, (long) i << 38); buffer.write(BYTE, (byte) 0);
            buffer.write(VAR_INT, Block.AIR.stateId()); buffer.write(VAR_INT, 0);
        }
        finish(buffer);
        assertThrows(IllegalArgumentException.class, () -> EditBufferDecoder.decode(buffer, (byte) 0));
    }
    @Test void decodesBiomeQuartCoordinatesNotBlockSections() {
        var buffer = NetworkBuffer.resizableBuffer(); buffer.write(BYTE, (byte) 1);
        buffer.write(STRING, "minecraft:desert"); buffer.write(BYTE, (byte) 0);
        buffer.write(LONG, (-1L & 0x3ffffffL) << 38 | (2L << 12) | 0xfffL);
        for (int i = 0; i < 4096; i++) buffer.write(BYTE, (byte) (i == (1 + 2 * 16 + 3 * 256) ? 1 : 0));
        finish(buffer);
        var batch = EditBufferDecoder.decode(buffer, (byte) 1);
        var edit = (EditBatch.BiomeChange) batch.segments().getFirst().change(1 + 2 * 16 + 3 * 256);
        assertEquals(-60, edit.position().blockX()); assertEquals(-56, edit.position().blockY());
        assertEquals(140, edit.position().blockZ()); assertEquals("minecraft:desert", edit.biome());
        assertNull(batch.segments().getFirst().change(0));
    }
    @Test void invalidatedSessionStopsFollowingSlicesAndReportsPartialFailure() {
        var instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        try {
            instance.loadChunk(0, 0).join();
            var buffer = blockHeader(); buffer.write(VAR_INT, 0); finish(buffer);
            var batch = EditBufferDecoder.decode(buffer, (byte) 0);
            var ticks = new java.util.ArrayDeque<Runnable>();
            var valid = new AtomicBoolean(true);
            var done = new ScheduledEditTarget(ticks::add).apply(new EditTarget.Request(null, instance,
                    valid::get, true, null), batch).toCompletableFuture();
            ticks.remove().run();
            assertFalse(done.isDone()); assertEquals(Block.STONE, instance.getBlock(0, 0, 0));
            valid.set(false); ticks.remove().run();
            assertThrows(java.util.concurrent.CompletionException.class, done::join);
            assertEquals(Block.AIR, instance.getBlock(0, 15, 0));
        } finally { MinecraftServer.getInstanceManager().unregisterInstance(instance); }
    }
    private static NetworkBuffer blockHeader() {
        var buffer = NetworkBuffer.resizableBuffer(); buffer.write(LONG, 0L);
        buffer.write(BYTE, (byte) 0); buffer.write(VAR_INT, Block.STONE.stateId()); return buffer;
    }
    private static void finish(NetworkBuffer buffer) { buffer.write(LONG, EditBufferDecoder.SENTINEL); buffer.write(VAR_INT, 0); }
}
