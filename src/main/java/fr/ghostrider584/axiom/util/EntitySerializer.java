package fr.ghostrider584.axiom.util;

import fr.ghostrider584.axiom.metadata.MetadataCodecRegistry;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntitySerializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(EntitySerializer.class);

	private static final Codec<Entity> ENTITY_CODEC = new Codec<>() {
		@Override
		public <D> @NotNull Result<D> encode(@NotNull Transcoder<D> transcoder, @Nullable Entity entity) {
			if (entity == null) {
				return new Result.Error<>("Entity cannot be null");
			}

			try {
				final var mapBuilder = transcoder.createMap();

				encodeBasicData(transcoder, mapBuilder, entity);
				encodePositionData(transcoder, mapBuilder, entity);
				encodeMetadata(transcoder, mapBuilder, entity);
				encodeCustomTags(transcoder, mapBuilder, entity);

				return new Result.Ok<>(mapBuilder.build());
			} catch (Exception e) {
				return new Result.Error<>("Failed to serialize entity: " + e.getMessage());
			}
		}

		@Override
		public <D> @NotNull Result<Entity> decode(@NotNull Transcoder<D> coder, @NotNull D value) {
			return new Result.Error<>("Entity deserialization not implemented");
		}

		private <D> void encodeBasicData(Transcoder<D> transcoder, Transcoder.MapBuilder<D> builder, Entity entity) {
			builder.put("id", transcoder.createString(entity.getEntityType().key().asString()));
			builder.put("UUID", transcoder.createString(entity.getUuid().toString()));

			builder.put("NoGravity", transcoder.createBoolean(entity.hasNoGravity()));
			builder.put("Silent", transcoder.createBoolean(entity.isSilent()));
			builder.put("Glowing", transcoder.createBoolean(entity.isGlowing()));
			builder.put("OnGround", transcoder.createBoolean(entity.isOnGround()));

			if (entity instanceof LivingEntity livingEntity) {
				builder.put("Invulnerable", transcoder.createBoolean(livingEntity.isInvulnerable()));
			}

			final var customName = entity.get(DataComponents.CUSTOM_NAME);
			if (customName != null) {
				final var componentResult = Codec.COMPONENT.encode(transcoder, customName);
				if (componentResult instanceof Result.Ok<D>(D componentData)) {
					builder.put("CustomName", componentData);
					builder.put("CustomNameVisible", transcoder.createBoolean(entity.isCustomNameVisible()));
				}
			}
		}

		private <D> void encodePositionData(Transcoder<D> transcoder, Transcoder.MapBuilder<D> builder, Entity entity) {
			final var pos = entity.getPosition();

			// double list [x, y, z]
			final var positionList = transcoder.createList(3);
			positionList.add(transcoder.createDouble(pos.x()));
			positionList.add(transcoder.createDouble(pos.y()));
			positionList.add(transcoder.createDouble(pos.z()));
			builder.put("Pos", positionList.build());

			// double list [yaw, pitch]
			final var rotationList = transcoder.createList(2);
			rotationList.add(transcoder.createFloat(pos.yaw()));
			rotationList.add(transcoder.createFloat(pos.pitch()));
			builder.put("Rotation", rotationList.build());

			// Motion/velocity
			final var velocity = entity.getVelocity();
			final var motionList = transcoder.createList(3);
			motionList.add(transcoder.createDouble(velocity.x()));
			motionList.add(transcoder.createDouble(velocity.y()));
			motionList.add(transcoder.createDouble(velocity.z()));
			builder.put("Motion", motionList.build());
		}

		private <D> void encodeMetadata(Transcoder<D> transcoder, Transcoder.MapBuilder<D> builder, Entity entity) {
			try {
				final var entityMeta = entity.getEntityMeta();
				final var metadataNbt = MetadataCodecRegistry.toNBT(entityMeta);
				mergeCompound(transcoder, builder, metadataNbt);
			} catch (Exception e) {
				LOGGER.warn("Failed to encode metadata for entity {}: {}", entity.getUuid(), e.getMessage());
			}
		}

		private <D> void encodeCustomTags(Transcoder<D> transcoder, Transcoder.MapBuilder<D> builder, Entity entity) {
			final var customTags = entity.tagHandler().asCompound();
			if (customTags != CompoundBinaryTag.empty()) {
				mergeCompound(transcoder, builder, customTags);
			}
		}

		private static <D> void mergeCompound(Transcoder<D> transcoder, Transcoder.MapBuilder<D> builder, CompoundBinaryTag compound) {
			final var result = Codec.NBT_COMPOUND.encode(transcoder, compound);
			if (result instanceof Result.Ok<D>(D data)) {
				if (transcoder.getMap(data) instanceof Result.Ok<Transcoder.MapLike<D>>(var tagsMap)) {
					for (final var key : tagsMap.keys()) {
						if (tagsMap.getValue(key) instanceof Result.Ok<D>(D value)) {
							builder.put(key, value);
						}
					}
				}
			}
		}
	};

	private EntitySerializer() {
	}

	public static Result<CompoundBinaryTag> serialize(Entity entity) {
		return ENTITY_CODEC.encode(Transcoder.NBT, entity)
				.mapResult(tag -> (CompoundBinaryTag) tag);
	}

	public static boolean canSerialize(Entity entity) {
		if (entity instanceof Player) {
			return false;
		}

		try {
			final var entityMeta = entity.getEntityMeta();
			MetadataCodecRegistry.get(entityMeta.getClass());
			return true;
		} catch (IllegalArgumentException e) {
			LOGGER.debug("No metadata codec found for entity type: {}", entity.getEntityType());
		}

		return false;
	}
}