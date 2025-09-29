package fr.ghostrider584.axiom.annotation.type;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record MoveAnnotation(
		UUID uuid,
		Point to
) implements AnnotationUpdateAction {

	public static final NetworkBuffer.Type<MoveAnnotation> TYPE = NetworkBufferTemplate.template(
			UUID, MoveAnnotation::uuid,
			VECTOR3, MoveAnnotation::to,
			MoveAnnotation::new
	);
}