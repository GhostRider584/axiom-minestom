package fr.ghostrider584.axiom.metadata;

import net.minestom.server.entity.MetadataHolder;
import net.minestom.server.entity.metadata.EntityMeta;

import java.lang.reflect.Field;

final class MetadataFieldAccessor {
	private static final Field METADATA_FIELD;

	static {
		try {
			METADATA_FIELD = EntityMeta.class.getDeclaredField("metadata");
			METADATA_FIELD.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	private MetadataFieldAccessor() {
	}

	static MetadataHolder getMetadataHolder(EntityMeta entityMeta) {
		try {
			return (MetadataHolder) METADATA_FIELD.get(entityMeta);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
