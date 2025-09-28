package fr.ghostrider584.axiom.marker;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.network.packet.server.MarkerDataMessage;
import fr.ghostrider584.axiom.network.packet.server.MarkerDataMessage.MarkerData;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import net.kyori.adventure.nbt.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// TODO: refactor and improve
public class MarkerManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(MarkerManager.class);
	private static final Map<UUID, Map<UUID, Marker>> INSTANCE_MARKERS = new ConcurrentHashMap<>();

	private static Task markerUpdateTask;

	public static void initialize(EventNode<@NotNull Event> eventNode) {
		eventNode.addListener(AddEntityToInstanceEvent.class, event -> {
			if (event.getEntity() instanceof Player player) {
				onPlayerJoinInstance(player, event.getInstance());
			}
		});
	}

	public static void startMarkerUpdates() {
		if (markerUpdateTask != null) {
			markerUpdateTask.cancel();
		}

		markerUpdateTask = MinecraftServer.getSchedulerManager().scheduleTask(() -> {
			MinecraftServer.getInstanceManager().getInstances().forEach(MarkerManager::updateMarkersForInstance);
		}, TaskSchedule.immediate(), TaskSchedule.tick(1));

		LOGGER.trace("Started marker update task");
	}

	public static void stopMarkerUpdates() {
		if (markerUpdateTask != null) {
			markerUpdateTask.cancel();
			markerUpdateTask = null;
		}
		LOGGER.trace("Stopped marker update task");
	}

	public static void onPlayerJoinInstance(Player player, Instance instance) {
		if (!hasAxiomPermission(player)) return;

		final var instanceMarkers = INSTANCE_MARKERS.get(instance.getUuid());
		if (instanceMarkers == null || instanceMarkers.isEmpty()) return;

		final var markerData = instanceMarkers.values().stream()
				.map(Marker::toNetworkData)
				.toList();

		final var message = new MarkerDataMessage(markerData, Set.of());
		AxiomMinestom.messageRegistry().send(player, message);

		LOGGER.trace("Sent {} existing markers to player {} joining instance {}",
				markerData.size(), player.getUsername(), instance.getUuid());
	}

	private static void updateMarkersForInstance(Instance instance) {
		final var instanceMarkers = INSTANCE_MARKERS.computeIfAbsent(instance.getUuid(), k -> new ConcurrentHashMap<>());

		final var changedMarkers = new ArrayList<MarkerData>();
		final var currentMarkerIds = new HashSet<UUID>();

		for (final var entity : instance.getEntities()) {
			if (entity.getEntityType() != EntityType.MARKER || isMarkerHidden(entity)) {
				continue;
			}

			final var currentMarker = Marker.fromEntity(entity);
			final var previousMarker = instanceMarkers.get(entity.getUuid());

			if (!Objects.equals(currentMarker, previousMarker)) {
				instanceMarkers.put(entity.getUuid(), currentMarker);
				changedMarkers.add(currentMarker.toNetworkData());
			}

			currentMarkerIds.add(entity.getUuid());
		}

		final var removedMarkerIds = new HashSet<>(instanceMarkers.keySet());
		removedMarkerIds.removeAll(currentMarkerIds);
		instanceMarkers.keySet().removeAll(removedMarkerIds);

		if (!changedMarkers.isEmpty() || !removedMarkerIds.isEmpty()) {
			final var message = new MarkerDataMessage(changedMarkers, removedMarkerIds);
			final var axiomPlayers = AxiomMinestom.getAxiomPlayers();

			AxiomMinestom.messageRegistry().sendGrouped(axiomPlayers, message);

			if (!axiomPlayers.isEmpty()) {
				LOGGER.trace("Sent marker updates to {} players in instance {}: {} changed, {} removed",
						axiomPlayers.size(), instance.getUuid(), changedMarkers.size(), removedMarkerIds.size());
			}
		}
	}

	public static boolean updateMarkerImmediately(Entity entity) {
		if (entity.getEntityType() != EntityType.MARKER || entity.getInstance() == null) {
			return false;
		}

		final var instance = entity.getInstance();
		final var instanceMarkers = INSTANCE_MARKERS.computeIfAbsent(instance.getUuid(), k -> new ConcurrentHashMap<>());

		if (isMarkerHidden(entity)) {
			return removeMarkerImmediately(entity);
		}

		final var currentMarker = Marker.fromEntity(entity);
		final var previousMarker = instanceMarkers.get(entity.getUuid());

		if (!Objects.equals(currentMarker, previousMarker)) {
			instanceMarkers.put(entity.getUuid(), currentMarker);

			final var message = new MarkerDataMessage(List.of(currentMarker.toNetworkData()), Set.of());
			final var axiomPlayers = AxiomMinestom.getAxiomPlayers();

			axiomPlayers.forEach(player -> AxiomMinestom.messageRegistry().send(player, message));

			if (!axiomPlayers.isEmpty()) {
				LOGGER.trace("Immediately updated marker {} in instance {} for {} players",
						entity.getUuid(), instance.getUuid(), axiomPlayers.size());
			}
			return true;
		}
		return false;
	}

	public static boolean removeMarkerImmediately(Entity entity) {
		if (entity.getInstance() == null) return false;

		final var instance = entity.getInstance();
		final var instanceMarkers = INSTANCE_MARKERS.get(instance.getUuid());

		if (instanceMarkers != null && instanceMarkers.remove(entity.getUuid()) != null) {
			final var message = new MarkerDataMessage(List.of(), Set.of(entity.getUuid()));
			final var axiomPlayers = AxiomMinestom.getAxiomPlayers();

			axiomPlayers.forEach(player -> AxiomMinestom.messageRegistry().send(player, message));

			if (!axiomPlayers.isEmpty()) {
				LOGGER.trace("Immediately removed marker {} from instance {} for {} players",
						entity.getUuid(), instance.getUuid(), axiomPlayers.size());
			}
			return true;
		}
		return false;
	}

	public static void refreshInstanceMarkersImmediately(Instance instance) {
		updateMarkersForInstance(instance);
	}

	private static boolean hasAxiomPermission(Player player) {
		return AxiomPermissions.hasPermission(player, AxiomPermission.USE);
	}

	private static boolean isMarkerHidden(Entity entity) {
		return entity.isInvisible();
	}

	public record Marker(
			UUID uuid,
			Vec position,
			@Nullable String name,
			@Nullable Region region
	) {
		public static Marker fromEntity(Entity entity) {
			final var name = entity.getTag(MarkerTags.NAME);
			final var nbtData = entity.tagHandler().asCompound();
			final var position = entity.getPosition().asVec();
			final var region = parseRegion(entity.getPosition().asVec(), nbtData);

			return new Marker(entity.getUuid(), position, name, region);
		}

		public MarkerData toNetworkData() {
			final var networkRegion = region != null
					? new MarkerDataMessage.Region(region.min(), region.max(), region.lineArgb(), region.lineThickness(), region.faceArgb())
					: null;
			return new MarkerData(uuid, position, name, networkRegion);
		}

		private static @Nullable Region parseRegion(Vec entityPosition, CompoundBinaryTag data) {
			final var minTag = data.get("min");
			final var maxTag = data.get("max");

			if (!(minTag instanceof ListBinaryTag minList) || !(maxTag instanceof ListBinaryTag maxList)) {
				return null;
			}

			if (minList.size() != 3 || maxList.size() != 3) {
				return null;
			}

			try {
				final var minVec = parseVec(minList, entityPosition);
				final var maxVec = parseVec(maxList, entityPosition);

				if (minVec == null || maxVec == null) {
					return null;
				}

				final int lineArgb = data.getInt("line_argb", 0);
				final float lineThickness = data.getFloat("line_thickness", 0.0f);
				final int faceArgb = data.getInt("face_argb", 0);

				return new Region(minVec, maxVec, lineArgb, lineThickness, faceArgb);
			} catch (Exception e) {
				LOGGER.error("Failed to parse marker region: {}", e.getMessage());
				return null;
			}
		}

		private static @Nullable Vec parseVec(ListBinaryTag list, Vec entityPosition) {
			try {
				final double x = parseCoordinate(list.get(0), entityPosition.x());
				final double y = parseCoordinate(list.get(1), entityPosition.y());
				final double z = parseCoordinate(list.get(2), entityPosition.z());

				if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
					return new Vec(x, y, z);
				}
			} catch (Exception e) {
				LOGGER.error("Failed to parse vector coordinates: {}", e.getMessage());
			}
			return null;
		}

		private static double parseCoordinate(BinaryTag tag, double currentPosition) {
			return switch (tag) {
				case DoubleBinaryTag doubleTag -> doubleTag.doubleValue();
				case StringBinaryTag stringTag -> calculateRelativeCoordinate(stringTag.value(), currentPosition);
				default -> {
					LOGGER.warn("Unsupported coordinate tag type: {}", tag.getClass().getSimpleName());
					yield Double.NaN;
				}
			};
		}

		private static double calculateRelativeCoordinate(String coordinate, double position) {
			if (coordinate == null || coordinate.trim().isEmpty()) {
				return Double.NaN;
			}

			coordinate = coordinate.trim();
			final boolean relative = coordinate.startsWith("~");

			if (relative) {
				coordinate = coordinate.substring(1).trim();
			}

			try {
				final double value = coordinate.isEmpty() ? 0.0 : Double.parseDouble(coordinate);
				return relative ? position + value : value;
			} catch (NumberFormatException e) {
				LOGGER.error("Failed to parse coordinate '{}': {}", coordinate, e.getMessage());
				return Double.NaN;
			}
		}

		public record Region(
				Vec min,
				Vec max,
				int lineArgb,
				float lineThickness,
				int faceArgb
		) {
		}
	}
}