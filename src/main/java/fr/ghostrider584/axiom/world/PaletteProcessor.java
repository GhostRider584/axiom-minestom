package fr.ghostrider584.axiom.world;

import fr.ghostrider584.axiom.util.CompressedBlockEntity;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.network.NetworkBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static net.minestom.server.network.NetworkBuffer.*;

public class PaletteProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaletteProcessor.class);

	private static final long SENTINEL_VALUE = 0b1000000000000000000000000010000000000000000000000000100000000000L;
	private static final int EMPTY_BLOCK_STATE = Block.VOID_AIR.stateId();

	public static void processChunkData(NetworkBuffer buffer, boolean allowNbt, Instance instance) {
		final long startTime = System.currentTimeMillis();

		int sectionsProcessed = 0;

		try {
			while (buffer.readableBytes() >= 8) {
				final long sectionIndex = buffer.read(LONG);
				if (sectionIndex == SENTINEL_VALUE) {
					break;
				}

				final int sectionX = (int) (sectionIndex >> 38);
				final int sectionY = (int) (sectionIndex << 52 >> 52);
				final int sectionZ = (int) (sectionIndex << 26 >> 38);

				final var chunk = instance.getChunk(sectionX, sectionZ);
				if (chunk == null) {
					LOGGER.warn("Chunk [{}, {}] not loaded, skipping", sectionX, sectionZ);
					skipSectionData(buffer);
					continue;
				}

				final var incomingPalette = buffer.read(Palette.BLOCK_SERIALIZER);
				// Offsets are local to this section, never to the entire edit buffer.
				final var blockEntities = new HashMap<Integer, CompoundBinaryTag>();

				final int blockEntityCount = buffer.read(VAR_INT);
				for (int i = 0; i < blockEntityCount; i++) {
					final short offset = buffer.read(SHORT);
					final var nbt = parseCompressedBlockEntity(buffer);
					// Always consume the wire payload, even without NBT-edit permission.
					if (allowNbt && nbt instanceof CompoundBinaryTag compoundTag) {
						blockEntities.put(offset & 0xFFF, compoundTag);
					}
				}

				applyPalette(chunk, sectionY, incomingPalette, blockEntities, allowNbt);
				sectionsProcessed++;

				LOGGER.trace("Processed section [{}, {}, {}]", sectionX, sectionY, sectionZ);
			}
		} catch (Exception e) {
			LOGGER.error("Failed to process chunk data after {} sections", sectionsProcessed, e);
			return;
		}

		if (LOGGER.isDebugEnabled()) {
			final long endTime = System.currentTimeMillis();
			final long totalTime = endTime - startTime;

			LOGGER.debug("Palette processing completed: {} sections in {}ms", sectionsProcessed, totalTime);
		}
	}

	private static void applyPalette(Chunk chunk, int sectionY, Palette incomingPalette,
	                                 Map<Integer, CompoundBinaryTag> blockEntities, boolean allowNbt) {
		chunk.lockWriteLock();
		try {
			if (!chunk.isLoaded()) {
				LOGGER.warn("Attempted to modify unloaded chunk [{}, {}]", chunk.getChunkX(), chunk.getChunkZ());
				return;
			}

			// Chunk.setBlock owns NBT entries, handlers, heightmaps and ticking state.
			// Raw palette fill/copy leaves those side tables stale. Keep one correct
			// write path for sparse and dense edits, skipping unchanged cells.
			final boolean[] changed = {false};
			incomingPalette.getAll((x, y, z, stateId) -> {
				if (stateId == EMPTY_BLOCK_STATE) return; // VOID_AIR means "not edited".
				final int worldX = (chunk.getChunkX() << 4) + x;
				final int worldY = (sectionY << 4) + y;
				final int worldZ = (chunk.getChunkZ() << 4) + z;
				final var current = chunk.getBlock(worldX, worldY, worldZ);
				var replacement = Block.fromStateId(stateId);
				if (replacement == null) throw new IllegalArgumentException("Unknown block state: " + stateId);
				if (allowNbt) {
					replacement = replacement.withNbt(blockEntities.get((x << 8) | (z << 4) | y));
				} else if (replacement.id() == current.id()) {
					// A permitted block-state edit must not overwrite existing NBT.
					replacement = current.withProperties(replacement.properties());
				}
				if (!replacement.equals(current)) {
					chunk.setBlock(worldX, worldY, worldZ, replacement);
					changed[0] = true;
				}
			});

			if (changed[0]) {
				MinecraftServer.getSchedulerManager().scheduleNextTick(() -> {
					if (chunk.isLoaded()) chunk.sendChunk();
				});
			}
		} finally {
			chunk.unlockWriteLock();
		}
	}

	private static void skipSectionData(NetworkBuffer buffer) {
		try {
			buffer.read(Palette.BLOCK_SERIALIZER);
			int blockEntityCount = buffer.read(VAR_INT);
			for (int i = 0; i < blockEntityCount; i++) {
				buffer.read(SHORT);
				parseCompressedBlockEntity(buffer);
			}
		} catch (Exception e) {
			LOGGER.error("Failed to skip section data", e);
		}
	}

	private static BinaryTag parseCompressedBlockEntity(NetworkBuffer buffer) {
		try {
			int originalSize = buffer.read(VAR_INT);
			byte compressionDict = buffer.read(BYTE);
			byte[] compressedBytes = buffer.read(BYTE_ARRAY);

			if (compressionDict != 0) {
				LOGGER.error("Unknown compression dict: {}", compressionDict);
				return CompoundBinaryTag.empty();
			}

			final var compressed = new CompressedBlockEntity(originalSize, compressionDict, compressedBytes);
			return CompressedBlockEntity.decompress(compressed);
		} catch (Exception e) {
			LOGGER.warn("Failed to parse block entity NBT", e);
			return CompoundBinaryTag.empty();
		}
	}
}
