package fr.ghostrider584.axiom.network.channel;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.network.packet.client.HelloMessage;
import fr.ghostrider584.axiom.restrictions.AxiomPermission;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import fr.ghostrider584.axiom.world.AxiomEdits;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class AxiomHandshake {
    public static final int API_VERSION = 10;
    private static final Map<Player, Long> PENDING = new WeakHashMap<>();
    private AxiomHandshake() {}

    public static synchronized void begin(Player player) {
        if (player.getInstance() == null || AxiomMinestom.isAxiomPlayer(player) || PENDING.containsKey(player)
                || !AxiomMinestom.channelRegistrationManager().isChannelSupported(player, "axiom:hello")
                || !AxiomPermissions.hasPermission(player, AxiomPermission.USE)
                || !AxiomEdits.allowsChannel(player, "axiom:hello")) return;
        long nonce = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        PENDING.put(player, nonce);
        player.sendPluginMessage("axiom:hello", NetworkBuffer.makeArray(NetworkBuffer.LONG, nonce));
    }
    public static synchronized boolean accept(Player player, HelloMessage hello) {
        Long nonce = PENDING.remove(player);
        return nonce != null && nonce == hello.handshakeId() && hello.apiVersion() == API_VERSION
                && hello.protocolVersion() == MinecraftServer.PROTOCOL_VERSION
                && hello.dataVersion() == MinecraftServer.DATA_VERSION
                && AxiomPermissions.hasPermission(player, AxiomPermission.USE);
    }
    public static synchronized void clear(Player player) { PENDING.remove(player); }
}
