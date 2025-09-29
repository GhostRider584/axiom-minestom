package fr.ghostrider584.axiom.world;

import fr.ghostrider584.axiom.util.CompressedBlockEntity;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.network.NetworkBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minestom.server.network.NetworkBuffer.*;

public class PaletteProcessor {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaletteProcessor.class);

	private static final long SENTINEL_VALUE = 0b1000000000000000000000000010000000000000000000000000100000000000L;
	private static final int EMPTY_BLOCK_STATE = Block.VOID_AIR.stateId();

	private static final ThreadLocal<int[]> PALETTE_BUFFER = ThreadLocal.withInitial(() -> new int[4096]);

	public static void processChunkData(NetworkBuffer buffer, boolean allowNbt, Instance instance) {
		final long startTime = System.currentTimeMillis();
		final var blockEntities = new HashMap<Point, BinaryTag>();

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

				final int blockEntityCount = buffer.read(VAR_INT);
				for (int i = 0; i < blockEntityCount; i++) {
					final short offset = buffer.read(SHORT);
					final int localX = (offset >> 8) & 0xF;
					final int localY = offset & 0xF;
					final int localZ = (offset >> 4) & 0xF;

					final var pos = new Vec(
							(sectionX << 4) + localX,
							(sectionY << 4) + localY,
							(sectionZ << 4) + localZ
					);

					final var nbt = parseCompressedBlockEntity(buffer);
					blockEntities.put(pos, nbt);
				}

				applyPaletteOptimized(chunk, sectionY, incomingPalette, blockEntities);
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

	private static void applyPaletteOptimized(Chunk chunk, int sectionY, Palette incomingPalette, Map<Point, BinaryTag> blockEntities) {
		final var section = chunk.getSection(sectionY);

		synchronized (chunk) {
			if (!chunk.isLoaded()) {
				LOGGER.warn("Attempted to modify unloaded chunk [{}, {}]", chunk.getChunkX(), chunk.getChunkZ());
				return;
			}

			final int incomingCount = incomingPalette.count();
			final int maxSize = section.blockPalette().maxSize(); // should be 4096 for a chunk

			// if incoming palette is effectively empty, do nothing
			if (incomingCount == 0) {
				return;
			}

			// if incoming palette is single value, fill the whole section
			final int singleValue = incomingPalette.singleValue();
			if (singleValue >= 0 && singleValue != EMPTY_BLOCK_STATE) {
				chunk.invalidate();
				section.blockPalette().fill(singleValue);
				MinecraftServer.getSchedulerManager().scheduleNextTick(chunk::sendChunk);
				return;
			}

			// if incoming palette is completely full, use bulk copy
			if (incomingCount == maxSize) {
				// Check if incoming palette has any EMPTY_BLOCK_STATE values
				final var emptyCount = new AtomicInteger(0);
				incomingPalette.getAll((x, y, z, value) -> {
					if (value == EMPTY_BLOCK_STATE) {
						emptyCount.incrementAndGet();
					}
				});

				if (emptyCount.get() == 0) {
					// No empty blocks in incoming palette, safe to copy entirely
					chunk.invalidate();
					section.blockPalette().copyFrom(incomingPalette);
					MinecraftServer.getSchedulerManager().scheduleNextTick(chunk::sendChunk);
					return;
				}
			}

			// pre-extract incoming palette values to avoid repeated get() calls
			final int[] incomingValues = PALETTE_BUFFER.get();
			incomingPalette.getAll((x, y, z, value) -> {
				final int idx = (y << 8) | (z << 4) | x; // linear index
				incomingValues[idx] = value;
			});

			chunk.invalidate();

			// use bulk replacement
			section.blockPalette().replaceAll((sx, sy, sz, currentStateId) -> {
				final int linearIndex = (sy << 8) | (sz << 4) | sx;
				final int newBlockState = incomingValues[linearIndex];

				if (newBlockState != EMPTY_BLOCK_STATE) {
					return newBlockState;
				}
				return currentStateId;
			});

			blockEntities.forEach((position, binaryTag) -> {
				final int relativeX = CoordConversion.globalToSectionRelative(position.blockX());
				final int relativeY = position.blockY();
				final int relativeZ = CoordConversion.globalToSectionRelative(position.blockZ());

				final int blockSectionY = CoordConversion.globalToSection(position.blockY());
				if (blockSectionY == sectionY) {
					final var currentBlock = chunk.getBlock(relativeX, relativeY, relativeZ);

					if (binaryTag instanceof CompoundBinaryTag compoundTag && !compoundTag.keySet().isEmpty()) {
						chunk.setBlock(relativeX, relativeY, relativeZ, currentBlock.withNbt(compoundTag));
					}
				}
			});

			MinecraftServer.getSchedulerManager().scheduleNextTick(chunk::sendChunk);
			LOGGER.trace("Sent chunk update for [{}, {}]", chunk.getChunkX(), chunk.getChunkZ());
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