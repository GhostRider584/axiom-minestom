package fr.ghostrider584.axiom.annotation.type;

import fr.ghostrider584.axiom.network.IdentifiedNetworkType;
import net.minestom.server.network.NetworkBuffer;

import java.util.List;

public interface AnnotationUpdateAction {
	NetworkBuffer.Type<AnnotationUpdateAction> TYPE = IdentifiedNetworkType.Polymorphic(AnnotationType.values());
	NetworkBuffer.Type<List<AnnotationUpdateAction>> LIST_TYPE = TYPE.list(256);
}