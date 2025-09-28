package fr.ghostrider584.axiom.event;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AxiomSpawnEntityEvent extends AxiomCancellablePlayerInstanceEvent {
	private final Entity spawnedEntity;

	public AxiomSpawnEntityEvent(Player player, Entity spawnedEntity) {
		super(player);
		this.spawnedEntity = spawnedEntity;
	}

	public @NotNull Entity spawnedEntity() {
		return spawnedEntity;
	}
}
