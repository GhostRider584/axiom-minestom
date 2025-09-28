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

public record TextAnnotationData(
		String text,
		Vector3f position,
		Quaternionf rotation,
		Direction direction,
		float fallbackYaw,
		float scale,
		byte billboardMode,
		int colour,
		boolean shadow
) implements AnnotationData {

	public static final NetworkBuffer.Type<TextAnnotationData> TYPE = NetworkBufferTemplate.template(
			STRING, TextAnnotationData::text,
			JVECTOR3_F, TextAnnotationData::position,
			JQUATERNION_F, TextAnnotationData::rotation,
			DIRECTION, TextAnnotationData::direction,
			FLOAT, TextAnnotationData::fallbackYaw,
			FLOAT, TextAnnotationData::scale,
			BYTE, TextAnnotationData::billboardMode,
			INT, TextAnnotationData::colour,
			BOOLEAN, TextAnnotationData::shadow,
			TextAnnotationData::new
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