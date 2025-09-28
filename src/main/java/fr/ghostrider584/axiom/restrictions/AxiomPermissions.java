package fr.ghostrider584.axiom.restrictions;

import net.minestom.server.entity.Player;

import java.util.function.BiPredicate;

public class AxiomPermissions {
	private static BiPredicate<Player, AxiomPermission> permissionPredicate = (player, axiomPermission) -> true;

	public static void setPermissionPredicate(BiPredicate<Player, AxiomPermission> predicate) {
		permissionPredicate = predicate;
	}

	public static boolean hasPermission(Player player, AxiomPermission permission) {
		return permissionPredicate.test(player, permission);
	}
}
