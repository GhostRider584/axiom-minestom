package fr.ghostrider584.axiom.event;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

public class AxiomUnknownTeleportEvent extends AxiomCancellablePlayerInstanceEvent {
	private final Key dimension;
	private final Pos position;

	public AxiomUnknownTeleportEvent(Player player, Key dimension, Pos position) {
		super(player);
		this.dimension = dimension;
		this.position = position;
	}

	public Key dimension() {
		return dimension;
	}

	public Pos position() {
		return position;
	}
}