package fr.ghostrider584.axiom.network.channel;

import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.ClientPacket;

@FunctionalInterface
public interface IncomingMessageHandler<T extends ClientPacket> {
	void handle(Player player, String channel, T packet);
}