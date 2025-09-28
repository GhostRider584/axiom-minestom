package fr.ghostrider584.axiom.annotation.data;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import static net.minestom.server.network.NetworkBuffer.*;

public record FreehandOutlineAnnotationData(
		Point start,
		int offsetCount,
		int colour,
		byte[] offsets
) implements AnnotationData {

	public static final NetworkBuffer.Type<FreehandOutlineAnnotationData> TYPE = NetworkBufferTemplate.template(
			VECTOR3I, FreehandOutlineAnnotationData::start,
			VAR_INT, FreehandOutlineAnnotationData::offsetCount,
			INT, FreehandOutlineAnnotationData::colour,
			BYTE_ARRAY, FreehandOutlineAnnotationData::offsets,
			FreehandOutlineAnnotationData::new
	);

	public FreehandOutlineAnnotationData {
		colour = 0xFF000000 | colour;
	}
}