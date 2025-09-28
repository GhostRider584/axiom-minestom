package fr.ghostrider584.axiom.annotation.storage;

import fr.ghostrider584.axiom.annotation.data.AnnotationData;

import java.util.Map;
import java.util.UUID;

public interface AnnotationStorage {
	Map<UUID, AnnotationData> loadAnnotations(UUID worldId);

	void saveAnnotations(UUID worldId, Map<UUID, AnnotationData> annotations);

	void deleteWorld(UUID worldId);

	default void close() {

	}
}