package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.UpdateAnnotationMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateAnnotationHandler implements IncomingMessageHandler<UpdateAnnotationMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAnnotationHandler.class);

	@Override
	public void handle(Player player, String channel, UpdateAnnotationMessage packet) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.ANNOTATION)) {
			LOGGER.warn("Player {} attempted to update annotations without permission", player.getUsername());
			return;
		}

		final var instance = player.getInstance();
		if (instance == null) {
			LOGGER.warn("Player {} attempted to update annotations while not in an instance", player.getUsername());
			return;
		}

		final var annotationRegistry = AxiomMinestom.annotationRegistry();
		if (annotationRegistry == null) {
			LOGGER.error("Annotation registry not initialized! Cannot process updates from player {}", player.getUsername());
			return;
		}

		try {
			final var annotationManager = annotationRegistry.getOrCreate(instance);
			annotationManager.handleUpdates(packet.actions(), player, instance);

			LOGGER.trace("Processed {} annotation updates from player {} in world {}",
					packet.actions().size(), player.getUsername(), instance.getUuid());
		} catch (Exception e) {
			LOGGER.error("Error processing annotation updates from player {}: {}", player.getUsername(), e.getMessage(), e);
			player.sendMessage(Component.text("An error occurred while updating annotations: " + e.getMessage()));
		}
	}
}