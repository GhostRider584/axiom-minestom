package fr.ghostrider584.axiom.metadata;

import net.minestom.server.codec.Codec;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.registry.Registries;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;

public record MetadataMapping<T>(
		@NotNull String path,
		@NotNull MetadataDef.Entry<T> metadataEntry,
		@NotNull Codec<T> codec
) {

	public static <T> MetadataMapping<RegistryKey<T>> registry(String path, MetadataDef.Entry<RegistryKey<T>> entry, Registries.Selector<T> selector) {
		return new MetadataMapping<>(path, entry, RegistryKey.codec(selector));
	}

	public static <T> MetadataMapping<T> mapping(String path, MetadataDef.Entry<T> entry, Codec<T> codec) {
		return new MetadataMapping<>(path, entry, codec);
	}

	public static <T> MetadataMapping<T> direct(String path, MetadataDef.Entry<T> entry) {
		return new MetadataMapping<>(path, entry, inferCodec(entry));
	}

	@SuppressWarnings("unchecked")
	private static <T> Codec<T> inferCodec(MetadataDef.Entry<T> entry) {
		final var defaultValue = entry.defaultValue();
		return switch (defaultValue) {
			case Integer ignored -> (Codec<T>) Codec.INT;
			case Float ignored -> (Codec<T>) Codec.FLOAT;
			case Boolean ignored -> (Codec<T>) Codec.BOOLEAN;
			case Byte ignored -> (Codec<T>) Codec.BYTE;
			case String ignored -> (Codec<T>) Codec.STRING;
			default -> throw new IllegalArgumentException("Cannot infer codec for type: " + defaultValue.getClass());
		};
	}
}