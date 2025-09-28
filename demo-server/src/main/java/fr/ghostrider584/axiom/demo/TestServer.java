package fr.ghostrider584.axiom.demo;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.marker.MarkerTags;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.metadata.animal.PigVariant;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.world.DimensionType;
import org.slf4j.LoggerFactory;

public class TestServer {

	private static final RegistryKey<DimensionType> FULLBRIGHT_DIMENSION = RegistryKey.unsafeOf(Key.key("custom:fullbright_dimension"));

	public static void main(String[] args) {
		System.setProperty("minestom.chunk-view-distance", "32");

		final var logger = LoggerFactory.getLogger(TestServer.class);
		logger.info("Starting server...");

		final var server = MinecraftServer.init();
		MinecraftServer.getDimensionTypeRegistry().register(FULLBRIGHT_DIMENSION.key(), DimensionType.builder().ambientLight(1.0F).build());

		final var instance = MinecraftServer.getInstanceManager().createInstanceContainer(FULLBRIGHT_DIMENSION);
		instance.setChunkSupplier(DynamicChunk::new);
		instance.setGenerator(unit -> {
			unit.modifier().fillHeight(4, 5, Block.GRASS_BLOCK);
			unit.modifier().fillHeight(1, 4, Block.DIRT);
			unit.modifier().fillHeight(0, 1, Block.BEDROCK);
		});

		final var moveWithMarkerTag = Tag.Boolean("move_with_marker").defaultValue(false);

		final var testPig = new Entity(EntityType.PIG);
		testPig.set(DataComponents.PIG_VARIANT, PigVariant.COLD);
		testPig.setInstance(instance, new Pos(10, 5, 0));
		//testPig.setTag(moveWithMarkerTag, true);
		testPig.setNoGravity(true);

		final var testVillager = new Entity(EntityType.VILLAGER);
		testVillager.setInstance(instance, new Pos(0, 5, 0));
		//testVillager.setTag(moveWithMarkerTag, true);
		testVillager.setNoGravity(true);

		final var testMarker = new Entity(EntityType.MARKER);
		testMarker.setInstance(instance, new Pos(5, 5, 0));
		testMarker.setNoGravity(true);

		testMarker.setTag(MarkerTags.NAME, "Example Region Marker");
		testMarker.setTag(MarkerTags.MIN, MarkerTags.stringVecToList("~2.0", "~2.0", "~2.0"));
		testMarker.setTag(MarkerTags.MAX, MarkerTags.stringVecToList("~-2.0", "~-2.0", "~-2.0"));
		testMarker.setTag(MarkerTags.LINE_ARGB, MarkerTags.argbToInt(255, 255, 255, 255));
		testMarker.setTag(MarkerTags.FACE_ARGB, MarkerTags.argbToInt(50, 255, 255, 255));
		testMarker.setTag(MarkerTags.LINE_THICKNESS, 2.5f);

		final var eventNode = MinecraftServer.getGlobalEventHandler();
		eventNode.addListener(AsyncPlayerConfigurationEvent.class, event -> {
			event.setSpawningInstance(instance);
		});

		eventNode.addListener(PlayerSpawnEvent.class, event -> {
			final var player = event.getPlayer();
			player.teleport(new Pos(0, 5, 0));
			player.setGameMode(GameMode.CREATIVE);
			player.setPermissionLevel(4);
		});

		eventNode.addListener(EntitySpawnEvent.class, event -> {
			final var entity = event.getEntity();
			if (entity.getEntityType() == EntityType.PLAYER) {
				return;
			}

			if (!entity.getTag(moveWithMarkerTag)) {
				return;
			}

			final var marker = new Entity(EntityType.MARKER);
			marker.setInstance(entity.getInstance(), entity.getPosition());
			marker.setNoGravity(true);

			final var boundingBox = entity.getBoundingBox();
			final var halfWidth = boundingBox.width() / 2d;
			final var halfHeight = boundingBox.height() / 2d;
			final var halfDepth = boundingBox.depth() / 2d;

			marker.setTag(MarkerTags.NAME, "§f" + entity.getEntityType().key().value());
			marker.setTag(MarkerTags.MIN, MarkerTags.stringVecToList("~" + halfWidth, "~" + halfHeight, "~" + halfDepth));
			marker.setTag(MarkerTags.MAX, MarkerTags.stringVecToList("~-" + halfWidth, "~-" + halfHeight, "~-" + halfDepth));
			marker.setTag(MarkerTags.LINE_ARGB, MarkerTags.argbToInt(255, 255, 255, 255));
			marker.setTag(MarkerTags.FACE_ARGB, MarkerTags.argbToInt(30, 255, 255, 255));
			marker.setTag(MarkerTags.LINE_THICKNESS, 2.5f);

			entity.scheduler().scheduleTask(() -> {
				entity.teleport(marker.getPosition().sub(0, halfHeight, 0));
			}, TaskSchedule.immediate(), TaskSchedule.tick(1));
		});

		AxiomMinestom.initialize();

		server.start("0.0.0.0", 25565);
		logger.info("Server started");
	}
}
