package fr.ghostrider584.axiom.network.channel;

import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ChannelRegistrationManager {
	private static final String REGISTER_CHANNEL = "minecraft:register";
	private static final String UNREGISTER_CHANNEL = "minecraft:unregister";

	private final Logger logger = LoggerFactory.getLogger(ChannelRegistrationManager.class);

	private final PlayerChannelRegistry playerRegistry = new PlayerChannelRegistry();
	private final Set<String> serverChannels = ConcurrentHashMap.newKeySet();

	private final EventNode<@NotNull PlayerEvent> eventNode = EventNode.type("channel_registration", EventFilter.PLAYER)
			.addListener(PlayerPluginMessageEvent.class, this::handlePluginMessage)
			.addListener(PlayerSpawnEvent.class, this::handlePlayerSpawn)
			.addListener(PlayerDisconnectEvent.class, event -> removePlayer(event.getPlayer()));

	public void initialize(EventNode<@NotNull Event> parentNode) {
		parentNode.addChild(eventNode);
		logger.debug("Channel registration manager initialized with {} server channels", serverChannels.size());
	}

	public void registerServerChannel(String channel) {
		if (serverChannels.add(channel)) {
			logger.trace("Registered server channel: {}", channel);
		}
	}

	public void unregisterServerChannel(String channel) {
		if (serverChannels.remove(channel)) {
			logger.trace("Unregistered server channel: {}", channel);
		}
	}

	public boolean isChannelSupported(Player player, String channel) {
		return playerRegistry.supports(player, channel);
	}

	public Set<String> getPlayerChannels(Player player) {
		return playerRegistry.getChannels(player);
	}

	public void removePlayer(Player player) {
		final var removedChannels = playerRegistry.removePlayer(player);
		if (!removedChannels.isEmpty()) {
			logger.trace("Removed channel data for player {}: {}", player.getUsername(), removedChannels);
		}
	}

	private void handlePlayerSpawn(PlayerSpawnEvent event) {
		if (event.isFirstSpawn()) {
			sendServerChannels(event.getPlayer());
		}
	}

	private void handlePluginMessage(PlayerPluginMessageEvent event) {
		final var identifier = event.getIdentifier();

		if (REGISTER_CHANNEL.equals(identifier)) {
			handleChannelRegistration(event.getPlayer(), event.getMessage(), true);
		} else if (UNREGISTER_CHANNEL.equals(identifier)) {
			handleChannelRegistration(event.getPlayer(), event.getMessage(), false);
		}
	}

	private void handleChannelRegistration(Player player, byte[] data, boolean register) {
		final var channels = ChannelListCodec.decode(data);
		if (channels.isEmpty()) {
			return;
		}

		boolean changed;
		if (register) {
			changed = playerRegistry.registerChannels(player, channels);
			if (changed) {
				logger.debug("Player {} registered channels: {}", player.getUsername(), channels);
			}
		} else {
			changed = playerRegistry.unregisterChannels(player, channels);
			if (changed) {
				logger.debug("Player {} unregistered channels: {}", player.getUsername(), channels);
			}
		}
	}

	private void sendServerChannels(Player player) {
		if (serverChannels.isEmpty()) {
			return;
		}

		final byte[] data = ChannelListCodec.encode(serverChannels);
		player.sendPluginMessage(REGISTER_CHANNEL, data);

		logger.debug("Sent {} server channels to {}", serverChannels.size(), player.getUsername());
	}

	public static boolean isRegistrationChannel(String channel) {
		return ChannelRegistrationManager.REGISTER_CHANNEL.equals(channel)
				|| ChannelRegistrationManager.UNREGISTER_CHANNEL.equals(channel);
	}
}