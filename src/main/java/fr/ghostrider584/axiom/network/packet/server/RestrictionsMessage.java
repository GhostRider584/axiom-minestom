package fr.ghostrider584.axiom.network.packet.server;

import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.server.ServerPacket;

import java.util.EnumSet;
import java.util.Set;

import static net.minestom.server.network.NetworkBuffer.*;

public record RestrictionsMessage(
		EnumSet<AxiomPermission> allowedPermissions,
		EnumSet<AxiomPermission> deniedPermissions,
		int infiniteReachLimit,
		Set<PlotBox> bounds
) implements ServerPacket.Play {

	public RestrictionsMessage {
		bounds = Set.copyOf(bounds);
	}

	public static final NetworkBuffer.Type<RestrictionsMessage> TYPE = NetworkBufferTemplate.template(
			AxiomPermission.PERMISSION_SET, RestrictionsMessage::allowedPermissions,
			AxiomPermission.PERMISSION_SET, RestrictionsMessage::deniedPermissions,
			INT, RestrictionsMessage::infiniteReachLimit,
			PlotBox.TYPE_SET, RestrictionsMessage::bounds,
			RestrictionsMessage::new
	);

	public RestrictionsMessage() {
		this(EnumSet.noneOf(AxiomPermission.class), EnumSet.allOf(AxiomPermission.class), -1, Set.of());
	}

	public record PlotBox(Point min, Point max) {
		public static final NetworkBuffer.Type<PlotBox> TYPE = NetworkBufferTemplate.template(
				BLOCK_POSITION, PlotBox::min,
				BLOCK_POSITION, PlotBox::max,
				PlotBox::new
		);

		public static final NetworkBuffer.Type<Set<PlotBox>> TYPE_SET = PlotBox.TYPE.set();
	}
}