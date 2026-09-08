package fr.ghostrider584.axiom.listener;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.world.DimensionType;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeleportHandlerTest {
    private final List<InstanceContainer> instances = new ArrayList<>();
    @BeforeAll static void start() { MinecraftServer.init(); }
    @AfterAll static void stop() { MinecraftServer.stopCleanly(); }
    @AfterEach void cleanup() { instances.forEach(MinecraftServer.getInstanceManager()::unregisterInstance); }

    @Test void sameDimensionKeepsTheCurrentInstanceEvenWhenRegisteredLater() {
        create(false);
        var current = create(false);
        assertSame(current, TeleportHandler.resolveInstance(current, Key.key("minecraft:overworld")));
    }

    @Test void rejectsAmbiguousOtherDimensionButAcceptsUniqueTarget() {
        var current = create(false);
        var target = create(true);
        assertSame(target, TeleportHandler.resolveInstance(current, Key.key("minecraft:the_nether")));
        create(true);
        assertNull(TeleportHandler.resolveInstance(current, Key.key("minecraft:the_nether")));
        assertNull(TeleportHandler.resolveInstance(current, Key.key("test:unknown")));
    }

    private InstanceContainer create(boolean nether) {
        var instance = MinecraftServer.getInstanceManager().createInstanceContainer(nether ? DimensionType.THE_NETHER : DimensionType.OVERWORLD);
        instances.add(instance);
        return instance;
    }
}
