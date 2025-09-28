package fr.ghostrider584.axiom.annotation.type;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record DeleteAnnotation(
		UUID uuid
) implements AnnotationUpdateAction {

	public static final NetworkBuffer.Type<DeleteAnnotation> TYPE = NetworkBufferTemplate.template(
			UUID, DeleteAnnotation::uuid,
			DeleteAnnotation::new
	);
}