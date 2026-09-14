package fr.ghostrider584.axiom.world;

import fr.ghostrider584.axiom.block.*;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.*;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.*;
import java.util.*;
import static net.minestom.server.network.NetworkBuffer.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomBlockCatalogueTest {
    @BeforeAll static void start() { MinecraftServer.init(); }
    @AfterAll static void stop() { MinecraftServer.stopCleanly(); }
    @Test void encodesV2OptionalItemAndExplicitTransformMaps() {
        var item = ItemStack.of(Material.STICK);
        var definition = definition(item, List.of(Block.STONE.stateId()), List.of(), Map.of());
        byte[] data = NetworkBuffer.makeArray(definition::write); var buffer = NetworkBuffer.wrap(data, 0, data.length);
        assertEquals("test:lamp", buffer.read(STRING)); assertEquals("Lamp", buffer.read(STRING));
        assertEquals(0, buffer.read(VAR_INT)); assertEquals(List.of(Block.STONE.stateId()), buffer.read(VAR_INT.list()));
        assertTrue(buffer.read(BOOLEAN)); assertEquals(item, buffer.read(ItemStack.NETWORK_TYPE));
        assertTrue(buffer.read(BOOLEAN)); assertTrue(buffer.read(BOOLEAN)); assertTrue(buffer.read(BOOLEAN));
        assertEquals(0, buffer.read(VAR_INT)); assertFalse(buffer.read(BOOLEAN));
        for (int i = 0; i < 5; i++) assertEquals(0, buffer.read(VAR_INT));
        assertEquals(0, buffer.readableBytes());
    }
    @Test void rejectsWrongProductsCollidingCarriersAndInvalidTransforms() {
        int a = Block.STONE.stateId(), b = Block.DIRT.stateId();
        var property = new CustomBlockDefinition.Property("axis", List.of("x", "y"));
        assertThrows(IllegalArgumentException.class, () -> definition(null, List.of(a), List.of(property), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> definition(null, List.of(a, b), List.of(property), Map.of(a, b)));
        var valid = definition(null, List.of(a), List.of(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> new CustomBlockCatalogue(java.util.UUID.randomUUID(), List.of(valid, valid)));
    }
    private static CustomBlockDefinition definition(ItemStack item, List<Integer> states,
                                                    List<CustomBlockDefinition.Property> properties, Map<Integer, Integer> rotate) {
        return new CustomBlockDefinition(Key.key("test:lamp"), "Lamp", properties, states, item, Set.of(), rotate, Map.of(), Map.of(), Map.of());
    }
}
