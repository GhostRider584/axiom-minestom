package fr.ghostrider584.axiom.annotation.data;

import fr.ghostrider584.axiom.network.IdentifiedNetworkType;
import net.minestom.server.network.NetworkBuffer;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public interface AnnotationData {
	NetworkBuffer.Type<AnnotationData> TYPE = IdentifiedNetworkType.Polymorphic(AnnotationDataType.values());

	default void setPosition(Vector3fc position) {
	}

	default void setRotation(Quaternionfc rotation) {
	}
}