package fr.ghostrider584.axiom.annotation.type;

import fr.ghostrider584.axiom.annotation.data.AnnotationData;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record CreateAnnotation(
		UUID uuid,
		AnnotationData annotationData
) implements AnnotationUpdateAction {

	public static final NetworkBuffer.Type<CreateAnnotation> TYPE = NetworkBufferTemplate.template(
			UUID, CreateAnnotation::uuid,
			AnnotationData.TYPE, CreateAnnotation::annotationData,
			CreateAnnotation::new
	);
}