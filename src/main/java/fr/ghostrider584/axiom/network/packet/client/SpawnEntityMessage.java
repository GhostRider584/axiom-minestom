package fr.ghostrider584.axiom.network.packet.client;

import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;

import java.util.List;
import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record SpawnEntityMessage(List<SpawnEntry> entries) implements ClientPacket {

	public SpawnEntityMessage {
		entries = List.copyOf(entries);
	}

	public static final NetworkBuffer.Type<SpawnEntityMessage> TYPE = NetworkBufferTemplate.template(
			SpawnEntry.TYPE.list(Short.MAX_VALUE), SpawnEntityMessage::entries,
			SpawnEntityMessage::new
	);

	public record SpawnEntry(
			UUID newUuid,
			double x,
			double y,
			double z,
			float yaw,
			float pitch,
			UUID copyFrom,
			BinaryTag tag
	) {
		public static final NetworkBuffer.Type<SpawnEntry> TYPE = NetworkBufferTemplate.template(
				UUID, SpawnEntry::newUuid,
				DOUBLE, SpawnEntry::x,
				DOUBLE, SpawnEntry::y,
				DOUBLE, SpawnEntry::z,
				FLOAT, SpawnEntry::yaw,
				FLOAT, SpawnEntry::pitch,
				UUID.optional(), SpawnEntry::copyFrom,
				NBT, SpawnEntry::tag,
				SpawnEntry::new
		);
	}
}