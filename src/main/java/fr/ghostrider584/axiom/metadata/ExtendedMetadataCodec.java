package fr.ghostrider584.axiom.metadata;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.metadata.EntityMeta;

final class ExtendedMetadataCodec<T extends EntityMeta> implements MetadataCodec<T> {
	private final MetadataCodec<? super T> base;
	private final MetadataCodec<T> additional;

	ExtendedMetadataCodec(MetadataCodec<? super T> base, MetadataCodec<T> additional) {
		this.base = base;
		this.additional = additional;
	}

	@Override
	public void applyFromNBT(T meta, CompoundBinaryTag nbt) {
		base.applyFromNBT(meta, nbt);
		additional.applyFromNBT(meta, nbt);
	}

	@Override
	public CompoundBinaryTag toNBT(T meta) {
		final var baseNbt = base.toNBT(meta);
		final var additionalNbt = additional.toNBT(meta);

		final var merged = CompoundBinaryTag.builder();
		merged.put(baseNbt);
		merged.put(additionalNbt);
		return merged.build();
	}
}