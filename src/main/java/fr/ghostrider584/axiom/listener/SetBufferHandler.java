package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.dispatch.DispatchSendsManager;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.SetBufferMessage;
import fr.ghostrider584.axiom.world.AxiomEdits;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;

public class SetBufferHandler implements IncomingMessageHandler<SetBufferMessage> {
    @Override public void handle(Player player, String channel, SetBufferMessage packet) {
        AxiomEdits.submit(player, packet).whenComplete((_, failure) -> {
            // These are flow-control credits, not a success acknowledgement. One in-flight
            // section per client keeps several builders inside the shared world's edit budget.
            DispatchSendsManager.sendUpdateAvailableDispatchSends(player, 1, 1);
            if (failure != null) player.sendMessage(Component.text("Axiom edit failed (some earlier slices may have applied): "
                    + failure.getMessage(), NamedTextColor.RED));
        });
    }
}
