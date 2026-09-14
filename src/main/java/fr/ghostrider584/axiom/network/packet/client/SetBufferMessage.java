package fr.ghostrider584.axiom.network.packet.client;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record SetBufferMessage(
		String worldKey,
		UUID bufferUuid,
		byte bufferType,
		NetworkBuffer bufferData
) implements ClientPacket.Play {

	public static final NetworkBuffer.Type<SetBufferMessage> TYPE = new NetworkBuffer.Type<>() {
		@Override
		public void write(@NotNull NetworkBuffer buffer, @NotNull SetBufferMessage value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull SetBufferMessage read(@NotNull NetworkBuffer buffer) {
			final var worldKey = buffer.read(STRING);
			final var bufferUuid = buffer.read(UUID);
			final byte bufferType = buffer.read(BYTE);

			if (bufferType != 0 && bufferType != 1) throw new IllegalArgumentException("Unsupported edit buffer type");
			if (buffer.readableBytes() > fr.ghostrider584.axiom.world.EditBufferDecoder.MAX_WIRE_BYTES)
				throw new IllegalArgumentException("Edit buffer exceeds the wire budget");
			// The bounded copy belongs to the asynchronous decoder, not the packet event.
			final int remainingBytes = (int) buffer.readableBytes();
			final byte[] remainingData = new byte[remainingBytes];
			buffer.copyTo(buffer.readIndex(), remainingData, 0, remainingBytes);
			buffer.advanceRead(remainingBytes);

			final var subBuffer = NetworkBuffer.wrap(remainingData, 0, remainingData.length);
			return new SetBufferMessage(worldKey, bufferUuid, bufferType, subBuffer);
		}
	};
}
