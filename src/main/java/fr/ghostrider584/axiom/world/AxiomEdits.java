package fr.ghostrider584.axiom.world;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.network.packet.client.SetBlockMessage;
import fr.ghostrider584.axiom.network.packet.client.SetBufferMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Bounded admission covers decode AND pending application, not merely the decoder queue. */
public final class AxiomEdits {
    private static final ScheduledEditTarget DEFAULT = new ScheduledEditTarget();
    private static final ExecutorService DECODER = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(4), Thread.ofPlatform().daemon().name("axiom-decode-", 0).factory(),
            new ThreadPoolExecutor.AbortPolicy());
    private static final Map<Player, ArrayDeque<Pending>> QUEUES = new IdentityHashMap<>();
    private static final Map<Player, Object> SESSIONS = new WeakHashMap<>();
    private static volatile BiFunction<Player, Instance, EditTarget> targets = (player, instance) -> DEFAULT;
    private static volatile BiPredicate<Player, String> channelPolicy = (player, channel) -> true;
    static final int MAX_PER_PLAYER = 32;
    private static final int MAX_PLAYERS = 4;
    private static final long MAX_PENDING_BYTES = 32L * 1024 * 1024;
    private static long pendingBytes;
    private AxiomEdits() {}

    /** Set at integration startup. Return null to reject; never fall back for an owned world. */
    public static void setTargetResolver(BiFunction<Player, Instance, EditTarget> resolver) { targets = Objects.requireNonNull(resolver); }
    /** Additional host policy for EVERY incoming Axiom channel, before its decoder/handler runs. */
    public static void setChannelPolicy(BiPredicate<Player, String> policy) { channelPolicy = Objects.requireNonNull(policy); }
    public static boolean allowsChannel(Player player, String channel) { return channelPolicy.test(player, channel); }
    /** Call on removal/transfer, disconnect and renewed handshake; old queued requests stay invalid. */
    public static synchronized void invalidate(Player player) { SESSIONS.remove(player); }
    private static synchronized Object session(Player player) { return SESSIONS.computeIfAbsent(player, _ -> new Object()); }
    private static synchronized boolean sessionIs(Player player, Object token) { return SESSIONS.get(player) == token; }
    public static BooleanSupplier captureSession(Player player) {
        var instance = player.getInstance();
        var token = session(player);
        return () -> player.isOnline() && player.getInstance() == instance && sessionIs(player, token)
                && AxiomMinestom.isAxiomPlayer(player);
    }

    public static CompletionStage<Void> submit(Player player, SetBufferMessage packet) {
        return submit(player, AxiomPermission.BUILD_SECTION, packet.worldKey(), null, packet.bufferData().readableBytes(),
                () -> EditBufferDecoder.decode(packet.bufferData(), packet.bufferType()));
    }
    public static CompletionStage<Void> submit(Player player, SetBlockMessage packet) {
        return submit(player, AxiomPermission.BUILD_PLACE, null, packet,
                256L + packet.blocks().size() * 128L + packet.preventUpdatesAt().size() * 32L, () -> new EditBatch(List.of(
                new EditBatch.Individual(packet.blocks().entrySet().stream().map(entry -> new EditBatch.BlockChange(
                        entry.getKey().asBlockVec(), packet.breaking() ? Block.AIR : entry.getValue())).toList()))));
    }
    private static CompletionStage<Void> submit(Player player, AxiomPermission permission, String worldKey,
                                                SetBlockMessage single, long bytes, Supplier<EditBatch> decode) {
        var done = new CompletableFuture<Void>();
        try {
            var instance = Objects.requireNonNull(player.getInstance(), "Player is not in a world");
            if (worldKey != null && !worldKey.equals(instance.getDimensionName()))
                throw new IllegalArgumentException("Edit belongs to a different world");
            String channel = single == null ? "axiom:set_buffer" : "axiom:set_block";
            Object token = session(player);
            var target = Objects.requireNonNull(targets.apply(player, instance), "Editing is not allowed here");
            boolean nbt = AxiomPermissions.hasPermission(player, AxiomPermission.BUILD_NBT);
            var request = new EditTarget.Request(player, instance, () -> player.isOnline()
                    && player.getInstance() == instance && sessionIs(player, token)
                    && AxiomMinestom.isAxiomPlayer(player) && allowsChannel(player, channel)
                    && AxiomPermissions.hasPermission(player, permission)
                    && (!nbt || AxiomPermissions.hasPermission(player, AxiomPermission.BUILD_NBT)), nbt, single);
            request.requireCurrent();
            var pending = new Pending(player, request, target, decode, done, Math.max(256L, bytes));
            boolean first;
            synchronized (QUEUES) {
                var queue = QUEUES.get(player);
                if ((queue == null && QUEUES.size() >= MAX_PLAYERS)
                        || (queue != null && queue.size() >= MAX_PER_PLAYER)
                        || pending.bytes > MAX_PENDING_BYTES - pendingBytes)
                    throw new RejectedExecutionException("Axiom edit queue is full; let the current work finish before adding more");
                if (queue == null) { queue = new ArrayDeque<>(); QUEUES.put(player, queue); }
                first = queue.isEmpty();
                queue.addLast(pending);
                pendingBytes += pending.bytes;
            }
            if (first) pending.start();
        } catch (Exception e) {
            done.completeExceptionally(e);
        }
        return done.minimalCompletionStage();
    }

    /** Only a queue's head is decoded/applied. Ordinary fast clicks queue in order. */
    private record Pending(Player player, EditTarget.Request request, EditTarget target, Supplier<EditBatch> decode,
                           CompletableFuture<Void> done, long bytes) {
        void start() {
            try { DECODER.execute(() -> {
                try {
                    request.requireCurrent();
                    var batch = decode.get();
                    request.requireCurrent();
                    target.apply(request, batch).whenComplete((_, failure) -> finish(failure));
                } catch (Throwable error) { finish(error); }
            }); } catch (RuntimeException error) { finish(error); }
        }
        void finish(Throwable error) {
            Pending next;
            synchronized (QUEUES) {
                var queue = QUEUES.get(player);
                if (queue == null || queue.peekFirst() != this) return;
                queue.removeFirst(); pendingBytes -= bytes;
                next = queue.peekFirst();
                if (next == null) QUEUES.remove(player);
            }
            if (error == null) done.complete(null); else done.completeExceptionally(error);
            if (next != null) next.start();
        }
    }
}
