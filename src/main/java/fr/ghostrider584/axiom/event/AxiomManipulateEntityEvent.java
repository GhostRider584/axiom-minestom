package fr.ghostrider584.axiom.event;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AxiomManipulateEntityEvent extends AxiomCancellablePlayerInstanceEvent {
	private final Entity targetEntity;

	public AxiomManipulateEntityEvent(Player player, Entity targetEntity) {
		super(player);
		this.targetEntity = targetEntity;
	}

	public @NotNull Entity targetEntity() {
		return targetEntity;
	}
}
