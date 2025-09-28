package fr.ghostrider584.axiom.event;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public final class AxiomTeleportEvent extends AxiomCancellablePlayerInstanceEvent {
	private final Instance targetInstance;
	private final Pos position;

	public AxiomTeleportEvent(Player player, Instance targetInstance, Pos position) {
		super(player);
		this.targetInstance = targetInstance;
		this.position = position;
	}

	public @NotNull Instance targetInstance() {
		return targetInstance;
	}

	public @NotNull Pos position() {
		return position;
	}
}
