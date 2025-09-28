package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.SetBlockMessage;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;

public class SetBlockHandler implements IncomingMessageHandler<SetBlockMessage> {

	@Override
	public void handle(Player player, String channel, SetBlockMessage packet) {
		if (!AxiomMinestom.isAxiomPlayer(player)) {
			return;
		}

		final var instance = player.getInstance();
		if (instance == null) {
			return;
		}

		if (packet.breaking()) {
			handleBlockBreaking(player, instance, packet);
		} else {
			handleBlockPlacing(player, instance, packet);
		}
	}

	private void handleBlockBreaking(Player player, Instance instance, SetBlockMessage packet) {
		for (final var entry : packet.blocks().entrySet()) {
			final var position = entry.getKey();
			final var currentBlock = instance.getBlock(position);

			final var blockFace = BlockFace.fromDirection(packet.blockHit().direction());
			final var breakEvent = new PlayerBlockBreakEvent(player, currentBlock, currentBlock, new BlockVec(position), blockFace);
			EventDispatcher.callCancellable(breakEvent, () -> {
				instance.setBlock(position, Block.AIR, packet.updateNeighbors());
			});
		}
	}

	private void handleBlockPlacing(Player player, Instance instance, SetBlockMessage packet) {
		for (final var entry : packet.blocks().entrySet()) {
			final var position = entry.getKey();
			final var newBlock = entry.getValue();

			final var blockFace = BlockFace.fromDirection(packet.blockHit().direction());
			final var placeEvent = new PlayerBlockPlaceEvent(player, newBlock, blockFace, new BlockVec(position), Vec.ZERO, packet.hand());
			EventDispatcher.callCancellable(placeEvent, () -> {
				final var preventUpdates = !packet.updateNeighbors() || packet.preventUpdatesAt().contains(position);
				instance.setBlock(position, newBlock, preventUpdates);
			});
		}
	}
}