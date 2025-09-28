package fr.ghostrider584.axiom.registry;

import fr.ghostrider584.axiom.listener.*;
import fr.ghostrider584.axiom.network.channel.ChannelRegistrationManager;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.*;
import fr.ghostrider584.axiom.network.packet.server.*;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.server.utils.PacketSendingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class AxiomMessageRegistry {
	private final Logger logger = LoggerFactory.getLogger(AxiomMessageRegistry.class);

	private final Map<String, ChannelRegistration> channels = new HashMap<>();
	private final Map<Class<?>, String> packetToChannel = new HashMap<>();

	private final EventNode<@NotNull PlayerEvent> eventNode = EventNode.type("axiom", EventFilter.PLAYER)
			.addListener(PlayerPluginMessageEvent.class, this::handlePluginMessage);

	private final ChannelRegistrationManager channelRegistrationManager;

	public AxiomMessageRegistry(ChannelRegistrationManager channelRegistrationManager) {
		this.channelRegistrationManager = channelRegistrationManager;
	}

	public void initialize(EventNode<@NotNull Event> eventNode) {
		eventNode.addChild(this.eventNode);
	}

	public void detach(EventNode<@NotNull Event> eventNode) {
		eventNode.removeChild(this.eventNode);
	}

	public <T extends ClientPacket> void registerIncoming(String channel,
	                                                      Class<T> packetClass,
	                                                      NetworkBuffer.Type<T> serializer,
	                                                      IncomingMessageHandler<T> handler) {

		final var incoming = new IncomingRegistration<>(packetClass, serializer, handler);
		updateChannelRegistration(channel, incoming, null);

		packetToChannel.put(packetClass, channel);
		logger.trace("Registered incoming: {} -> {}", channel, packetClass.getSimpleName());
	}

	public <T extends ServerPacket> void registerOutgoing(String channel,
	                                                      Class<T> packetClass,
	                                                      NetworkBuffer.Type<T> serializer) {

		final var outgoing = new OutgoingRegistration<>(packetClass, serializer);
		updateChannelRegistration(channel, null, outgoing);

		packetToChannel.put(packetClass, channel);
		logger.trace("Registered outgoing: {} -> {}", channel, packetClass.getSimpleName());
	}

	private void updateChannelRegistration(String channel,
	                                       @Nullable IncomingRegistration<?> incoming,
	                                       @Nullable OutgoingRegistration<?> outgoing) {

		final ChannelRegistration existing = channels.get(channel);
		ChannelRegistration updated;

		if (existing == null) {
			updated = new ChannelRegistration(incoming, outgoing);
			channelRegistrationManager.registerServerChannel(channel);
			logger.trace("Registered new channel: {}", channel);
		} else {
			updated = new ChannelRegistration(
					incoming != null ? incoming : existing.incoming(),
					outgoing != null ? outgoing : existing.outgoing()
			);
		}

		channels.put(channel, updated);
	}

	private void handlePluginMessage(PlayerPluginMessageEvent event) {
		final var identifier = event.getIdentifier();
		if (ChannelRegistrationManager.isRegistrationChannel(identifier)) {
			return;
		}

		if (!identifier.startsWith("axiom")) {
			return;
		}

		final var buffer = NetworkBuffer.wrap(event.getMessage(), 0, event.getMessage().length);
		handleIncomingPacket(event.getPlayer(), identifier, buffer);
	}

	@SuppressWarnings("unchecked")
	private void handleIncomingPacket(Player player, String channel, NetworkBuffer buffer) {
		logger.trace("Received packet on channel \"{}\" from {} ({} bytes)", channel, player.getUsername(), buffer.readableBytes());

		final var registration = channels.get(channel);
		if (registration == null || !registration.hasIncoming()) {
			logger.warn("No incoming handler registered for channel: {}", channel);
			return;
		}

		try {
			final var incoming = registration.incoming();
			Objects.requireNonNull(incoming, "Incoming registration cannot be null");

			final var packet = incoming.serializer().read(buffer);
			((IncomingMessageHandler<ClientPacket>) incoming.handler()).handle(player, channel, packet);
		} catch (Exception e) {
			logger.error("Error handling packet from channel {} for player {}: {}", channel, player.getUsername(), e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public void sendGrouped(Collection<Player> players, ServerPacket packet) {
		final var channel = packetToChannel.get(packet.getClass());
		if (channel == null) {
			logger.warn("No channel registered for packet class: {}", packet.getClass().getSimpleName());
			return;
		}

		final var registration = channels.get(channel);
		if (registration == null || !registration.hasOutgoing()) {
			logger.warn("No outgoing serializer registered for channel: {}", channel);
			return;
		}

		final var supportedPlayers = players.stream()
				.filter(player -> channelRegistrationManager.isChannelSupported(player, channel))
				.collect(Collectors.toList());

		if (supportedPlayers.isEmpty()) {
			return;
		}

		try {
			final var outgoing = registration.outgoing();
			Objects.requireNonNull(outgoing, "Outgoing registration cannot be null");

			final byte[] data = NetworkBuffer.makeArray((NetworkBuffer.Type<ServerPacket>) outgoing.serializer(), packet);
			final var pluginMessage = new PluginMessagePacket(channel, data);
			PacketSendingUtils.sendGroupedPacket(supportedPlayers, pluginMessage);

			logger.trace("Sent packet on channel \"{}\" to {} players ({} bytes)", channel, supportedPlayers.size(), data.length);
		} catch (Exception e) {
			logger.error("Error handling outgoing packet on channel {}: {}", channel, e.getMessage(), e);
		}
	}

	public void send(Player player, ServerPacket packet) {
		sendGrouped(List.of(player), packet);
	}

	public static AxiomMessageRegistry defaultRegistry(ChannelRegistrationManager channelRegistrationManager) {
		final var registry = new AxiomMessageRegistry(channelRegistrationManager);

		// outgoing messages
		registry.registerOutgoing(AxiomPluginChannel.Server.ENABLE, EnableMessage.class, EnableMessage.TYPE);
		registry.registerOutgoing(AxiomPluginChannel.Server.RESTRICTIONS, RestrictionsMessage.class, RestrictionsMessage.TYPE);
		registry.registerOutgoing(AxiomPluginChannel.Server.MARKER_NBT_RESPONSE, MarkerNbtResponseMessage.class, MarkerNbtResponseMessage.TYPE);
		registry.registerOutgoing(AxiomPluginChannel.Server.MARKER_DATA, MarkerDataMessage.class, MarkerDataMessage.TYPE);
		registry.registerOutgoing(AxiomPluginChannel.Server.RESPONSE_ENTITY_DATA, ResponseEntityDataMessage.class, ResponseEntityDataMessage.TYPE);
		registry.registerOutgoing(AxiomPluginChannel.Server.UPDATE_AVAILABLE_DISPATCH_SENDS, UpdateAvailableDispatchSendsMessage.class, UpdateAvailableDispatchSendsMessage.TYPE);
		registry.registerOutgoing(AxiomPluginChannel.Bidirectional.ANNOTATION_UPDATE, AnnotationUpdateResponse.class, AnnotationUpdateResponse.TYPE);

		// incoming messages
		registry.registerIncoming(AxiomPluginChannel.Client.HELLO, HelloMessage.class, HelloMessage.TYPE, new HelloMessageHandler());
		registry.registerIncoming(AxiomPluginChannel.Client.SET_GAME_MODE, SetGameModeMessage.class, SetGameModeMessage.TYPE, new SetGameModeHandler());
		registry.registerIncoming(AxiomPluginChannel.Client.SET_FLY_SPEED, SetFlySpeedMessage.class, SetFlySpeedMessage.TYPE, new SetFlySpeedHandler());
		registry.registerIncoming(AxiomPluginChannel.Client.MARKER_NBT_REQUEST, MarkerNbtRequestMessage.class, MarkerNbtRequestMessage.TYPE, new MarkerNbtRequestHandler());
		registry.registerIncoming(AxiomPluginChannel.Client.SET_BUFFER, SetBufferMessage.class, SetBufferMessage.TYPE, new SetBufferHandler());
		registry.registerIncoming(AxiomPluginChannel.Client.SET_BLOCK, SetBlockMessage.class, SetBlockMessage.TYPE, new SetBlockHandler());
		registry.registerIncoming(AxiomPluginChannel.Client.SPAWN_ENTITY, SpawnEntityMessage.class, SpawnEntityMessage.TYPE, new SpawnEntityHandler());
		registry.registerIncoming(AxiomPluginChannel.Client.MANIPULATE_ENTITY, ManipulateEntityMessage.class, ManipulateEntityMessage.TYPE, new ManipulateEntityHandler());
		registry.registerIncoming(AxiomPluginChannel.Client.DELETE_ENTITY, DeleteEntityMessage.class, DeleteEntityMessage.TYPE, new DeleteEntityHandler());
		registry.registerIncoming(AxiomPluginChannel.Client.REQUEST_ENTITY_DATA, RequestEntityDataMessage.class, RequestEntityDataMessage.TYPE, new RequestEntityDataHandler());
		registry.registerIncoming(AxiomPluginChannel.Client.TELEPORT, TeleportMessage.class, TeleportMessage.TYPE, new TeleportHandler());
		registry.registerIncoming(AxiomPluginChannel.Bidirectional.ANNOTATION_UPDATE, UpdateAnnotationMessage.class, UpdateAnnotationMessage.TYPE, new UpdateAnnotationHandler());

		return registry;
	}
}