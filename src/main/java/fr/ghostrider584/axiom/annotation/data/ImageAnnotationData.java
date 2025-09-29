package fr.ghostrider584.axiom.annotation.data;

import fr.ghostrider584.axiom.math.Quaternionf;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.utils.Direction;

import static net.minestom.server.network.NetworkBuffer.*;

public record ImageAnnotationData(
		String imageUrl,
		Point position,
		Quaternionf rotation,
		Direction direction,
		float fallbackYaw,
		float width,
		float opacity,
		byte billboardMode
) implements AnnotationData {

	public static final NetworkBuffer.Type<ImageAnnotationData> TYPE = NetworkBufferTemplate.template(
			STRING, ImageAnnotationData::imageUrl,
			VECTOR3, ImageAnnotationData::position,
			Quaternionf.TYPE, ImageAnnotationData::rotation,
			DIRECTION, ImageAnnotationData::direction,
			FLOAT, ImageAnnotationData::fallbackYaw,
			FLOAT, ImageAnnotationData::width,
			FLOAT, ImageAnnotationData::opacity,
			BYTE, ImageAnnotationData::billboardMode,
			ImageAnnotationData::new
	);

	@Override
	public ImageAnnotationData withPosition(Point position) {
		return new ImageAnnotationData(imageUrl, position, rotation, direction,
				fallbackYaw, width, opacity, billboardMode);
	}

	@Override
	public ImageAnnotationData withRotation(Quaternionf rotation) {
		return new ImageAnnotationData(imageUrl, position, rotation, direction,
				fallbackYaw, width, opacity, billboardMode);
	}
}