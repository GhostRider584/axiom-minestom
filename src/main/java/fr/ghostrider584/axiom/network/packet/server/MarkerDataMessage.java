package fr.ghostrider584.axiom.network.packet.server;

import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static net.minestom.server.network.NetworkBuffer.*;

public record MarkerDataMessage(
		List<MarkerData> changedMarkers,
		Set<UUID> removedMarkers
) implements ServerPacket.Play {

	public MarkerDataMessage {
		changedMarkers = List.copyOf(changedMarkers);
		removedMarkers = Set.copyOf(removedMarkers);
	}

	public static final NetworkBuffer.Type<MarkerDataMessage> TYPE = NetworkBufferTemplate.template(
			MarkerData.TYPE.list(1024), MarkerDataMessage::changedMarkers,
			UUID.set(1024), MarkerDataMessage::removedMarkers,
			MarkerDataMessage::new
	);

	public record Region(
			Point min,
			Point max,
			int lineArgb,
			float lineThickness,
			int faceArgb
	) {
		public Region {
			Objects.requireNonNull(min, "Region min cannot be null");
			Objects.requireNonNull(max, "Region max cannot be null");
		}
	}

	public record MarkerData(
			UUID uuid,
			Point position,
			@Nullable String name,
			@Nullable Region region
	) {
		private static final byte FLAG_HAS_REGION = 1;
		private static final byte FLAG_HAS_LINE_COLOR = 2;
		private static final byte FLAG_HAS_LINE_THICKNESS = 4;
		private static final byte FLAG_HAS_FACE_COLOR = 8;

		public static final NetworkBuffer.Type<MarkerData> TYPE = new NetworkBuffer.Type<>() {
			@Override
			public void write(@NotNull NetworkBuffer buffer, @NotNull MarkerData value) {
				buffer.write(UUID, value.uuid());
				buffer.write(VECTOR3D, value.position());
				buffer.write(STRING.optional(), value.name());

				final byte flags = calculateFlags(value);
				buffer.write(BYTE, flags);

				if (value.region() != null && (flags & FLAG_HAS_REGION) != 0) {
					final var region = value.region();
					buffer.write(VECTOR3D, region.min());
					buffer.write(VECTOR3D, region.max());

					if ((flags & FLAG_HAS_LINE_COLOR) != 0) {
						buffer.write(INT, region.lineArgb());
					}
					if ((flags & FLAG_HAS_LINE_THICKNESS) != 0) {
						buffer.write(FLOAT, region.lineThickness());
					}
					if ((flags & FLAG_HAS_FACE_COLOR) != 0) {
						buffer.write(INT, region.faceArgb());
					}
				}
			}

			@Override
			public @NotNull MarkerData read(@NotNull NetworkBuffer buffer) {
				final var uuid = buffer.read(UUID);
				final var position = buffer.read(VECTOR3D);
				final var name = buffer.read(STRING.optional());
				final byte flags = buffer.read(BYTE);

				Region region = null;
				if ((flags & FLAG_HAS_REGION) != 0) {
					final var minRegion = buffer.read(VECTOR3D);
					final var maxRegion = buffer.read(VECTOR3D);

					int lineArgb = 0;
					if ((flags & FLAG_HAS_LINE_COLOR) != 0) {
						lineArgb = buffer.read(INT);
					}

					float lineThickness = 0.0f;
					if ((flags & FLAG_HAS_LINE_THICKNESS) != 0) {
						lineThickness = buffer.read(FLOAT);
					}

					int faceArgb = 0;
					if ((flags & FLAG_HAS_FACE_COLOR) != 0) {
						faceArgb = buffer.read(INT);
					}

					region = new Region(minRegion, maxRegion, lineArgb, lineThickness, faceArgb);
				}

				return new MarkerData(uuid, position, name, region);
			}

			private byte calculateFlags(MarkerData marker) {
				byte flags = 0;

				if (marker.region() != null) {
					final var region = marker.region();
					flags |= FLAG_HAS_REGION;

					if (region.lineArgb() != 0) {
						flags |= FLAG_HAS_LINE_COLOR;
					}
					if (region.lineThickness() != 0.0f) {
						flags |= FLAG_HAS_LINE_THICKNESS;
					}
					if (region.faceArgb() != 0) {
						flags |= FLAG_HAS_FACE_COLOR;
					}
				}

				return flags;
			}
		};
	}
}