package fr.ghostrider584.axiom.network.channel;

import fr.ghostrider584.axiom.AxiomMinestom;
import fr.ghostrider584.axiom.network.packet.client.HelloMessage;
import fr.ghostrider584.axiom.network.packet.server.EnableMessage;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static net.minestom.server.network.NetworkBuffer.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AxiomHandshakeTest {
    @BeforeAll static void start() { MinecraftServer.init(); }
    @AfterAll static void stop() { MinecraftServer.stopCleanly(); }
    @Test void acceptsOnlyExactCurrentProtocolAndSingleUseNonce() {
        var player = mock(Player.class);
        when(player.getUuid()).thenReturn(java.util.UUID.randomUUID());
        when(player.getInstance()).thenReturn(mock(Instance.class));
        EventNode<Event> events = EventNode.all("handshake-test");
        var manager = AxiomMinestom.channelRegistrationManager(); manager.initialize(events);
        try {
            events.call(new PlayerPluginMessageEvent(player, "minecraft:register", ChannelListCodec.encode(List.of("axiom:hello"))));
            var bytes = ArgumentCaptor.forClass(byte[].class);
            verify(player).sendPluginMessage(eq("axiom:hello"), bytes.capture());
            byte[] data = bytes.getValue();
            long nonce = NetworkBuffer.wrap(data, 0, data.length).read(LONG);
            var reply = new HelloMessage(10, MinecraftServer.DATA_VERSION, MinecraftServer.PROTOCOL_VERSION, nonce);
            assertTrue(AxiomHandshake.accept(player, reply));
            assertFalse(AxiomHandshake.accept(player, reply));
            AxiomHandshake.begin(player);
            assertFalse(AxiomHandshake.accept(player, new HelloMessage(9, MinecraftServer.DATA_VERSION, MinecraftServer.PROTOCOL_VERSION, nonce)));
        } finally { AxiomHandshake.clear(player); manager.removePlayer(player); }
    }
    @Test void enableAndHelloUseTheApi10Layout() {
        var enabled = new EnableMessage(true, List.of("axiom:hello", "axiom:tunnel"));
        byte[] data = NetworkBuffer.makeArray(EnableMessage.TYPE, enabled);
        var buffer = NetworkBuffer.wrap(data, 0, data.length);
        assertTrue(buffer.read(BOOLEAN)); assertEquals(0, Byte.toUnsignedInt(buffer.read(BYTE))); assertEquals(2, buffer.read(VAR_INT));
        assertEquals(31_000, buffer.read(INT)); assertEquals(2 * 1024 * 1024, buffer.read(INT));
        assertEquals(0, buffer.read(VAR_INT)); assertEquals(0, buffer.read(VAR_INT));
        assertEquals(enabled.incomingChannels(), buffer.read(STRING.list())); assertEquals(0, buffer.readableBytes());
        byte[] disabled = NetworkBuffer.makeArray(EnableMessage.TYPE, new EnableMessage(false, List.of()));
        assertArrayEquals(new byte[]{0}, disabled);
        byte[] hello = NetworkBuffer.makeArray(b -> {
            b.write(VAR_INT, 10); b.write(VAR_INT, MinecraftServer.DATA_VERSION);
            b.write(VAR_INT, MinecraftServer.PROTOCOL_VERSION); b.write(LONG, 1234L);
        });
        assertEquals(1234L, NetworkBuffer.wrap(hello, 0, hello.length).read(HelloMessage.TYPE).handshakeId());
    }
}
