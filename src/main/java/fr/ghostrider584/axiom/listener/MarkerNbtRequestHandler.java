package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.MarkerNbtRequestMessage;
import fr.ghostrider584.axiom.network.packet.server.MarkerNbtResponseMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkerNbtRequestHandler implements IncomingMessageHandler<MarkerNbtRequestMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MarkerNbtRequestHandler.class);

	@Override
	public void handle(Player player, String channel, MarkerNbtRequestMessage packet) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.ENTITY_REQUESTDATA)) {
			LOGGER.warn("Player {} does not have permission to request entity data", player.getUsername());
			return;
		}

		if (!canModifyWorld(player)) {
			LOGGER.warn("Player {} cannot modify world", player.getUsername());
			return;
		}

		final var entityUuid = packet.uuid();
		final var entity = player.getInstance().getEntityByUuid(entityUuid);

		if (entity != null && entity.getEntityType() == EntityType.MARKER) {
			final var nbtData = entity.tagHandler().asCompound();

			if (nbtData != CompoundBinaryTag.empty()) {
				MarkerNbtResponseMessage response = new MarkerNbtResponseMessage(entityUuid, nbtData);
				AxiomMinestom.messageRegistry().send(player, response);

				LOGGER.trace("Sent marker NBT data for UUID {} to player {}", entityUuid, player.getUsername());
			} else {
				LOGGER.warn("Could not get NBT data for marker with UUID {}", entityUuid);
			}
		} else {
			LOGGER.warn("Marker entity with UUID {} not found or is not a marker", entityUuid);
		}
	}

	private boolean canModifyWorld(Player player) {
		return true;
	}
}