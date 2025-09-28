package fr.ghostrider584.axiom.annotation;

import fr.ghostrider584.axiom.annotation.storage.AnnotationStorage;
import net.minestom.server.instance.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AnnotationRegistry {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationRegistry.class);

	private final Map<UUID, AnnotationManager> managers = new ConcurrentHashMap<>();
	private final AnnotationStorage storage;
	private final AnnotationConfig defaultConfig;

	public AnnotationRegistry(AnnotationStorage storage, AnnotationConfig defaultConfig) {
		this.storage = storage;
		this.defaultConfig = defaultConfig;

		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
	}

	public AnnotationManager getOrCreate(Instance instance) {
		return getOrCreate(instance, defaultConfig);
	}

	public AnnotationManager getOrCreate(Instance instance, AnnotationConfig config) {
		return managers.computeIfAbsent(instance.getUuid(),
				uuid -> AnnotationManager.create(instance, storage, config));
	}

	public AnnotationManager get(Instance instance) {
		return managers.get(instance.getUuid());
	}

	public void remove(Instance instance) {
		final var manager = managers.remove(instance.getUuid());
		if (manager != null) {
			manager.cleanup();
			LOGGER.debug("Removed annotation manager for world {}", instance.getUuid());
		}
	}

	public void saveAll() {
		managers.values().forEach(AnnotationManager::save);
		LOGGER.debug("Saved all annotation managers");
	}

	public void shutdown() {
		managers.values().forEach(AnnotationManager::cleanup);
		if (storage != null) {
			storage.close();
		}
		managers.clear();
		LOGGER.info("Annotation registry shutdown complete");
	}

	public Map<UUID, Integer> getAnnotationCounts() {
		return managers.entrySet().stream().collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().getAnnotationCount()
		));
	}
}