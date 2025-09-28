package fr.ghostrider584.axiom.metadata;

import fr.ghostrider584.axiom.metadata.entity.DisplayMetaCodecs;
import fr.ghostrider584.axiom.metadata.entity.EntityMetaCodec;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.entity.metadata.animal.PigMeta;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.registry.Registries;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static fr.ghostrider584.axiom.metadata.MetadataCodecs.*;
import static fr.ghostrider584.axiom.metadata.MetadataMapping.*;

public final class MetadataCodecRegistry {
	private static final Map<Class<? extends EntityMeta>, MetadataCodec<?>> REGISTRY = new ConcurrentHashMap<>();

	static {
		register(EntityMeta.class, EntityMetaCodec.ENTITY);
		register(ItemDisplayMeta.class, DisplayMetaCodecs.ITEM_DISPLAY);
		register(BlockDisplayMeta.class, DisplayMetaCodecs.BLOCK_DISPLAY);
		register(TextDisplayMeta.class, DisplayMetaCodecs.TEXT_DISPLAY);

		register(PigMeta.class, extend(EntityMetaCodec.ENTITY,
				registry("variant", MetadataDef.Pig.VARIANT, Registries::pigVariant)));
	}

	public static <T extends EntityMeta> void register(Class<T> metaClass, MetadataCodec<T> codec) {
		REGISTRY.put(metaClass, codec);
	}

	@SuppressWarnings("unchecked")
	public static <T extends EntityMeta> MetadataCodec<T> get(Class<T> metaClass) {
		final var codec = (MetadataCodec<T>) REGISTRY.get(metaClass);
		if (codec != null) {
			return codec;
		}

		// try to find a codec for a superclass
		for (final var entry : REGISTRY.entrySet()) {
			if (entry.getKey().isAssignableFrom(metaClass)) {
				return (MetadataCodec<T>) entry.getValue();
			}
		}

		throw new IllegalArgumentException("No codec found for " + metaClass);
	}

	@SuppressWarnings("unchecked")
	public static <T extends EntityMeta> void applyNBT(T meta, CompoundBinaryTag nbt) {
		final var codec = (MetadataCodec<T>) get(meta.getClass());
		codec.applyFromNBT(meta, nbt);
	}

	@SuppressWarnings("unchecked")
	public static <T extends EntityMeta> CompoundBinaryTag toNBT(T meta) {
		final var codec = (MetadataCodec<T>) get(meta.getClass());
		return codec.toNBT(meta);
	}
}