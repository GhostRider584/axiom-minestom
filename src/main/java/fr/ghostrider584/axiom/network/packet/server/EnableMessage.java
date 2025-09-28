package fr.ghostrider584.axiom.network.packet.server;

import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;

import static net.minestom.server.network.NetworkBuffer.*;

public record EnableMessage(
		boolean enable,
		int maxBufferSize,
		int blueprintVersion,
		int customDataOverrides,
		int rotationOverrides
) implements ServerPacket.Play {

	public static final Type<EnableMessage> TYPE = NetworkBufferTemplate.template(
			BOOLEAN, EnableMessage::enable,
			INT, EnableMessage::maxBufferSize,
			VAR_INT, EnableMessage::blueprintVersion,
			VAR_INT, EnableMessage::customDataOverrides,
			VAR_INT, EnableMessage::rotationOverrides,
			EnableMessage::new
	);
}
