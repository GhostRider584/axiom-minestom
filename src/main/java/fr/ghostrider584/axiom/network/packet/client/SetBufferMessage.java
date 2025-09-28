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
) implements ClientPacket {

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

			// TODO: handle the data here instead of in SetBufferHandler
			final long remainingBytes = buffer.readableBytes();
			final byte[] remainingData = new byte[(int) remainingBytes];
			buffer.copyTo(buffer.readIndex(), remainingData, 0, remainingBytes);
			buffer.advanceRead(remainingBytes);

			final var subBuffer = NetworkBuffer.wrap(remainingData, 0, remainingData.length);
			return new SetBufferMessage(worldKey, bufferUuid, bufferType, subBuffer);
		}
	};
}