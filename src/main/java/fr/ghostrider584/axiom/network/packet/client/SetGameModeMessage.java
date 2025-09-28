package fr.ghostrider584.axiom.network.packet.client;

import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;

import static net.minestom.server.network.NetworkBuffer.*;

public record SetGameModeMessage(byte gameMode) implements ClientPacket {

	public static final Type<SetGameModeMessage> TYPE = NetworkBufferTemplate.template(
			BYTE, SetGameModeMessage::gameMode,
			SetGameModeMessage::new
	);
}