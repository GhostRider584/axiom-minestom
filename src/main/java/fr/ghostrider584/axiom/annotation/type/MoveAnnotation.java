package fr.ghostrider584.axiom.annotation.type;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.joml.Vector3fc;

import java.util.UUID;

import static fr.ghostrider584.axiom.network.JomlNetworkTypes.*;
import static net.minestom.server.network.NetworkBuffer.*;

public record MoveAnnotation(
		UUID uuid,
		Vector3fc to
) implements AnnotationUpdateAction {

	public static final NetworkBuffer.Type<MoveAnnotation> TYPE = NetworkBufferTemplate.template(
			UUID, MoveAnnotation::uuid,
			JVECTOR3_FC, MoveAnnotation::to,
			MoveAnnotation::new
	);
}