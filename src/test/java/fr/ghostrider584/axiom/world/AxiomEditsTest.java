package fr.ghostrider584.axiom.world;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.network.packet.client.SetBufferMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static net.minestom.server.network.NetworkBuffer.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AxiomEditsTest {
    @BeforeAll static void start() { MinecraftServer.init(); }
    @AfterAll static void stop() { MinecraftServer.stopCleanly(); }
    @AfterEach void reset() {
        AxiomEdits.setTargetResolver((_, _) -> new ScheduledEditTarget());
        AxiomEdits.setChannelPolicy((_, _) -> true);
        AxiomPermissions.setPermissionPredicate((_, _) -> true);
    }
    @Test void capturesTargetAndCancelsEvenAfterReturningToTheSameWorld() throws Exception {
        var player = player(); var target = player.getInstance();
        var captured = new CompletableFuture<EditTarget.Request>();
        var pending = new CompletableFuture<Void>();
        AxiomEdits.setTargetResolver((_, instance) -> (request, _) -> { captured.complete(request); return pending; });
        var done = AxiomEdits.submit(player, empty(target.getDimensionName())).toCompletableFuture();
        try {
            var request = captured.get(3, TimeUnit.SECONDS);
            assertSame(target, request.instance()); request.requireCurrent();
            AxiomEdits.invalidate(player);
            when(player.getInstance()).thenReturn(mock(Instance.class));
            assertThrows(IllegalStateException.class, request::requireCurrent);
            when(player.getInstance()).thenReturn(target);
            assertThrows(IllegalStateException.class, request::requireCurrent);
        } finally { pending.complete(null); done.get(3, TimeUnit.SECONDS); AxiomMinestom.unregisterAxiomPlayer(player); }
    }
    @Test void rechecksNbtPermissionAndDoesNotReleaseBudgetWhenAnObserverCancels() throws Exception {
        var player = player(); var captured = new CompletableFuture<EditTarget.Request>();
        var pending = new CompletableFuture<Void>(); var allowNbt = new AtomicBoolean(true);
        AxiomPermissions.setPermissionPredicate((_, permission) -> permission != AxiomPermission.BUILD_NBT || allowNbt.get());
        AxiomEdits.setTargetResolver((_, _) -> (request, _) -> { captured.complete(request); return pending; });
        var observer = AxiomEdits.submit(player, empty("minecraft:overworld")).toCompletableFuture();
        var queued = new java.util.ArrayList<CompletableFuture<Void>>();
        try {
            var request = captured.get(3, TimeUnit.SECONDS);
            observer.cancel(false);
            for (int i = 1; i < AxiomEdits.MAX_PER_PLAYER; i++)
                queued.add(AxiomEdits.submit(player, empty("minecraft:overworld")).toCompletableFuture());
            assertThrows(CompletionException.class, () -> AxiomEdits.submit(player, empty("minecraft:overworld")).toCompletableFuture().join());
            allowNbt.set(false);
            assertThrows(IllegalStateException.class, request::requireCurrent);
        } finally {
            AxiomEdits.invalidate(player);
            pending.complete(null);
            CompletableFuture.allOf(queued.toArray(CompletableFuture[]::new)).handle((_, _) -> null).get(3, TimeUnit.SECONDS);
            AxiomMinestom.unregisterAxiomPlayer(player);
        }
    }
    @Test void rapidEditsAreAppliedInArrivalOrderAndFailureDoesNotStrandTheQueue() throws Exception {
        var player = player(); var started = new java.util.LinkedHashMap<Integer, CompletableFuture<Void>>();
        var applied = new LinkedBlockingQueue<Integer>();
        var sequence = new AtomicInteger();
        AxiomEdits.setTargetResolver((_, _) -> {
            int id = sequence.getAndIncrement();
            var result = new CompletableFuture<Void>(); started.put(id, result);
            return (_, _) -> { applied.add(id); return result; };
        });
        var results = new java.util.ArrayList<CompletableFuture<Void>>();
        try {
            for (int i = 0; i < 12; i++) results.add(AxiomEdits.submit(player, empty("minecraft:overworld")).toCompletableFuture());
            for (int i = 0; i < 12; i++) {
                assertEquals(i, applied.poll(3, TimeUnit.SECONDS));
                assertTrue(applied.isEmpty(), "A later edit must wait for actual application, not only decoding");
                if (i == 3) started.get(i).completeExceptionally(new IllegalStateException("edit rejected"));
                else started.get(i).complete(null);
            }
            for (int i = 0; i < 12; i++) {
                if (i == 3) assertThrows(ExecutionException.class, () -> results.get(3).get(3, TimeUnit.SECONDS));
                else results.get(i).get(3, TimeUnit.SECONDS);
            }
        } finally {
            AxiomEdits.invalidate(player); started.values().forEach(f -> f.complete(null));
            CompletableFuture.allOf(results.toArray(CompletableFuture[]::new)).handle((_, _) -> null).get(3, TimeUnit.SECONDS);
            AxiomMinestom.unregisterAxiomPlayer(player);
        }
    }
    @Test void queuedEditsCannotFollowAPlayerBackIntoAnOldWorld() throws Exception {
        var player = player(); var calls = new AtomicInteger(); var ready = new CompletableFuture<Void>();
        var pending = new CompletableFuture<Void>();
        AxiomEdits.setTargetResolver((_, _) -> (_, _) -> { calls.incrementAndGet(); ready.complete(null); return pending; });
        var first = AxiomEdits.submit(player, empty("minecraft:overworld")).toCompletableFuture();
        var second = AxiomEdits.submit(player, empty("minecraft:overworld")).toCompletableFuture();
        try {
            ready.get(3, TimeUnit.SECONDS); AxiomEdits.invalidate(player); pending.complete(null);
            first.get(3, TimeUnit.SECONDS);
            assertThrows(ExecutionException.class, () -> second.get(3, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
        } finally { pending.complete(null); AxiomMinestom.unregisterAxiomPlayer(player); }
    }
    @Test void rejectsWrongWorldOrMissingPermissionBeforeCallingTarget() {
        var player = player(); var calls = new AtomicInteger();
        AxiomEdits.setTargetResolver((_, _) -> { calls.incrementAndGet(); return (_, _) -> CompletableFuture.completedFuture(null); });
        try {
            assertThrows(CompletionException.class, () -> AxiomEdits.submit(player, empty("minecraft:the_nether")).toCompletableFuture().join());
            assertEquals(0, calls.get());
            AxiomPermissions.setPermissionPredicate((_, _) -> false);
            assertThrows(CompletionException.class, () -> AxiomEdits.submit(player, empty("minecraft:overworld")).toCompletableFuture().join());
        } finally { AxiomMinestom.unregisterAxiomPlayer(player); }
    }
    private static Player player() {
        var player = mock(Player.class); var instance = mock(Instance.class);
        when(instance.getDimensionName()).thenReturn("minecraft:overworld");
        when(player.getInstance()).thenReturn(instance); when(player.isOnline()).thenReturn(true);
        AxiomMinestom.registerAxiomPlayer(player); return player;
    }
    private static SetBufferMessage empty(String world) {
        var buffer = NetworkBuffer.resizableBuffer(); buffer.write(LONG, EditBufferDecoder.SENTINEL); buffer.write(VAR_INT, 0);
        return new SetBufferMessage(world, java.util.UUID.randomUUID(), (byte) 0, buffer);
    }
}
