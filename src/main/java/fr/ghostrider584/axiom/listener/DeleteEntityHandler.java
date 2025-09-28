package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.event.AxiomRemoveEntityEvent;
import fr.ghostrider584.axiom.marker.MarkerManager;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.DeleteEntityMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class DeleteEntityHandler implements IncomingMessageHandler<DeleteEntityMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeleteEntityHandler.class);

	@Override
	public void handle(Player player, String channel, DeleteEntityMessage packet) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.ENTITY_DELETE)) {
			LOGGER.warn("Player {} attempted to delete entities without permission", player.getUsername());
			return;
		}

		final var instance = player.getInstance();
		if (instance == null) {
			LOGGER.warn("Player {} attempted to delete entities while not in an instance", player.getUsername());
			return;
		}

		int deletedCount = 0;
		for (final var entityUuid : packet.entityUuids()) {
			if (processEntityDeletion(player, entityUuid)) {
				deletedCount++;
			}
		}

		if (deletedCount > 0) {
			LOGGER.trace("Player {} successfully deleted {} entities", player.getUsername(), deletedCount);
		}
	}

	private boolean processEntityDeletion(Player player, UUID entityUuid) {
		final var instance = player.getInstance();
		final var entity = instance.getEntityByUuid(entityUuid);

		if (entity == null) {
			LOGGER.debug("Entity with UUID {} not found for deletion by player {}", entityUuid, player.getUsername());
			return false;
		}

		if (entity instanceof Player) {
			LOGGER.warn("Player {} attempted to delete another player entity {}", player.getUsername(), entityUuid);
			return false;
		}

		try {
			final var removeEvent = new AxiomRemoveEntityEvent(player, entity);
			EventDispatcher.callCancellable(removeEvent, () -> {
				if (entity.getEntityType() == EntityType.MARKER) {
					MarkerManager.removeMarkerImmediately(entity);
				}

				entity.remove();
				LOGGER.trace("Successfully deleted entity {} for player {}", entityUuid, player.getUsername());
			});

			return !removeEvent.isCancelled();
		} catch (Exception e) {
			LOGGER.error("Error deleting entity {} for player {}: {}",
					entityUuid, player.getUsername(), e.getMessage(), e);
			return false;
		}
	}
}