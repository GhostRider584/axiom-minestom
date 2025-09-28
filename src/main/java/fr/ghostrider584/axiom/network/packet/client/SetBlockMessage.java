package fr.ghostrider584.axiom.network.packet.client;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static net.minestom.server.network.NetworkBuffer.*;

public record SetBlockMessage(
		Map<Point, Block> blocks,
		boolean updateNeighbors,
		Set<Point> preventUpdatesAt,
		int reason,
		boolean breaking,
		BlockHitResult blockHit,
		PlayerHand hand,
		int sequenceId
) implements ClientPacket {

	public static final NetworkBuffer.Type<SetBlockMessage> TYPE = new NetworkBuffer.Type<>() {
		private static final NetworkBuffer.Type<PlayerHand> PLAYER_HAND_TYPE = NetworkBuffer.Enum(PlayerHand.class);

		@Override
		public void write(@NotNull NetworkBuffer buffer, @NotNull SetBlockMessage value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public @NotNull SetBlockMessage read(@NotNull NetworkBuffer buffer) {
			final int blocksSize = buffer.read(VAR_INT);

			final var blocks = new LinkedHashMap<Point, Block>(blocksSize);
			for (int i = 0; i < blocksSize; i++) {
				final var pos = buffer.read(BLOCK_POSITION);
				final int stateId = buffer.read(VAR_INT);
				final var block = Block.fromStateId((short) stateId);
				blocks.put(pos, block);
			}

			final boolean updateNeighbors = buffer.read(BOOLEAN);

			Set<Point> preventUpdatesAt = Set.of();
			if (updateNeighbors) {
				preventUpdatesAt = buffer.read(BLOCK_POSITION.set());
			}

			final int reason = buffer.read(VAR_INT);
			final boolean breaking = buffer.read(BOOLEAN);
			final var blockHit = buffer.read(BlockHitResult.TYPE);
			final var hand = buffer.read(PLAYER_HAND_TYPE);
			int sequenceId = buffer.read(VAR_INT);

			return new SetBlockMessage(blocks, updateNeighbors, preventUpdatesAt, reason,
					breaking, blockHit, hand, sequenceId);
		}
	};

	public record BlockHitResult(
			Point blockPos,
			Direction direction,
			Point offset,
			boolean inside,
			boolean worldBorderHit
	) {
		public static final NetworkBuffer.Type<BlockHitResult> TYPE = NetworkBufferTemplate.template(
				BLOCK_POSITION, BlockHitResult::blockPos,
				DIRECTION, BlockHitResult::direction,
				VECTOR3, BlockHitResult::offset,
				BOOLEAN, BlockHitResult::inside,
				BOOLEAN, BlockHitResult::worldBorderHit,
				BlockHitResult::new
		);

	}
}