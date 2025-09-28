package fr.ghostrider584.axiom;

import fr.ghostrider584.axiom.annotation.AnnotationConfig;
import fr.ghostrider584.axiom.annotation.AnnotationRegistry;
import fr.ghostrider584.axiom.annotation.storage.AnnotationStorage;
import fr.ghostrider584.axiom.annotation.storage.json.JsonAnnotationStorage;
import fr.ghostrider584.axiom.marker.MarkerManager;
import fr.ghostrider584.axiom.registry.AxiomMessageRegistry;
import fr.ghostrider584.axiom.network.channel.ChannelRegistrationManager;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class AxiomMinestom {

	private static final Logger LOGGER = LoggerFactory.getLogger(AxiomMinestom.class);
	private static final Set<Player> AXIOM_PLAYERS = Collections.newSetFromMap(new WeakHashMap<>());

	private static final ChannelRegistrationManager CHANNEL_REGISTRATION_MANAGER = new ChannelRegistrationManager();
	private static final AxiomMessageRegistry MESSAGE_REGISTRY = AxiomMessageRegistry.defaultRegistry(CHANNEL_REGISTRATION_MANAGER);

	private static AnnotationRegistry annotationRegistry;

	private AxiomMinestom() {
	}

	public static void initialize() {
		final var annotationPath = Paths.get("axiom", "annotations");
		final var annotationStorage = new JsonAnnotationStorage(annotationPath);
		final var annotationConfig = AnnotationConfig.defaultConfig();
		initialize(MinecraftServer.getGlobalEventHandler(), annotationStorage, annotationConfig);
	}

	public static void initialize(EventNode<@NotNull Event> eventNode,
	                              AnnotationStorage annotationStorage,
	                              AnnotationConfig annotationConfig) {

		LOGGER.info("Initializing Axiom for Minestom");

		CHANNEL_REGISTRATION_MANAGER.initialize(eventNode);
		MESSAGE_REGISTRY.initialize(eventNode);

		annotationRegistry = new AnnotationRegistry(annotationStorage, annotationConfig);

		MarkerManager.initialize(eventNode);
		MarkerManager.startMarkerUpdates();

		eventNode.addListener(PlayerDisconnectEvent.class, event -> unregisterAxiomPlayer(event.getPlayer()));

		LOGGER.info("Axiom integration is ready!");
	}

	public static AnnotationRegistry annotationRegistry() {
		return annotationRegistry;
	}

	public static AxiomMessageRegistry messageRegistry() {
		return MESSAGE_REGISTRY;
	}

	public static ChannelRegistrationManager channelRegistrationManager() {
		return CHANNEL_REGISTRATION_MANAGER;
	}

	public static boolean isAxiomPlayer(final Player player) {
		return AXIOM_PLAYERS.contains(player);
	}

	public static Set<Player> getAxiomPlayers() {
		return Collections.unmodifiableSet(AXIOM_PLAYERS);
	}

	public static void registerAxiomPlayer(Player player) {
		AXIOM_PLAYERS.add(player);
	}

	public static void unregisterAxiomPlayer(Player player) {
		AXIOM_PLAYERS.remove(player);
	}
}