package fr.ghostrider584.axiom.annotation.data;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.utils.Direction;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import static fr.ghostrider584.axiom.network.JomlNetworkTypes.*;
import static net.minestom.server.network.NetworkBuffer.*;

public record ImageAnnotationData(
		String imageUrl,
		Vector3f position,
		Quaternionf rotation,
		Direction direction,
		float fallbackYaw,
		float width,
		float opacity,
		byte billboardMode
) implements AnnotationData {

	public static final NetworkBuffer.Type<ImageAnnotationData> TYPE = NetworkBufferTemplate.template(
			STRING, ImageAnnotationData::imageUrl,
			JVECTOR3_F, ImageAnnotationData::position,
			JQUATERNION_F, ImageAnnotationData::rotation,
			DIRECTION, ImageAnnotationData::direction,
			FLOAT, ImageAnnotationData::fallbackYaw,
			FLOAT, ImageAnnotationData::width,
			FLOAT, ImageAnnotationData::opacity,
			BYTE, ImageAnnotationData::billboardMode,
			ImageAnnotationData::new
	);

	@Override
	public void setPosition(Vector3fc position) {
		this.position.set(position);
	}

	@Override
	public void setRotation(Quaternionfc rotation) {
		this.rotation.set(rotation);
	}
}