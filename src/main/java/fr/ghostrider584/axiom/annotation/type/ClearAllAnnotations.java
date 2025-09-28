package fr.ghostrider584.axiom.annotation.type;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record ClearAllAnnotations() implements AnnotationUpdateAction {
	public static final NetworkBuffer.Type<ClearAllAnnotations> TYPE = NetworkBufferTemplate.template(ClearAllAnnotations::new);
}