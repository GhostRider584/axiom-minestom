package fr.ghostrider584.axiom.network.channel;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerChannelRegistry {
	private final Map<UUID, Set<String>> playerChannels = new ConcurrentHashMap<>();

	public boolean registerChannels(@NotNull Player player, @NotNull Collection<String> channels) {
		if (channels.isEmpty()) {
			return false;
		}

		final var existingChannels = playerChannels.computeIfAbsent(player.getUuid(), k -> ConcurrentHashMap.newKeySet());
		return existingChannels.addAll(channels);
	}

	public boolean unregisterChannels(@NotNull Player player, @NotNull Collection<String> channels) {
		final var existingChannels = playerChannels.get(player.getUuid());
		if (existingChannels == null || channels.isEmpty()) {
			return false;
		}

		return existingChannels.removeAll(channels);
	}

	public boolean supports(@NotNull Player player, @NotNull String channel) {
		final var channels = playerChannels.get(player.getUuid());
		return channels != null && channels.contains(channel);
	}

	public @NotNull Set<String> getChannels(@NotNull Player player) {
		final var channels = playerChannels.get(player.getUuid());
		return channels != null ? Set.copyOf(channels) : Set.of();
	}

	public @NotNull Set<String> removePlayer(@NotNull Player player) {
		final var removed = playerChannels.remove(player.getUuid());
		return removed != null ? Set.copyOf(removed) : Set.of();
	}
}