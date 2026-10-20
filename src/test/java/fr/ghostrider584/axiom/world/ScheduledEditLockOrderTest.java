package fr.ghostrider584.axiom.world;

import fr.ghostrider584.axiom.network.packet.client.SetBlockMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Timeout(20)
class ScheduledEditLockOrderTest {
    @BeforeEach void start() { MinecraftServer.init(); }
    @AfterEach void stop() { MinecraftServer.stopCleanly(); }

    @ParameterizedTest
    @CsvSource({"true,false", "false,false", "true,true", "false,true"})
    void editWaitingForInstanceDoesNotHoldChunkNeededByLighting(boolean breaking, boolean shared) throws Exception {
        var container = MinecraftServer.getInstanceManager().createInstanceContainer();
        Instance instance = shared ? MinecraftServer.getInstanceManager().createSharedInstance(container) : container;
        var chunk = instance.loadChunk(0, 0).get(5, SECONDS);
        var position = new BlockVec(1, 2, 3);
        var replacement = breaking ? Block.AIR : Block.GLOWSTONE;
        instance.setBlock(position, Block.STONE);
        var calls = new AtomicInteger();
        // Mortar cancels the event and re-enters instance.setBlock from its listener.
        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockBreakEvent.class, event -> {
            event.setCancelled(true);
            instance.setBlock(position, replacement);
            calls.incrementAndGet();
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockPlaceEvent.class, event -> {
            event.setCancelled(true);
            instance.setBlock(position, replacement);
            calls.incrementAndGet();
        });
        var message = new SetBlockMessage(Map.of(position, replacement), false, Set.of(), 0, breaking,
                new SetBlockMessage.BlockHitResult(position, Direction.UP, Vec.ZERO, false, false),
                PlayerHand.MAIN, 1);
        var batch = new EditBatch(List.of(new EditBatch.Individual(
                List.of(new EditBatch.BlockChange(position, replacement)))));
        var ticks = new ArrayDeque<Runnable>();
        var applied = new ScheduledEditTarget(ticks::add).apply(
                new EditTarget.Request(mock(Player.class), instance, () -> true, true, message), batch)
                .toCompletableFuture();

        CompletableFuture<Void> lighting = null;
        try {
            synchronized (container) {
                var edit = Thread.ofPlatform().daemon().start(ticks.remove());
                awaitInstanceMonitor(edit, container);
                // Model the socket writer holding the instance monitor and joining a
                // lighting worker that needs this chunk's read lock. The bounded get
                // lets the original deadlock unwind when the test releases the monitor.
                lighting = CompletableFuture.runAsync(() -> {
                    chunk.lockReadLock();
                    try { chunk.getBlock(1, 2, 3); }
                    finally { chunk.unlockReadLock(); }
                });
                lighting.get(2, SECONDS);
            }
        } finally {
            applied.get(5, SECONDS);
            if (lighting != null) lighting.get(5, SECONDS);
        }
        assertEquals(1, calls.get());
        assertEquals(replacement, instance.getBlock(position));
    }

    private static void awaitInstanceMonitor(Thread edit, Instance instance) {
        var threads = ManagementFactory.getThreadMXBean();
        long deadline = System.nanoTime() + SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var info = threads.getThreadInfo(edit.threadId());
            if (info != null && info.getThreadState() == Thread.State.BLOCKED && info.getLockInfo() != null
                    && info.getLockInfo().getIdentityHashCode() == System.identityHashCode(instance)) return;
            LockSupport.parkNanos(1_000_000);
        }
        fail("Scheduled edit did not block on the instance monitor: " + edit.getState());
    }
}
