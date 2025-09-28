package fr.ghostrider584.axiom.registry;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;

record OutgoingRegistration<T extends ServerPacket>(
		Class<T> packetClass,
		NetworkBuffer.Type<T> serializer
) {
}