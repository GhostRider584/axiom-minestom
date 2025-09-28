package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.event.AxiomFlySpeedChangeEvent;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.SetFlySpeedMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetFlySpeedHandler implements IncomingMessageHandler<SetFlySpeedMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SetFlySpeedHandler.class);

	@Override
	public void handle(Player player, String channel, SetFlySpeedMessage packet) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.PLAYER_SPEED)) {
			LOGGER.warn("Player {} does not have permission to change fly speed", player.getUsername());
			return;
		}

		// clamp fly speed to valid range
		final float newFlySpeed = Math.max(-1.0f, Math.min(1.0f, packet.flySpeed()));
		final var previousFlySpeed = player.getFlyingSpeed();

		EventDispatcher.callCancellable(new AxiomFlySpeedChangeEvent(player, previousFlySpeed, newFlySpeed), () -> {
			player.setFlyingSpeed(newFlySpeed);
			LOGGER.trace("Player {} changed fly speed to {}", player.getUsername(), newFlySpeed);
		});
	}
}