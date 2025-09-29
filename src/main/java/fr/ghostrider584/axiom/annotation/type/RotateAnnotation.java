package fr.ghostrider584.axiom.annotation.type;

import fr.ghostrider584.axiom.math.Quaternionf;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record RotateAnnotation(
		UUID uuid,
		Quaternionf to
) implements AnnotationUpdateAction {

	public static final NetworkBuffer.Type<RotateAnnotation> TYPE = NetworkBufferTemplate.template(
			UUID, RotateAnnotation::uuid,
			Quaternionf.TYPE, RotateAnnotation::to,
			RotateAnnotation::new
	);
}