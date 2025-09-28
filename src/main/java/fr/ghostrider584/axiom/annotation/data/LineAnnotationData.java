package fr.ghostrider584.axiom.annotation.data;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import static net.minestom.server.network.NetworkBuffer.*;

public record LineAnnotationData(
		Point startQuantized,
		float lineWidth,
		int colour,
		byte[] offsets
) implements AnnotationData {

	public static final NetworkBuffer.Type<LineAnnotationData> TYPE = NetworkBufferTemplate.template(
			VECTOR3I, LineAnnotationData::startQuantized,
			FLOAT, LineAnnotationData::lineWidth,
			INT, LineAnnotationData::colour,
			BYTE_ARRAY, LineAnnotationData::offsets,
			LineAnnotationData::new
	);

	public LineAnnotationData {
		colour = 0xFF000000 | colour;
	}
}