package fr.ghostrider584.axiom.network.packet.client;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;

import static net.minestom.server.network.NetworkBuffer.*;

public record HelloMessage(
		int apiVersion,
		int dataVersion,
		int protocolVersion,
		long handshakeId
) implements ClientPacket.Play {

	public static final NetworkBuffer.Type<HelloMessage> TYPE = NetworkBufferTemplate.template(
			VAR_INT, HelloMessage::apiVersion,
			VAR_INT, HelloMessage::dataVersion,
			VAR_INT, HelloMessage::protocolVersion,
			LONG, HelloMessage::handshakeId,
			HelloMessage::new
	);
}
