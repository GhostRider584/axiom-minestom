package fr.ghostrider584.axiom.annotation.type;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.joml.Quaternionfc;

import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;
import static fr.ghostrider584.axiom.network.JomlNetworkTypes.*;

public record RotateAnnotation(
		UUID uuid,
		Quaternionfc to
) implements AnnotationUpdateAction {

	public static final NetworkBuffer.Type<RotateAnnotation> TYPE = NetworkBufferTemplate.template(
			UUID, RotateAnnotation::uuid,
			JQUATERNION_FC, RotateAnnotation::to,
			RotateAnnotation::new
	);
}