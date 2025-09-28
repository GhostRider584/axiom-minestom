package fr.ghostrider584.axiom.network.packet.client;

import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

// todo: refactor & clean up
public record ManipulateEntityMessage(
		List<ManipulateEntry> entries
) implements ClientPacket {

	public static final NetworkBuffer.Type<ManipulateEntityMessage> TYPE = new NetworkBuffer.Type<>() {
		@Override
		public void write(@NotNull NetworkBuffer buffer, ManipulateEntityMessage value) {
			throw new UnsupportedOperationException("Writing not implemented");
		}

		@Override
		public ManipulateEntityMessage read(NetworkBuffer buffer) {
			final int entryCount = buffer.read(VAR_INT);
			final var entries = new ArrayList<ManipulateEntry>(entryCount);

			for (int i = 0; i < entryCount; i++) {
				entries.add(readManipulateEntry(buffer));
			}

			return new ManipulateEntityMessage(entries);
		}

		private ManipulateEntry readManipulateEntry(NetworkBuffer buffer) {
			final var uuid = buffer.read(UUID);
			final int flags = buffer.read(BYTE);

			Set<Relative> relativeMovementSet = null;
			Pos position = null;
			float yaw = 0;
			float pitch = 0;

			if (flags >= 0) {
				relativeMovementSet = parseRelativeSet(flags);
				double x = buffer.read(DOUBLE);
				double y = buffer.read(DOUBLE);
				double z = buffer.read(DOUBLE);
				yaw = buffer.read(FLOAT);
				pitch = buffer.read(FLOAT);
				position = new Pos(x, y, z, yaw, pitch);
			}

			final var nbt = buffer.read(NBT);
			final var passengerManipulation = buffer.read(PassengerManipulation.SERIALIZER);
			final var passengers = new ArrayList<UUID>();

			if (passengerManipulation == PassengerManipulation.ADD_LIST
					|| passengerManipulation == PassengerManipulation.REMOVE_LIST) {

				int passengerCount = buffer.read(VAR_INT);
				for (int j = 0; j < passengerCount; j++) {
					passengers.add(buffer.read(UUID));
				}
			}

			return new ManipulateEntry(uuid, relativeMovementSet, position, nbt, passengerManipulation, passengers);
		}

		private Set<Relative> parseRelativeSet(int flags) {
			final var relatives = new HashSet<Relative>();
			if ((flags & 0x01) != 0) relatives.add(Relative.X);
			if ((flags & 0x02) != 0) relatives.add(Relative.Y);
			if ((flags & 0x04) != 0) relatives.add(Relative.Z);
			if ((flags & 0x08) != 0) relatives.add(Relative.Y_ROT);
			if ((flags & 0x10) != 0) relatives.add(Relative.X_ROT);
			return relatives;
		}
	};

	public enum PassengerManipulation {
		NONE,
		REMOVE_ALL,
		ADD_LIST,
		REMOVE_LIST;

		public static final Type<PassengerManipulation> SERIALIZER = NetworkBuffer.Enum(PassengerManipulation.class);
	}

	public enum Relative {
		X, Y, Z, Y_ROT, X_ROT
	}

	public record ManipulateEntry(
			UUID uuid,
			@Nullable Set<Relative> relativeMovementSet,
			@Nullable Pos position,
			BinaryTag nbt,
			PassengerManipulation passengerManipulation,
			List<UUID> passengers
	) {
	}
}