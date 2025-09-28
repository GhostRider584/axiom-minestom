package fr.ghostrider584.axiom.metadata.common;

import net.minestom.server.codec.Codec;

import java.util.List;

public final class CommonCodecs {

	public static final Codec<float[]> QUATERNION = Codec.FLOAT.list(4).transform(
			floats -> {
				float[] array = new float[4];
				for (int i = 0; i < Math.min(floats.size(), 4); i++) {
					array[i] = floats.get(i);
				}
				return array;
			},
			array -> List.of(array[0], array[1], array[2], array[3])
	);
}
