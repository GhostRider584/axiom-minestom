package fr.ghostrider584.axiom.registry;

import org.jetbrains.annotations.Nullable;

record ChannelRegistration(
		@Nullable IncomingRegistration<?> incoming,
		@Nullable OutgoingRegistration<?> outgoing
) {
	boolean hasIncoming() {
		return incoming != null;
	}

	boolean hasOutgoing() {
		return outgoing != null;
	}
}