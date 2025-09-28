package fr.ghostrider584.axiom.event;

import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AxiomGameModeChangeEvent extends AxiomCancellablePlayerInstanceEvent {
	private final GameMode previousGameMode;
	private final GameMode newGameMode;

	public AxiomGameModeChangeEvent(Player player, GameMode previousGameMode, GameMode newGameMode) {
		super(player);
		this.previousGameMode = previousGameMode;
		this.newGameMode = newGameMode;
	}

	public @NotNull GameMode previousGameMode() {
		return previousGameMode;
	}

	public @NotNull GameMode newGameMode() {
		return newGameMode;
	}
}
