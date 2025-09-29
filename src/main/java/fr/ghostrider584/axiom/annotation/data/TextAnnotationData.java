package fr.ghostrider584.axiom.annotation.data;

import fr.ghostrider584.axiom.math.Quaternionf;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.utils.Direction;

import static net.minestom.server.network.NetworkBuffer.*;

public record TextAnnotationData(
		String text,
		Point position,
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
			VECTOR3, TextAnnotationData::position,
			Quaternionf.TYPE, TextAnnotationData::rotation,
			DIRECTION, TextAnnotationData::direction,
			FLOAT, TextAnnotationData::fallbackYaw,
			FLOAT, TextAnnotationData::scale,
			BYTE, TextAnnotationData::billboardMode,
			INT, TextAnnotationData::colour,
			BOOLEAN, TextAnnotationData::shadow,
			TextAnnotationData::new
	);

	@Override
	public TextAnnotationData withPosition(Point position) {
		return new TextAnnotationData(text, position, rotation, direction,
				fallbackYaw, scale, billboardMode, colour, shadow);
	}

	@Override
	public TextAnnotationData withRotation(Quaternionf rotation) {
		return new TextAnnotationData(text, position, rotation, direction,
				fallbackYaw, scale, billboardMode, colour, shadow);
	}
}