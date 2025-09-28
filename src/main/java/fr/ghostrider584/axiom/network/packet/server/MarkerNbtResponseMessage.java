package fr.ghostrider584.axiom.network.packet.server;

import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;

import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record MarkerNbtResponseMessage(
		UUID uuid,
		BinaryTag nbtData
) implements ServerPacket.Play {

	public static final Type<MarkerNbtResponseMessage> TYPE = NetworkBufferTemplate.template(
			UUID, MarkerNbtResponseMessage::uuid,
			NBT, MarkerNbtResponseMessage::nbtData,
			MarkerNbtResponseMessage::new
	);
}