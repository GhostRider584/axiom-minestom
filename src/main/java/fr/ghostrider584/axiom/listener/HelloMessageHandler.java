package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.marker.MarkerManager;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.HelloMessage;
import fr.ghostrider584.axiom.network.packet.server.EnableMessage;
import fr.ghostrider584.axiom.restrictions.RestrictionsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloMessageHandler implements IncomingMessageHandler<HelloMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(HelloMessageHandler.class);

	@Override
	public void handle(Player player, String channel, HelloMessage packet) {
		final var instance = player.getInstance();

		if (instance == null || !fr.ghostrider584.axiom.network.channel.AxiomHandshake.accept(player, packet)) {
			AxiomMinestom.unregisterAxiomPlayer(player);
			player.sendPluginMessage("axiom:goodbye", net.minestom.server.network.NetworkBuffer.makeArray(
					net.minestom.server.network.NetworkBuffer.STRING,
					"This server requires Axiom API 10 for the exact Minecraft server version; handshake rejected."));
			player.sendMessage(Component.text("Axiom handshake rejected: use a compatible API 10 client for this Minecraft version.", NamedTextColor.RED));
			return;
		}
		AxiomMinestom.messageRegistry().send(player, new EnableMessage(true, AxiomMinestom.messageRegistry().incomingChannels()));
		RestrictionsManager.sendPermissionBasedRestrictions(player);

		AxiomMinestom.registerAxiomPlayer(player);
		fr.ghostrider584.axiom.block.CustomBlockCatalogues.refresh(player);
		fr.ghostrider584.axiom.dispatch.DispatchSendsManager.sendUpdateAvailableDispatchSends(player, 1, 1);
		MarkerManager.onPlayerJoinInstance(player, instance);

		final var annotationRegistry = AxiomMinestom.annotationRegistry();
		if (annotationRegistry == null) {
			LOGGER.error("Annotation registry not initialized! Cannot send existing annotations to {}", player.getUsername());
			return;
		}

		final var annotationManager = annotationRegistry.getOrCreate(instance);
		annotationManager.sendAllAnnotations(player);

		player.sendMessage(Component.text("Axiom support enabled!", NamedTextColor.GREEN));
	}
}
