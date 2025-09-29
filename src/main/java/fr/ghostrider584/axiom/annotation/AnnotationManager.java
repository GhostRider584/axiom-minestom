package fr.ghostrider584.axiom.annotation;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.annotation.data.AnnotationData;
import fr.ghostrider584.axiom.annotation.storage.AnnotationStorage;
import fr.ghostrider584.axiom.annotation.type.*;
import fr.ghostrider584.axiom.network.packet.server.AnnotationUpdateResponse;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class AnnotationManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationManager.class);

	private final UUID worldId;
	private final AnnotationStorage storage;
	private final AnnotationConfig config;
	private final Map<UUID, AnnotationData> annotations = new LinkedHashMap<>();
	private boolean dirty = false;

	private AnnotationManager(UUID worldId, AnnotationStorage storage, AnnotationConfig config) {
		this.worldId = worldId;
		this.storage = storage;
		this.config = config;
		loadAnnotations();
	}

	public static AnnotationManager create(Instance instance, AnnotationStorage storage, AnnotationConfig config) {
		return new AnnotationManager(instance.getUuid(), storage, config);
	}

	private void loadAnnotations() {
		if (storage != null) {
			annotations.putAll(storage.loadAnnotations(worldId));
			LOGGER.debug("Loaded {} annotations for world {}", annotations.size(), worldId);
		}
	}

	public void sendAllAnnotations(Player player) {
		if (!config.enabled() || !AxiomPermissions.hasPermission(player, AxiomPermission.ANNOTATION)) {
			return;
		}

		final var actions = new ArrayList<AnnotationUpdateAction>();
		actions.add(new ClearAllAnnotations());

		for (final var entry : annotations.entrySet()) {
			actions.add(new CreateAnnotation(entry.getKey(), entry.getValue()));
		}

		final var response = new AnnotationUpdateResponse(actions);
		AxiomMinestom.messageRegistry().send(player, response);

		LOGGER.trace("Sent {} annotations to player {} in world {}",
				annotations.size(), player.getUsername(), worldId);
	}

	public void handleUpdates(List<AnnotationUpdateAction> actions, Player requester, Instance instance) {
		if (!config.enabled()) {
			return;
		}

		boolean changed = false;
		for (final var action : actions) {
			if (processAction(action, requester)) {
				changed = true;
			}
		}

		if (changed) {
			markDirty();
			broadcastToPlayers(actions, instance);
			save();
		}
	}

	private boolean processAction(AnnotationUpdateAction action, Player player) {
		return switch (action) {
			case CreateAnnotation create -> handleCreate(create, player);
			case DeleteAnnotation delete -> handleDelete(delete, player);
			case MoveAnnotation move -> handleMove(move, player);
			case RotateAnnotation rotate -> handleRotate(rotate, player);
			case ClearAllAnnotations clear -> handleClearAll(clear, player);
			default -> throw new IllegalStateException("Unexpected value: " + action);
		};
	}

	private boolean handleCreate(CreateAnnotation create, Player player) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.ANNOTATION_CREATE)) {
			return false;
		}

		if (annotations.size() >= config.maxAnnotationsPerWorld()) {
			LOGGER.warn("Player {} tried to create annotation but world {} is at limit ({})",
					player.getUsername(), worldId, config.maxAnnotationsPerWorld());
			return false;
		}

		annotations.put(create.uuid(), create.annotationData());
		return true;
	}

	private boolean handleDelete(DeleteAnnotation delete, Player player) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.ANNOTATION_CREATE)) {
			return false;
		}

		return annotations.remove(delete.uuid()) != null;
	}

	private boolean handleMove(MoveAnnotation move, Player player) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.ANNOTATION_CREATE)) {
			return false;
		}

		final var annotation = annotations.get(move.uuid());
		if (annotation != null) {
			final var movedAnnotation = annotation.withPosition(move.to());
			annotations.put(move.uuid(), movedAnnotation);
			return true;
		}
		return false;
	}

	private boolean handleRotate(RotateAnnotation rotate, Player player) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.ANNOTATION_CREATE)) {
			return false;
		}

		final var annotation = annotations.get(rotate.uuid());
		if (annotation != null) {
			final var movedAnnotation = annotation.withRotation(rotate.to());
			annotations.put(rotate.uuid(), movedAnnotation);
			return true;
		}
		return false;
	}

	private boolean handleClearAll(ClearAllAnnotations clear, Player player) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.ANNOTATION_CLEARALL)) {
			return false;
		}

		if (!annotations.isEmpty()) {
			annotations.clear();
			return true;
		}
		return false;
	}

	private void broadcastToPlayers(List<AnnotationUpdateAction> actions, Instance instance) {
		final var axiomPlayers = instance.getPlayers().stream()
				.filter(player -> AxiomMinestom.isAxiomPlayer(player)
						&& AxiomPermissions.hasPermission(player, AxiomPermission.ANNOTATION))
				.collect(Collectors.toSet());

		if (!axiomPlayers.isEmpty()) {
			final var response = new AnnotationUpdateResponse(actions);
			AxiomMinestom.messageRegistry().sendGrouped(axiomPlayers, response);

			LOGGER.trace("Broadcasted {} annotation updates to {} players in world {}",
					actions.size(), axiomPlayers.size(), worldId);
		}
	}

	private void markDirty() {
		this.dirty = true;
	}

	public void save() {
		if (dirty && storage != null) {
			storage.saveAnnotations(worldId, new HashMap<>(annotations));
			dirty = false;
			LOGGER.trace("Saved {} annotations for world {}", annotations.size(), worldId);
		}
	}

	public void cleanup() {
		save();
		annotations.clear();
	}

	public int getAnnotationCount() {
		return annotations.size();
	}

	public UUID getWorldId() {
		return worldId;
	}
}