package fr.ghostrider584.axiom.network.packet.client;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;

import static net.minestom.server.network.NetworkBuffer.*;

public record TeleportMessage(
		Key dimension,
		Pos position
) implements ClientPacket {

	public static final NetworkBuffer.Type<TeleportMessage> TYPE = NetworkBufferTemplate.template(
			KEY, TeleportMessage::dimension,
			POS, TeleportMessage::position,
			TeleportMessage::new
	);
}