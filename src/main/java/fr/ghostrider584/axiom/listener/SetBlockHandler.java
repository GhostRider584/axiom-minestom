package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.SetBlockMessage;
import fr.ghostrider584.axiom.world.AxiomEdits;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;

public class SetBlockHandler implements IncomingMessageHandler<SetBlockMessage> {
    @Override public void handle(Player player, String channel, SetBlockMessage packet) {
        AxiomEdits.submit(player, packet).whenComplete((_, failure) -> {
            if (failure != null) player.sendMessage(Component.text("Axiom edit failed: " + failure.getMessage(), NamedTextColor.RED));
        });
    }
}
