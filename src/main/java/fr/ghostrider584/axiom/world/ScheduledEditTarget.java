package fr.ghostrider584.axiom.world;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.SharedInstance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.registry.RegistryKey;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/** Default path for explicitly authorized ordinary instances; workspaces install their own target. */
public final class ScheduledEditTarget implements EditTarget {
    public static final int CELLS_PER_TICK = 512;
    private final Consumer<Runnable> nextTick;
    public ScheduledEditTarget() { this(task -> MinecraftServer.getSchedulerManager().scheduleNextTick(task)); }
    public ScheduledEditTarget(Consumer<Runnable> nextTick) { this.nextTick = nextTick; }

    @Override public CompletionStage<Void> apply(Request request, EditBatch batch) {
        var done = new CompletableFuture<Void>();
        class Pending implements Runnable {
            int section, cell;
            public void run() {
                var changed = new HashSet<Chunk>();
                Exception failure = null;
                try {
                    request.requireCurrent();
                    for (int budget = CELLS_PER_TICK; budget > 0 && section < batch.segments().size(); budget--) {
                        var segment = batch.segments().get(section);
                        if (cell == segment.size()) { section++; cell = 0; budget++; continue; }
                        var change = segment.change(cell++);
                        if (change != null) applyCell(request, change, changed);
                    }
                    while (section < batch.segments().size() && cell == batch.segments().get(section).size()) { section++; cell = 0; }
                } catch (Exception e) { failure = e; }
                try { for (Chunk chunk : changed) if (chunk.isLoaded()) chunk.sendChunk(); }
                catch (Exception e) { if (failure == null) failure = e; else failure.addSuppressed(e); }
                if (failure != null) done.completeExceptionally(failure);
                else if (section == batch.segments().size()) done.complete(null);
                else try { nextTick.accept(this); } catch (Exception e) { done.completeExceptionally(e); }
            }
        }
        try { nextTick.accept(new Pending()); } catch (Exception e) { done.completeExceptionally(e); }
        return done.minimalCompletionStage();
    }

    private static void applyCell(Request request, EditBatch.Change change, HashSet<Chunk> changed) {
        var instance = request.instance();
        // Listeners may re-enter synchronized instance mutations. Match Minestom's
        // instance -> chunk lock order, including SharedInstance's backing container.
        synchronized (instance instanceof SharedInstance shared ? shared.getInstanceContainer() : instance) {
            var p = change.position();
            var dimension = instance.getCachedDimensionType();
            if (p.blockY() < dimension.minY() || p.blockY() >= dimension.maxY() || !instance.getWorldBorder().inBounds(p))
                throw new IllegalArgumentException("Edit outside this world's bounds");
            var chunk = instance.getChunk(p.chunkX(), p.chunkZ());
            if (chunk == null || !chunk.isLoaded()) throw new IllegalStateException("Load the target chunks before editing");
            chunk.lockWriteLock();
            try {
                switch (change) {
                    case EditBatch.BiomeChange biome -> {
                        var key = RegistryKey.<net.minestom.server.world.biome.Biome>unsafeOf(biome.biome());
                        if (MinecraftServer.getBiomeRegistry().get(key) == null) throw new IllegalArgumentException("Unknown biome " + biome.biome());
                        chunk.setBiome(p.blockX(), p.blockY(), p.blockZ(), key);
                    }
                    case EditBatch.BlockChange block -> {
                        Block current = chunk.getBlock(p.blockX(), p.blockY(), p.blockZ());
                        Block replacement = block.block();
                        if (!request.allowNbt()) replacement = replacement.id() == current.id()
                                ? current.withProperties(replacement.properties()) : replacement.withNbt(null);
                        var single = request.singleBlockMessage();
                        if (single == null) chunk.setBlock(p.blockX(), p.blockY(), p.blockZ(), replacement);
                        else {
                            boolean updates = single.updateNeighbors() && !single.preventUpdatesAt().contains(p);
                            var face = BlockFace.fromDirection(single.blockHit().direction());
                            if (single.breaking()) {
                                var event = new PlayerBlockBreakEvent(request.player(), instance, current, current, p, face);
                                EventDispatcher.call(event);
                                if (!event.isCancelled()) instance.setBlock(p, Block.AIR, updates);
                            } else {
                                var event = new PlayerBlockPlaceEvent(request.player(), instance, replacement, face, p, Vec.ZERO, single.hand());
                                EventDispatcher.call(event);
                                if (!event.isCancelled()) instance.setBlock(p, event.getBlock(), updates);
                            }
                        }
                    }
                }
                changed.add(chunk); // Also correct a cancelled client-predicted single-block edit.
            } finally { chunk.unlockWriteLock(); }
        }
    }
}
