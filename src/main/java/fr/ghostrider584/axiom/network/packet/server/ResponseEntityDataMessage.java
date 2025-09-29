package fr.ghostrider584.axiom.network.packet.server;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;

import java.util.Map;
import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record ResponseEntityDataMessage(
		long requestId,
		boolean finished,
		Map<UUID, CompoundBinaryTag> entityData
) implements ServerPacket.Play {

	public ResponseEntityDataMessage {
		entityData = Map.copyOf(entityData);
	}

	public static final NetworkBuffer.Type<ResponseEntityDataMessage> TYPE = NetworkBufferTemplate.template(
			LONG, ResponseEntityDataMessage::requestId,
			BOOLEAN, ResponseEntityDataMessage::finished,
			UUID.mapValue(NBT_COMPOUND, Integer.MAX_VALUE), ResponseEntityDataMessage::entityData,
			ResponseEntityDataMessage::new
	);
}