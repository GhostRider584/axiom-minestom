package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.event.AxiomGameModeChangeEvent;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.SetGameModeMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetGameModeHandler implements IncomingMessageHandler<SetGameModeMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SetGameModeHandler.class);

	@Override
	public void handle(Player player, String channel, SetGameModeMessage packet) {
		final var newGameMode = GameMode.values()[packet.gameMode()];
		// TODO: check if gamemode has a valid value before parsing

		final var permission = switch (newGameMode) {
			case SURVIVAL -> AxiomPermission.PLAYER_GAMEMODE_SURVIVAL;
			case CREATIVE -> AxiomPermission.PLAYER_GAMEMODE_CREATIVE;
			case ADVENTURE -> AxiomPermission.PLAYER_GAMEMODE_ADVENTURE;
			case SPECTATOR -> AxiomPermission.PLAYER_GAMEMODE_SPECTATOR;
		};

		if (!AxiomPermissions.hasPermission(player, permission)) {
			LOGGER.warn("Player {} does not have permission for game mode {}", player.getUsername(), newGameMode);
			return;
		}

		final var previousGameMode = player.getGameMode();
		EventDispatcher.callCancellable(new AxiomGameModeChangeEvent(player, previousGameMode, newGameMode), () -> {
			player.setGameMode(newGameMode);
			LOGGER.trace("Player {} changed game mode to {}", player.getUsername(), newGameMode);
		});
	}
}