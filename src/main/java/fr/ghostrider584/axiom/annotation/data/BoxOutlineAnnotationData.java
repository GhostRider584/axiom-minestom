package fr.ghostrider584.axiom.annotation.data;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import static net.minestom.server.network.NetworkBuffer.*;

public record BoxOutlineAnnotationData(
		Point from,
		Point to,
		int colour
) implements AnnotationData {

	public static final NetworkBuffer.Type<BoxOutlineAnnotationData> TYPE = NetworkBufferTemplate.template(
			VECTOR3I, BoxOutlineAnnotationData::from,
			VECTOR3I, BoxOutlineAnnotationData::to,
			INT, BoxOutlineAnnotationData::colour,
			BoxOutlineAnnotationData::new
	);

	public BoxOutlineAnnotationData {
		colour = 0xFF000000 | colour;
	}
}