package fr.ghostrider584.axiom.util;

import net.minestom.server.coordinate.Point;

public final class WorldBoundaries {

	public static boolean isInValidBounds(Point pos) {
		return !isOutsideSpawnableHeight(pos.y()) && isInWorldBoundsHorizontal(pos);
	}

	public static boolean isInWorldBoundsHorizontal(Point pos) {
		return pos.x() >= -30000000 && pos.z() >= -30000000 && pos.x() < 30000000 && pos.z() < 30000000;
	}

	public static boolean isOutsideSpawnableHeight(double y) {
		return y < -20000000 || y >= 20000000;
	}
}
