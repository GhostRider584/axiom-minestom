package fr.ghostrider584.axiom.listener;

import fr.ghostrider584.axiom.world.PaletteProcessor;
import fr.ghostrider584.axiom.dispatch.DispatchSendsManager;
import fr.ghostrider584.axiom.network.channel.IncomingMessageHandler;
import fr.ghostrider584.axiom.network.packet.client.SetBufferMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SetBufferHandler implements IncomingMessageHandler<SetBufferMessage> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SetBufferHandler.class);
	private static final Map<Player, Semaphore> PLAYER_LOCKS = Collections.synchronizedMap(new WeakHashMap<>());

	private static final int BLOCK_BUFFER_TYPE = 0;
	private static final int BIOME_BUFFER_TYPE = 1;

	@Override
	public void handle(Player player, String channel, SetBufferMessage packet) {
		if (!AxiomPermissions.hasPermission(player, AxiomPermission.BUILD_SECTION)) {
			LOGGER.warn("Player {} does not have permission for BUILD_SECTION", player.getUsername());
			return;
		}

		processBufferAsync(player, packet).exceptionally(throwable -> {
			LOGGER.error("Error processing buffer from player {}", player.getUsername(), throwable);
			return null;
		});
	}

	private CompletableFuture<Void> processBufferAsync(Player player, SetBufferMessage packet) {
		final var playerLock = PLAYER_LOCKS.computeIfAbsent(player, p -> new Semaphore(1));
		try {
			if (!playerLock.tryAcquire(30, TimeUnit.SECONDS)) {
				return CompletableFuture.failedFuture(new IllegalStateException("Failed to acquire lock"));
			}
		} catch (InterruptedException e) {
			return CompletableFuture.failedFuture(e);
		}

		return CompletableFuture.runAsync(() -> {
			try {
				final var instance = player.getInstance();
				final long startTime = System.currentTimeMillis();
				LOGGER.trace("Processing buffer from player {}, type={}, buffer size={}", player.getUsername(), packet.bufferType(), packet.bufferData().readableBytes());

				switch (packet.bufferType()) {
					case BLOCK_BUFFER_TYPE -> {
						final boolean allowNbt = AxiomPermissions.hasPermission(player, AxiomPermission.BUILD_NBT);
						PaletteProcessor.processChunkData(packet.bufferData(), allowNbt, instance);
						DispatchSendsManager.sendUpdateAvailableDispatchSends(player, 8192, 8192);
					}
					case BIOME_BUFFER_TYPE -> {
						// TODO: implement biome buffer processing
						player.sendMessage(Component.text("Biome editing is not yet supported", NamedTextColor.YELLOW));
					}
					default -> {
						LOGGER.error("Unknown buffer type {} from player {}", packet.bufferType(), player.getUsername());
						return;
					}
				}

				final long processingTime = System.currentTimeMillis() - startTime;
				LOGGER.trace("Buffer processing completed for player {} in {}ms", player.getUsername(), processingTime);
			} finally {
				playerLock.release();
			}
		});
	}
}