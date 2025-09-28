package fr.ghostrider584.axiom.metadata;

import net.minestom.server.entity.metadata.EntityMeta;

import java.util.List;

public final class MetadataCodecs {

	public static <T extends EntityMeta> MetadataCodec<T> metadataCodec(List<MetadataMapping<?>> mappings) {
		return new SimpleMetadataCodec<>(mappings);
	}

	public static <T extends EntityMeta> MetadataCodec<T> metadataCodec(MetadataMapping<?>... mappings) {
		return metadataCodec(List.of(mappings));
	}

	public static <T extends EntityMeta> MetadataCodec<T> extend(MetadataCodec<? super T> base, List<MetadataMapping<?>> additionalMappings) {
		return new ExtendedMetadataCodec<>(base, metadataCodec(additionalMappings));
	}

	public static <T extends EntityMeta> MetadataCodec<T> extend(MetadataCodec<? super T> base, MetadataMapping<?>... additionalMappings) {
		return extend(base, List.of(additionalMappings));
	}
}