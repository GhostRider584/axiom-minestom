package fr.ghostrider584.axiom.network.packet.client;

import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;

import static net.minestom.server.network.NetworkBuffer.*;

public record SetFlySpeedMessage(float flySpeed) implements ClientPacket {

	public static final Type<SetFlySpeedMessage> TYPE = NetworkBufferTemplate.template(
			FLOAT, SetFlySpeedMessage::flySpeed,
			SetFlySpeedMessage::new
	);
}