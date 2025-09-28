package fr.ghostrider584.axiom.metadata;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.registry.RegistryTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

final class SimpleMetadataCodec<T extends EntityMeta> implements MetadataCodec<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMetadataCodec.class);
	private static final Transcoder<BinaryTag> REGISTRY_NBT_TRANSCODER = new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());

	private final List<MetadataMapping<?>> mappings;

	SimpleMetadataCodec(List<MetadataMapping<?>> mappings) {
		this.mappings = List.copyOf(mappings);
	}

	@Override
	public void applyFromNBT(T meta, CompoundBinaryTag nbt) {
		for (final var mapping : mappings) {
			final var value = getNestedValue(nbt, mapping.path());
			if (value != null) {
				applyMapping(meta, mapping, value);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public CompoundBinaryTag toNBT(T meta) {
		final var holder = MetadataFieldAccessor.getMetadataHolder(meta);

		var rootTag = CompoundBinaryTag.empty(); // TODO: use builder ?
		for (final var mapping : mappings) {
			final var value = holder.get(mapping.metadataEntry());

			final var encoded = ((Codec<Object>) mapping.codec()).encode(REGISTRY_NBT_TRANSCODER, value);
			if (encoded instanceof Result.Ok<BinaryTag>(BinaryTag tag)) {
				rootTag = setNestedValue(rootTag, mapping.path(), tag);
			} else if (encoded instanceof Result.Error<BinaryTag>(String message)) {
				LOGGER.error("Error encountered while encoding: {}", message);
			}
		}

		return rootTag;
	}

	private <V> void applyMapping(T meta, MetadataMapping<V> mapping, BinaryTag nbtValue) {
		final var holder = MetadataFieldAccessor.getMetadataHolder(meta);
		try {
			final var decoded = mapping.codec().decode(REGISTRY_NBT_TRANSCODER, nbtValue);
			if (decoded instanceof Result.Ok<V>(V value)) {
				holder.set(mapping.metadataEntry(), value);
			} else if (decoded instanceof Result.Error<V>(String message)) {
				LOGGER.error("Error encountered while decoding: {}", message);
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to apply mapping {}: {}", mapping.path(), e.getMessage());
		}
	}

	private BinaryTag getNestedValue(CompoundBinaryTag nbt, String path) {
		if (!path.contains(".")) {
			return nbt.get(path);
		}

		final var parts = path.split("\\.");
		BinaryTag current = nbt;

		for (final var part : parts) {
			if (current instanceof CompoundBinaryTag compound) {
				current = compound.get(part);
				if (current == null) {
					return null;
				}
			} else {
				return null;
			}
		}

		return current;
	}

	private static CompoundBinaryTag setNestedValue(CompoundBinaryTag existing, String path, BinaryTag value) {
		if (!path.contains(".")) {
			return existing.put(path, value);
		}

		final var parts = path.split("\\.");
		return setNestedValue(existing, parts, 0, value);
	}

	private static CompoundBinaryTag setNestedValue(CompoundBinaryTag current, String[] parts, int index, BinaryTag value) {
		final var key = parts[index];
		if (index == parts.length - 1) {
			return current.put(key, value);
		}

		final var nested = current.getCompound(key);
		final var updatedNested = setNestedValue(nested, parts, index + 1, value);

		return current.put(key, updatedNested);
	}
}