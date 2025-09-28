package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.event.AxiomTeleportEvent;
import fr.ghostrider584.axiom.event.AxiomUnknownTeleportEvent;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.TeleportMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import fr.ghostrider584.axiom.util.WorldBoundaries;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.world.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeleportHandler implements IncomingMessageHandler<TeleportMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(TeleportHandler.class);

	// todo: make this configurable
	private static final boolean ALLOW_TELEPORT_BETWEEN_INSTANCES = true;

	@Override
	public void handle(Player player, String channel, TeleportMessage packet) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.PLAYER_TELEPORT)) {
			LOGGER.warn("Player {} attempted to teleport without permission", player.getUsername());
			return;
		}

		final var currentInstance = player.getInstance();
		if (currentInstance == null) {
			LOGGER.warn("Player {} attempted to teleport while not in an instance", player.getUsername());
			return;
		}

		final var targetPos = packet.position();
		if (!WorldBoundaries.isInValidBounds(targetPos)) {
			LOGGER.debug("Player {} attempted to teleport to invalid position: {}", player.getUsername(), targetPos);
			return;
		}

		final var unknownTeleportEvent = new AxiomUnknownTeleportEvent(player, packet.dimension(), targetPos);
		EventDispatcher.callCancellable(unknownTeleportEvent, () -> {
			final var targetInstance = resolveInstance(packet.dimension());
			if (targetInstance == null) {
				LOGGER.warn("Could not resolve instance for dimension key: {}", packet.dimension());
				return;
			}

			final boolean isCrossInstance = !targetInstance.equals(currentInstance);
			if (isCrossInstance && !ALLOW_TELEPORT_BETWEEN_INSTANCES) {
				LOGGER.debug("Cross-instance teleport denied for player {} (from {} to {})",
						player.getUsername(), currentInstance.getUuid(), targetInstance.getUuid());
				return;
			}

			final var teleportEvent = new AxiomTeleportEvent(player, targetInstance, targetPos);
			EventDispatcher.callCancellable(teleportEvent, () -> {
				performTeleport(player, targetInstance, targetPos);
			});
		});
	}

	private Instance resolveInstance(Key dimensionKey) {
		// TODO: handle instance resolution differently?
		final var instanceManager = MinecraftServer.getInstanceManager();
		for (final var instance : instanceManager.getInstances()) {
			final var instanceDimensionKey = instance.getDimensionType();
			if (dimensionKey.compareTo(instanceDimensionKey.key()) == 0) {
				return instance;
			}
		}

		LOGGER.error("Could not resolve instance for dimension key {}", dimensionKey);
		return null;
	}

	private void performTeleport(Player player, Instance targetInstance, Pos targetPos) {
		try {
			final var currentInstance = player.getInstance();
			if (currentInstance.equals(targetInstance)) {
				player.teleport(targetPos).thenRun(() ->
						LOGGER.trace("Player {} teleported within instance to {}", player.getUsername(), targetPos));
			} else {
				player.setInstance(targetInstance, targetPos).thenRun(() ->
						LOGGER.trace("Player {} teleported to instance {} at {}", player.getUsername(), targetInstance.getUuid(), targetPos));
			}
		} catch (Exception e) {
			LOGGER.error("Error teleporting player {} to {}: {}", player.getUsername(), targetPos, e.getMessage(), e);
		}
	}
}