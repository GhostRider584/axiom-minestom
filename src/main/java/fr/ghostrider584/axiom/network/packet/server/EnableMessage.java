package fr.ghostrider584.axiom.network.packet.server;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;

import static net.minestom.server.network.NetworkBuffer.*;

public record EnableMessage(boolean enable, java.util.List<String> incomingChannels) implements ServerPacket.Play {
	public EnableMessage { incomingChannels = java.util.List.copyOf(incomingChannels); }

	/** Axiom API 10 ServerConfig v0; old pre-tunnel enable packets are intentionally retired. */
	public static final Type<EnableMessage> TYPE = new Type<>() {
		public void write(NetworkBuffer buffer, EnableMessage value) {
			buffer.write(BOOLEAN, value.enable());
			if (!value.enable()) return;
			buffer.write(BYTE, (byte) 0);
			buffer.write(VAR_INT, 2);
			buffer.write(INT, 31_000);
			buffer.write(INT, fr.ghostrider584.axiom.world.EditBufferDecoder.MAX_WIRE_BYTES);
			buffer.write(VAR_INT, 0); buffer.write(VAR_INT, 0);
			buffer.write(STRING.list(), value.incomingChannels());
		}
		public EnableMessage read(NetworkBuffer buffer) { throw new UnsupportedOperationException(); }
	};
}
