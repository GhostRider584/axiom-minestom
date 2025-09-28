package fr.ghostrider584.axiom.network.packet.server;

import fr.ghostrider584.axiom.annotation.type.AnnotationUpdateAction;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;

import java.util.List;

public record AnnotationUpdateResponse(
		List<AnnotationUpdateAction> actions
) implements ServerPacket.Play {

	public static final NetworkBuffer.Type<AnnotationUpdateResponse> TYPE = NetworkBufferTemplate.template(
			AnnotationUpdateAction.LIST_TYPE, AnnotationUpdateResponse::actions,
			AnnotationUpdateResponse::new
	);
}