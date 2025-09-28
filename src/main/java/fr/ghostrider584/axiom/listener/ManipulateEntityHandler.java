package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.event.AxiomManipulateEntityEvent;
import fr.ghostrider584.axiom.marker.MarkerManager;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.ManipulateEntityMessage;
import fr.ghostrider584.axiom.network.packet.client.ManipulateEntityMessage.Relative;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import fr.ghostrider584.axiom.util.EntityNbtHelper;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class ManipulateEntityHandler implements IncomingMessageHandler<ManipulateEntityMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ManipulateEntityHandler.class);

	@Override
	public void handle(Player player, String channel, ManipulateEntityMessage packet) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.ENTITY_MANIPULATE)) {
			LOGGER.warn("Player {} attempted entity manipulation without permission", player.getUsername());
			return;
		}

		final var instance = player.getInstance();
		if (instance == null) {
			return;
		}

		for (final var entry : packet.entries()) {
			processManipulateEntry(player, instance, entry);
		}
	}

	private void processManipulateEntry(Player player, Instance instance, ManipulateEntityMessage.ManipulateEntry entry) {
		final var entity = instance.getEntityByUuid(entry.uuid());
		if (entity == null) {
			LOGGER.debug("Entity {} not found for manipulation by {}", entry.uuid(), player.getUsername());
			return;
		}

		if (!canEntityBeManipulated(entity.getEntityType())) {
			LOGGER.debug("Entity type {} cannot be manipulated", entity.getEntityType());
			return;
		}

		if (hasPlayerPassenger(entity)) {
			LOGGER.warn("Player {} attempted to manipulate entity with player passenger", player.getUsername());
			return;
		}

		EventDispatcher.callCancellable(new AxiomManipulateEntityEvent(player, entity), () -> {
			try {
				if (!entry.nbt().equals(CompoundBinaryTag.empty()) && entry.nbt() instanceof CompoundBinaryTag compoundBinaryTag) {
					EntityNbtHelper.applyNbtChanges(entity, compoundBinaryTag);
				}

				if (entry.position() != null && entry.relativeMovementSet() != null) {
					applyPositionChanges(entity, entry.position(), entry.relativeMovementSet());
				}

				handlePassengerManipulation(instance, entity, entry);

				if (entity.getEntityType() == EntityType.MARKER) {
					MarkerManager.updateMarkerImmediately(entity);
				}

				LOGGER.trace("Successfully manipulated entity {} for player {}", entry.uuid(), player.getUsername());
			} catch (Exception e) {
				LOGGER.error("Error manipulating entity {} for player {}: {}",
						entry.uuid(), player.getUsername(), e.getMessage(), e);
			}
		});
	}

	private void applyPositionChanges(Entity entity, Pos newPosition, Set<Relative> relativeSet) {
		final var currentPos = entity.getPosition();

		final double finalX = relativeSet.contains(Relative.X) ? currentPos.x() + newPosition.x() : newPosition.x();
		final double finalY = relativeSet.contains(Relative.Y) ? currentPos.y() + newPosition.y() : newPosition.y();
		final double finalZ = relativeSet.contains(Relative.Z) ? currentPos.z() + newPosition.z() : newPosition.z();
		final float finalYaw = relativeSet.contains(Relative.Y_ROT) ? currentPos.yaw() + newPosition.yaw() : newPosition.yaw();
		final float finalPitch = relativeSet.contains(Relative.X_ROT) ? currentPos.pitch() + newPosition.pitch() : newPosition.pitch();

		final var finalPosition = new Pos(finalX, finalY, finalZ, finalYaw, finalPitch);
		entity.teleport(finalPosition);

		LOGGER.trace("Moved entity {} to position {}", entity.getUuid(), finalPosition);
	}

	private void handlePassengerManipulation(Instance instance, Entity entity, ManipulateEntityMessage.ManipulateEntry entry) {
		switch (entry.passengerManipulation()) {
			case NONE -> {
			}
			case REMOVE_ALL -> {
				final var passengers = List.copyOf(entity.getPassengers());
				for (final var passenger : passengers) {
					entity.removePassenger(passenger);
				}
				LOGGER.trace("Removed all passengers from entity {}", entity.getUuid());
			}
			case ADD_LIST -> {
				for (final var passengerUuid : entry.passengers()) {
					final var passenger = instance.getEntityByUuid(passengerUuid);
					if (passenger != null && canEntityBeManipulated(passenger.getEntityType()) && !hasPlayerPassenger(passenger)) {
						entity.addPassenger(passenger);
						LOGGER.trace("Added passenger {} to entity {}", passengerUuid, entity.getUuid());
					}
				}
			}
			case REMOVE_LIST -> {
				for (final var passengerUuid : entry.passengers()) {
					final var passenger = instance.getEntityByUuid(passengerUuid);
					if (passenger != null && entity.getPassengers().contains(passenger)) {
						entity.removePassenger(passenger);
						LOGGER.trace("Removed passenger {} from entity {}", passengerUuid, entity.getUuid());
					}
				}
			}
		}
	}

	private boolean canEntityBeManipulated(EntityType entityType) {
		return entityType != EntityType.PLAYER;
	}

	private boolean hasPlayerPassenger(Entity entity) {
		return entity.getPassengers().stream().anyMatch(passenger -> passenger instanceof Player);
	}
}