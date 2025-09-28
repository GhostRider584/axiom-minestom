package fr.ghostrider584.axiom.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.CancellableEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;

abstract class AxiomCancellablePlayerInstanceEvent implements PlayerInstanceEvent, CancellableEvent, AxiomEvent {
	private final @NotNull Player player;
	private boolean cancelled;

	AxiomCancellablePlayerInstanceEvent(@NotNull Player player) {
		this.player = player;
	}

	@Override
	public @NotNull Player getPlayer() {
		return player;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}
}
