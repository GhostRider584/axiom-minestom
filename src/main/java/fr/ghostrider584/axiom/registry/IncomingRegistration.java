package fr.ghostrider584.axiom.registry;

import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;

record IncomingRegistration<T extends ClientPacket>(
		Class<T> packetClass,
		NetworkBuffer.Type<T> serializer,
		IncomingMessageHandler<T> handler
) {
}