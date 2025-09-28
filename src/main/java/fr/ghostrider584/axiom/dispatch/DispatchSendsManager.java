package fr.ghostrider584.axiom.dispatch;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.network.packet.server.UpdateAvailableDispatchSendsMessage;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: implement "dispatch sends" system
public class DispatchSendsManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(DispatchSendsManager.class);

	public static void sendUpdateAvailableDispatchSends(Player player, int add, int max) {
		final var message = new UpdateAvailableDispatchSendsMessage(add, max);
		AxiomMinestom.messageRegistry().send(player, message);
	}
}