package fr.ghostrider584.axiom.network.packet.client;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;

import java.util.List;
import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record RequestEntityDataMessage(
		long requestId,
		List<UUID> entityUuids
) implements ClientPacket {

	public RequestEntityDataMessage {
		entityUuids = List.copyOf(entityUuids);
	}

	public static final NetworkBuffer.Type<RequestEntityDataMessage> TYPE = NetworkBufferTemplate.template(
			LONG, RequestEntityDataMessage::requestId,
			UUID.list(Short.MAX_VALUE), RequestEntityDataMessage::entityUuids,
			RequestEntityDataMessage::new
	);
}