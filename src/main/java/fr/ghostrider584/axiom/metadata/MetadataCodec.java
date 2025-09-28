package fr.ghostrider584.axiom.metadata;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.metadata.EntityMeta;

public interface MetadataCodec<T extends EntityMeta> {
	void applyFromNBT(T meta, CompoundBinaryTag nbt);

	CompoundBinaryTag toNBT(T meta);
}