package fr.ghostrider584.axiom.network.packet.client;

import fr.ghostrider584.axiom.annotation.type.AnnotationUpdateAction;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;

import java.util.List;

public record UpdateAnnotationMessage(
		List<AnnotationUpdateAction> actions
) implements ClientPacket {

	public UpdateAnnotationMessage {
		actions = List.copyOf(actions);
	}

	public static final NetworkBuffer.Type<UpdateAnnotationMessage> TYPE = NetworkBufferTemplate.template(
			AnnotationUpdateAction.LIST_TYPE, UpdateAnnotationMessage::actions,
			UpdateAnnotationMessage::new
	);
}