package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.event.AxiomSpawnEntityEvent;
import fr.ghostrider584.axiom.marker.MarkerManager;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.SpawnEntityMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import fr.ghostrider584.axiom.util.EntityNbtHelper;
import fr.ghostrider584.axiom.util.WorldBoundaries;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpawnEntityHandler implements IncomingMessageHandler<SpawnEntityMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SpawnEntityHandler.class);

	@Override
	public void handle(Player player, String channel, SpawnEntityMessage packet) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.ENTITY_SPAWN)) {
			LOGGER.warn("Player {} attempted to spawn entities without permission", player.getUsername());
			return;
		}

		final var instance = player.getInstance();
		if (instance == null) {
			LOGGER.warn("Player {} attempted to spawn entities while not in an instance", player.getUsername());
			return;
		}

		for (final var entry : packet.entries()) {
			processSpawnEntry(player, instance, entry);
		}
	}

	@SuppressWarnings("PatternValidation")
	private void processSpawnEntry(Player player, Instance instance, SpawnEntityMessage.SpawnEntry entry) {
		final var position = new Pos(entry.x(), entry.y(), entry.z(), entry.yaw(), entry.pitch());

		if (!WorldBoundaries.isInValidBounds(position)) {
			LOGGER.debug("Player {} attempted to spawn entity outside spawnable bounds at {}", player.getUsername(), position);
			return;
		}

		if (instance.getEntityByUuid(entry.newUuid()) != null) {
			LOGGER.debug("Entity with UUID {} already exists, skipping spawn for player {}", entry.newUuid(), player.getUsername());
			return;
		}

		try {
			var entityNbt = (CompoundBinaryTag) entry.tag();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Received entity NBT: \n{}", TagStringIO.tagStringIO().asString(entityNbt));
			}

			if (entry.copyFrom() != null) {
				final var copyFromEntity = instance.getEntityByUuid(entry.copyFrom());
				if (copyFromEntity != null) {
					final var copyNbt = copyFromEntity.tagHandler().asCompound();
					if (copyNbt != CompoundBinaryTag.empty()) {
						entityNbt = copyNbt.put(entityNbt);
					}
				}
			}

			if (!entityNbt.keySet().contains("id")) {
				LOGGER.warn("No entity id found in NBT for spawn entry from player {}", player.getUsername());
				return;
			}

			final var entityIdString = entityNbt.getString("id");
			if (entityIdString.isEmpty()) {
				LOGGER.warn("Empty entity id in NBT for spawn entry from player {}", player.getUsername());
				return;
			}

			final var entityType = EntityType.fromKey(entityIdString);
			if (entityType == null) {
				LOGGER.warn("Unknown entity type {} for spawn entry from player {}", entityIdString, player.getUsername());
				return;
			}

			if (!canEntityTypeBeSpawned(entityType)) {
				LOGGER.warn("Entity type {} cannot be spawned by player {}", entityType, player.getUsername());
				return;
			}

			final var entity = new Entity(entityType, entry.newUuid());
			entity.setInstance(instance, position);
			entity.setNoGravity(true);
			EntityNbtHelper.applyNbtChanges(entity, entityNbt);

			EventDispatcher.callCancellable(new AxiomSpawnEntityEvent(player, entity), () -> {
				entity.setInstance(instance, position).thenRun(() -> {
					if (entityType == EntityType.MARKER) {
						MarkerManager.updateMarkerImmediately(entity);
					}

					LOGGER.trace("Successfully spawned entity {} for player {}",
							entry.newUuid(), player.getUsername());
				});
			});
		} catch (Exception e) {
			LOGGER.error("Error spawning entity {} for player {}: {}",
					entry.newUuid(), player.getUsername(), e.getMessage(), e);
		}
	}

	private boolean canEntityTypeBeSpawned(EntityType entityType) {
		return entityType != EntityType.PLAYER;
	}
}