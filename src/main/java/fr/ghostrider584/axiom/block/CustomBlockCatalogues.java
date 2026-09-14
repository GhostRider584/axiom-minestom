package fr.ghostrider584.axiom.block;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.world.AxiomEdits;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import java.util.*;
import java.util.function.Function;

public final class CustomBlockCatalogues {
    public static final String CLEAR_CHANNEL = "axiom:custom_blocks";
    public static final String REGISTER_CHANNEL = "axiom:register_custom_block_v2";
    private static volatile Function<Player, CustomBlockCatalogue> provider = _ -> CustomBlockCatalogue.EMPTY;
    private static final Map<Player, UUID> SENT = new WeakHashMap<>();
    private CustomBlockCatalogues() {}
    /** Return EMPTY until the corresponding resource pack is ready and the player may edit here. */
    public static void setProvider(Function<Player, CustomBlockCatalogue> value) { provider = Objects.requireNonNull(value); }
    public static synchronized void forget(Player player) { SENT.remove(player); }
    /** Call on handshake, world change and pack-ready events, from the host's owning context. */
    public static synchronized void refresh(Player player) {
        if (!AxiomMinestom.isAxiomPlayer(player)) return;
        var catalogue = Objects.requireNonNull(provider.apply(player));
        if (catalogue.generation().equals(SENT.get(player))) return;
        var channels = AxiomMinestom.channelRegistrationManager();
        if (!channels.isChannelSupported(player, CLEAR_CHANNEL) || !channels.isChannelSupported(player, REGISTER_CHANNEL)) {
            if (!catalogue.blocks().isEmpty()) throw new IllegalStateException("This Axiom client does not support custom block v2 registration");
            return;
        }
        AxiomEdits.invalidate(player);
        player.sendPluginMessage(CLEAR_CHANNEL, new byte[]{0});
        for (var definition : catalogue.blocks()) player.sendPluginMessage(REGISTER_CHANNEL,
                NetworkBuffer.makeArray(definition::write));
        SENT.put(player, catalogue.generation());
    }
}
