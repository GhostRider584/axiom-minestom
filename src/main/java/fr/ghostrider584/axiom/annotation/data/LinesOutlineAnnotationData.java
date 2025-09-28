package fr.ghostrider584.axiom.annotation.data;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import static net.minestom.server.network.NetworkBuffer.*;

public record LinesOutlineAnnotationData(
		long[] positions,
		int colour
) implements AnnotationData {

	public static final NetworkBuffer.Type<LinesOutlineAnnotationData> TYPE = NetworkBufferTemplate.template(
			LONG_ARRAY, LinesOutlineAnnotationData::positions,
			INT, LinesOutlineAnnotationData::colour,
			LinesOutlineAnnotationData::new
	);

	public LinesOutlineAnnotationData {
		colour = 0xFF000000 | colour;
	}
}