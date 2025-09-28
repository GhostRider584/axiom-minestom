package fr.ghostrider584.axiom.metadata.entity;

import fr.ghostrider584.axiom.metadata.MetadataCodec;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.entity.metadata.EntityMeta;

import static fr.ghostrider584.axiom.metadata.MetadataCodecs.metadataCodec;
import static fr.ghostrider584.axiom.metadata.MetadataMapping.direct;

public final class EntityMetaCodec {

	public static final MetadataCodec<EntityMeta> ENTITY = metadataCodec(
			direct("on_fire", MetadataDef.IS_ON_FIRE),
			direct("sneaking", MetadataDef.IS_CROUCHING),
			direct("sprinting", MetadataDef.IS_SPRINTING),
			direct("swimming", MetadataDef.IS_SWIMMING),
			direct("invisible", MetadataDef.IS_INVISIBLE),
			direct("glowing", MetadataDef.HAS_GLOWING_EFFECT),
			direct("flying_with_elytra", MetadataDef.IS_FLYING_WITH_ELYTRA),
			direct("air_ticks", MetadataDef.AIR_TICKS),
			direct("silent", MetadataDef.IS_SILENT),
			direct("no_gravity", MetadataDef.HAS_NO_GRAVITY),
			direct("ticks_frozen", MetadataDef.TICKS_FROZEN)
	);
}
