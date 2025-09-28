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

		AxiomMinestom.messageRegistry().send(player, new EnableMessage(true, ServerFlag.MAX_PACKET_SIZE, 2, 0, 0));
		RestrictionsManager.sendPermissionBasedRestrictions(player);

		AxiomMinestom.registerAxiomPlayer(player);
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