package fr.ghostrider584.axiom.util;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

public final class JomlConvert {

	private JomlConvert() {
	}

	public static @NotNull Vec toMinestomVec(@NotNull Vector3fc vec) {
		return new Vec(vec.x(), vec.y(), vec.z());
	}

	public static @NotNull Vec toMinestomVec(@NotNull Vector3dc vec) {
		return new Vec(vec.x(), vec.y(), vec.z());
	}

	public static @NotNull Vector3d toJomlDoubleVec(@NotNull Point point) {
		return new Vector3d(point.x(), point.y(), point.z());
	}

	public static @NotNull Vector3f toJomlFloatVec(@NotNull Point point) {
		return new Vector3f((float) point.x(), (float) point.y(), (float) point.z());
	}

	public static double[] toDoubleArray(@NotNull Quaterniondc quaternion) {
		return new double[]{quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w()};
	}

	public static float[] toFloatArray(@NotNull Quaternionfc quaternion) {
		return new float[]{quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w()};
	}

	public static double[] toDoubleArray(@NotNull Quaternionfc quaternion) {
		return new double[]{(double) quaternion.x(), (double) quaternion.y(), (double) quaternion.z(), (double) quaternion.w()};
	}

	public static float[] toFloatArray(@NotNull Quaterniondc quaternion) {
		return new float[]{(float) quaternion.x(), (float) quaternion.y(), (float) quaternion.z(), (float) quaternion.w()};
	}

	public static @NotNull Quaterniond toQuaterniond(double @NotNull [] array) {
		if (array.length != 4) {
			throw new IllegalArgumentException("Array must have 4 elements for Quaterniond conversion.");
		}
		return new Quaterniond(array[0], array[1], array[2], array[3]);
	}

	public static @NotNull Quaterniond toQuaterniond(float @NotNull [] array) {
		if (array.length != 4) {
			throw new IllegalArgumentException("Array must have 4 elements for Quaterniond conversion.");
		}
		return new Quaterniond(array[0], array[1], array[2], array[3]);
	}

	public static @NotNull Quaternionf toQuaternionf(float @NotNull [] array) {
		if (array.length != 4) {
			throw new IllegalArgumentException("Array must have 4 elements for Quaternionf conversion.");
		}
		return new Quaternionf(array[0], array[1], array[2], array[3]);
	}

	public static @NotNull Quaternionf toQuaternionf(double @NotNull [] array) {
		if (array.length != 4) {
			throw new IllegalArgumentException("Array must have 4 elements for Quaternionf conversion.");
		}
		return new Quaternionf((float) array[0], (float) array[1], (float) array[2], (float) array[3]);
	}
}
