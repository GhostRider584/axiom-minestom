package fr.ghostrider584.axiom.annotation.storage.json;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import fr.ghostrider584.axiom.annotation.data.*;
import fr.ghostrider584.axiom.annotation.storage.AnnotationStorage;
import fr.ghostrider584.axiom.annotation.storage.json.serializer.*;
import fr.ghostrider584.axiom.math.Quaternionf;
import net.minestom.server.coordinate.Point;
import net.minestom.server.utils.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JsonAnnotationStorage implements AnnotationStorage {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonAnnotationStorage.class);

	private final Path dataDirectory;
	private final Gson gson;

	public JsonAnnotationStorage(Path dataDirectory) {
		this.dataDirectory = dataDirectory;
		this.gson = createGson();

		try {
			Files.createDirectories(dataDirectory);
		} catch (IOException e) {
			LOGGER.error("Failed to create annotation data directory", e);
		}
	}

	@Override
	public Map<UUID, AnnotationData> loadAnnotations(UUID worldId) {
		final var worldFile = dataDirectory.resolve(worldId + ".json");
		if (!Files.exists(worldFile)) {
			return new HashMap<>();
		}

		try {
			final var json = Files.readString(worldFile);
			final var type = new TypeToken<Map<UUID, AnnotationData>>() {
			}.getType();
			final var result = gson.<Map<UUID, AnnotationData>>fromJson(json, type);
			return result != null ? result : new HashMap<>();
		} catch (Exception e) {
			LOGGER.error("Failed to load annotations for world {}", worldId, e);
			return new HashMap<>();
		}
	}

	@Override
	public void saveAnnotations(UUID worldId, Map<UUID, AnnotationData> annotations) {
		final var worldFile = dataDirectory.resolve(worldId + ".json");
		try {
			final var json = gson.toJson(annotations);
			Files.writeString(worldFile, json);
		} catch (Exception e) {
			LOGGER.error("Failed to save annotations for world {}", worldId, e);
		}
	}

	@Override
	public void deleteWorld(UUID worldId) {
		final var worldFile = dataDirectory.resolve(worldId + ".json");
		try {
			Files.deleteIfExists(worldFile);
		} catch (IOException e) {
			LOGGER.error("Failed to delete annotations for world {}", worldId, e);
		}
	}

	private static Gson createGson() {
		return new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(AnnotationData.class, new AnnotationDataAdapter())
				.registerTypeAdapter(Quaternionf.class, new QuaternionfAdapter())
				.registerTypeAdapter(Point.class, new PointAdapter())
				.registerTypeAdapter(Direction.class, new DirectionAdapter())
				.create();
	}
}
