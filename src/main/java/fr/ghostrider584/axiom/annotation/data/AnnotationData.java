package fr.ghostrider584.axiom.annotation.data;

import fr.ghostrider584.axiom.math.Quaternionf;
import fr.ghostrider584.axiom.network.IdentifiedNetworkType;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;

public interface AnnotationData {
	NetworkBuffer.Type<AnnotationData> TYPE = IdentifiedNetworkType.Polymorphic(AnnotationDataType.values());

	default AnnotationData withPosition(Point position) {
		return null;
	}

	default AnnotationData withRotation(Quaternionf rotation) {
		return null;
	}
}