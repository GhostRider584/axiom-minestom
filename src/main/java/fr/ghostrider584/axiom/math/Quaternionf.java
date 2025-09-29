package fr.ghostrider584.axiom.math;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import static net.minestom.server.network.NetworkBuffer.QUATERNION;

public record Quaternionf(float x, float y, float z, float w) {

	public Quaternionf(float[] values) {
		this(values[0], values[1], values[2], values[3]);
	}

	public float[] toArray() {
		return new float[]{x, y, z, w};
	}

	public static final NetworkBuffer.Type<Quaternionf> TYPE =
			NetworkBufferTemplate.template(QUATERNION, Quaternionf::toArray, Quaternionf::new);
}
