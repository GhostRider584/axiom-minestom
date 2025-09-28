package fr.ghostrider584.axiom.marker;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.location.RelativeVec;

import java.util.List;

// todo: probably refactor
public class MarkerTags {
	public static final Tag<String> NAME = Tag.String("name");
	public static final Tag<Float> LINE_THICKNESS = Tag.Float("line_thickness");
	public static final Tag<Integer> LINE_ARGB = Tag.Integer("line_argb");
	public static final Tag<Integer> FACE_ARGB = Tag.Integer("face_argb");

	public static final Tag<List<String>> MIN = Tag.String("min").list();
	public static final Tag<List<String>> MAX = Tag.String("max").list();

	public static List<String> vecToList(Vec vec) {
		return List.of(String.valueOf(vec.x()), String.valueOf(vec.y()), String.valueOf(vec.z()));
	}

	public static List<String> relativeVecToList(RelativeVec relativeVec) {
		final var vec = relativeVec.vec();
		final var x = formatRelative(relativeVec.relativeX(), vec.x());
		final var y = formatRelative(relativeVec.relativeY(), vec.y());
		final var z = formatRelative(relativeVec.relativeZ(), vec.z());
		return List.of(x, y, z);
	}

	public static List<String> stringVecToList(String x, String y, String z) {
		return List.of(x, y, z);
	}

	public static String[] listToStringVec(List<String> list) {
		if (list == null || list.size() != 3) return new String[]{"0", "0", "0"};
		return new String[]{list.get(0), list.get(1), list.get(2)};
	}

	public static int argbToInt(int alpha, int red, int green, int blue) {
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}

	public static int[] intToArgb(int argb) {
		int alpha = (argb >> 24) & 0xFF;
		int red = (argb >> 16) & 0xFF;
		int green = (argb >> 8) & 0xFF;
		int blue = argb & 0xFF;

		return new int[]{alpha, red, green, blue};
	}

	private static String formatRelative(boolean isRelative, double value) {
		return (isRelative ? "~" : "") + value;
	}
}
