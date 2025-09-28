package fr.ghostrider584.axiom.util;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public final class NbtSizeCalculator {
	private static final Logger LOGGER = LoggerFactory.getLogger(NbtSizeCalculator.class);

	private NbtSizeCalculator() {
	}

	public static long calculateExactSize(CompoundBinaryTag nbt) {
		if (nbt == null || nbt == CompoundBinaryTag.empty()) {
			return 0;
		}

		try {
			final var buffer = NetworkBuffer.resizableBuffer();
			buffer.write(NetworkBuffer.NBT_COMPOUND, nbt);

			return buffer.writeIndex();
		} catch (Exception e) {
			LOGGER.warn("Failed to calculate exact NBT size: {}", e.getMessage());
			return -1;
		}
	}

	public static long calculateSizeWithOverhead(CompoundBinaryTag nbt, long overhead) {
		final long baseSize = calculateExactSize(nbt);
		if (baseSize == -1) {
			return -1;
		}
		return baseSize + overhead;
	}

	public static long calculateSizeWithOverhead(CompoundBinaryTag nbt) {
		return calculateSizeWithOverhead(nbt, 256);
	}

	public static long calculateBatchSize(Map<UUID, CompoundBinaryTag> nbtData, long overhead) {
		long totalSize = 0;

		for (final var entry : nbtData.entrySet()) {
			final long nbtSize = calculateSizeWithOverhead(entry.getValue(), overhead);
			if (nbtSize == -1) {
				LOGGER.warn("Failed to calculate size for entity {}", entry.getKey());
				continue;
			}
			totalSize += nbtSize;
		}

		totalSize += 64;
		return totalSize;
	}

	public static long calculateBatchSize(Map<UUID, CompoundBinaryTag> nbtData) {
		return calculateBatchSize(nbtData, 256);
	}

	public static boolean wouldExceedLimit(long currentSize, CompoundBinaryTag newNbt, long maxSize, long overhead) {
		final long nbtSize = calculateSizeWithOverhead(newNbt, overhead);
		return nbtSize == -1 || (currentSize + nbtSize) > maxSize;
	}

	public static boolean wouldExceedLimit(long currentSize, CompoundBinaryTag newNbt, long maxSize) {
		return wouldExceedLimit(currentSize, newNbt, maxSize, 256);
	}
}
