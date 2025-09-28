package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.RequestEntityDataMessage;
import fr.ghostrider584.axiom.network.packet.server.ResponseEntityDataMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import fr.ghostrider584.axiom.util.EntitySerializer;
import fr.ghostrider584.axiom.util.NbtSizeCalculator;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Result;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RequestEntityDataHandler implements IncomingMessageHandler<RequestEntityDataMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestEntityDataHandler.class);
	private static final int MAX_PACKET_SIZE = 0x100000; // 1MB

	@Override
	public void handle(Player player, String channel, RequestEntityDataMessage packet) {
		final long requestId = packet.requestId();

		if (!AxiomPermissions.hasPermission(player, AxiomPermission.ENTITY_REQUESTDATA)) {
			LOGGER.warn("Player {} attempted to request entity data without permission", player.getUsername());
			sendEmptyResponse(player, requestId);
			return;
		}

		final var instance = player.getInstance();
		if (instance == null) {
			LOGGER.warn("Player {} attempted to request entity data while not in an instance", player.getUsername());
			sendEmptyResponse(player, requestId);
			return;
		}

		processEntityDataRequest(player, requestId, packet.entityUuids());
	}

	private void processEntityDataRequest(Player player, long requestId, List<UUID> requestedUuids) {
		final var instance = player.getInstance();
		final var visitedEntities = new HashSet<UUID>();
		final var entityData = new HashMap<UUID, CompoundBinaryTag>();

		long currentBatchSize = 0;

		for (final var entityUuid : requestedUuids) {
			if (!visitedEntities.add(entityUuid)) {
				continue;
			}

			final var entity = instance.getEntityByUuid(entityUuid);
			if (entity == null) {
				continue;
			}

			if (!EntitySerializer.canSerialize(entity)) {
				LOGGER.debug("Entity {} cannot be serialized for player {}", entityUuid, player.getUsername());
				continue;
			}

			final var serializationResult = EntitySerializer.serialize(entity);
			if (serializationResult instanceof Result.Ok<CompoundBinaryTag>(var entityNbt)) {
				final long exactSize = NbtSizeCalculator.calculateSizeWithOverhead(entityNbt);

				if (exactSize == -1) {
					LOGGER.warn("Failed to calculate size for entity {}", entityUuid);
					continue;
				}

				if (exactSize >= MAX_PACKET_SIZE) {
					LOGGER.debug("Entity {} exceeds single packet size limit ({}), sending separately",
							entityUuid, exactSize);
					sendResponse(player, requestId, false, Map.of(entityUuid, entityNbt));
					continue;
				}

				if (NbtSizeCalculator.wouldExceedLimit(currentBatchSize, entityNbt, MAX_PACKET_SIZE)) {
					LOGGER.trace("Batch size limit reached, sending {} entities (total size: {})",
							entityData.size(), currentBatchSize);

					sendResponse(player, requestId, false, new HashMap<>(entityData));

					entityData.clear();
					currentBatchSize = 0;
				}

				entityData.put(entityUuid, entityNbt);
				currentBatchSize += exactSize;

				LOGGER.trace("Added entity {} to batch (size: {}, batch total: {})",
						entityUuid, exactSize, currentBatchSize);

			} else if (serializationResult instanceof Result.Error<CompoundBinaryTag>(var error)) {
				LOGGER.warn("Failed to serialize entity {} for player {}: {}",
						entityUuid, player.getUsername(), error);
			}
		}

		// Send final batch with exact size logging
		final long finalBatchSize = NbtSizeCalculator.calculateBatchSize(entityData);
		LOGGER.trace("Sending final batch of {} entities (exact size: {} bytes)", entityData.size(), finalBatchSize);
		sendResponse(player, requestId, true, entityData);

		LOGGER.debug("Completed entity data request for player {} (request ID: {}, {} entities processed)",
				player.getUsername(), requestId, visitedEntities.size());
	}

	private void sendEmptyResponse(Player player, long requestId) {
		sendResponse(player, requestId, true, Map.of());
	}

	private void sendResponse(Player player, long requestId, boolean finished, Map<UUID, CompoundBinaryTag> entityData) {
		final var response = new ResponseEntityDataMessage(requestId, finished, entityData);
		AxiomMinestom.messageRegistry().send(player, response);

		LOGGER.trace("Sent entity data response to player {} (request ID: {}, finished: {}, {} entities)",
				player.getUsername(), requestId, finished, entityData.size());
	}
}