package fr.ghostrider584.axiom.network.packet.server;

import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;

import static net.minestom.server.network.NetworkBuffer.*;

public record UpdateAvailableDispatchSendsMessage(
		int add,
		int max
) implements ServerPacket.Play {

	public static final Type<UpdateAvailableDispatchSendsMessage> TYPE = NetworkBufferTemplate.template(
			VAR_INT, UpdateAvailableDispatchSendsMessage::add,
			VAR_INT, UpdateAvailableDispatchSendsMessage::max,
			UpdateAvailableDispatchSendsMessage::new
	);
}