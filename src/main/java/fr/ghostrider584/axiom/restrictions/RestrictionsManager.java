package fr.ghostrider584.axiom.restrictions;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.network.packet.server.RestrictionsMessage;
import net.minestom.server.entity.Player;

import java.util.EnumSet;
import java.util.Set;

public class RestrictionsManager {

	public static void sendDefaultRestrictions(Player player) {
		final var message = new RestrictionsMessage();
		AxiomMinestom.messageRegistry().send(player, message);
	}

	public static void sendRestrictions(Player player,
	                                    EnumSet<AxiomPermission> allowedPermissions,
	                                    EnumSet<AxiomPermission> deniedPermissions,
	                                    int infiniteReachLimit) {

		final var message = new RestrictionsMessage(allowedPermissions, deniedPermissions, infiniteReachLimit, Set.of());
		AxiomMinestom.messageRegistry().send(player, message);
	}

	public static void sendPermissionBasedRestrictions(Player player) {
		final var allowedPermissions = EnumSet.noneOf(AxiomPermission.class);
		final var deniedPermissions = EnumSet.noneOf(AxiomPermission.class);

		for (final var permission : AxiomPermission.values()) {
			if (AxiomPermissions.hasPermission(player, permission)) {
				allowedPermissions.add(permission);
			} else {
				deniedPermissions.add(permission);
			}
		}

		final int infiniteReachLimit = AxiomPermissions.hasPermission(player, AxiomPermission.INFINITE_REACH) ? -1 : 100;
		sendRestrictions(player, allowedPermissions, deniedPermissions, infiniteReachLimit);
	}
}