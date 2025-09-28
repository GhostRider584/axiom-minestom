package fr.ghostrider584.axiom.event;

import net.minestom.server.entity.Player;

public final class AxiomFlySpeedChangeEvent extends AxiomCancellablePlayerInstanceEvent {
	private final float previousFlySpeed;
	private final float newFlySpeed;

	public AxiomFlySpeedChangeEvent(Player player, float previousFlySpeed, float newFlySpeed) {
		super(player);
		this.previousFlySpeed = previousFlySpeed;
		this.newFlySpeed = newFlySpeed;
	}

	public float previousFlySpeed() {
		return previousFlySpeed;
	}

	public float newFlySpeed() {
		return newFlySpeed;
	}
}
