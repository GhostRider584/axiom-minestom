package fr.ghostrider584.axiom.event;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AxiomRemoveEntityEvent extends AxiomCancellablePlayerInstanceEvent {
	private final Entity removedEntity;

	public AxiomRemoveEntityEvent(Player player, Entity removedEntity) {
		super(player);
		this.removedEntity = removedEntity;
	}

	public @NotNull Entity removedEntity() {
		return removedEntity;
	}
}
