package fr.ghostrider584.axiom.marker;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.restrictions.AxiomPermissions;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarkerManagerTest {
    @BeforeAll static void start() { MinecraftServer.init(); }
    @AfterAll static void stop() { MinecraftServer.stopCleanly(); }

    @Test void onlyAuthorizedAxiomPlayersInTheOwningInstanceReceiveMarkers() {
        var manager = MinecraftServer.getInstanceManager();
        var a = manager.createInstanceContainer();
        var b = manager.createInstanceContainer();
        var owner = mock(Player.class);
        var other = mock(Player.class);
        var denied = mock(Player.class);
        var detached = mock(Player.class);
        when(owner.getInstance()).thenReturn(a);
        when(other.getInstance()).thenReturn(b);
        when(denied.getInstance()).thenReturn(a);
        var players = List.of(owner, other, denied, detached);
        players.forEach(AxiomMinestom::registerAxiomPlayer);
        AxiomPermissions.setPermissionPredicate((player, permission) -> player != denied);
        try {
            assertEquals(List.of(owner), MarkerManager.recipients(a));
            assertEquals(List.of(other), MarkerManager.recipients(b));
        } finally {
            players.forEach(AxiomMinestom::unregisterAxiomPlayer);
            AxiomPermissions.setPermissionPredicate((player, permission) -> true);
            manager.unregisterInstance(a);
            manager.unregisterInstance(b);
        }
    }
}
