package fr.ghostrider584.axiom.network.packet.client;

import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;

import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record MarkerNbtRequestMessage(
		UUID uuid,
		int reason
) implements ClientPacket {

	public static final Type<MarkerNbtRequestMessage> TYPE = NetworkBufferTemplate.template(
			UUID, MarkerNbtRequestMessage::uuid,
			VAR_INT, MarkerNbtRequestMessage::reason,
			MarkerNbtRequestMessage::new
	);
}