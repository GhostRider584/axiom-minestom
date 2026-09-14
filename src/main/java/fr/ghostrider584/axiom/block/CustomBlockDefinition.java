package fr.ghostrider584.axiom.block;

import net.kyori.adventure.key.Key;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.NetworkBuffer;
import fr.ghostrider584.axiom.world.EditBufferDecoder;
import java.util.*;
import static net.minestom.server.network.NetworkBuffer.*;

/** Tool-neutral values, exact 26.2 carriers and explicit transforms for Axiom's v2 block catalogue. */
public record CustomBlockDefinition(Key id, String translationKey, List<Property> properties, List<Integer> states,
                                    ItemStack pickItem, Set<Placement> placement, Map<Integer, Integer> rotateY,
                                    Map<Integer, Integer> flipX, Map<Integer, Integer> flipY, Map<Integer, Integer> flipZ) {
    /** State list uses the Cartesian product in this property/value order (last property varies fastest). */
    public record Property(String name, List<String> values) {
        public Property {
            values = List.copyOf(values);
            if (!name.matches("[a-z0-9_]+") || values.isEmpty() || values.size() > 256 || new HashSet<>(values).size() != values.size()
                    || values.stream().anyMatch(value -> !value.matches("[a-z0-9_.-]+")))
                throw new IllegalArgumentException("Invalid finite custom property");
        }
    }
    public enum Placement {
        AXIS("axis"), FACING("facing"), FACING_OPPOSITE("facing_opposite"), FACING_CLICKED("facing_clicked"),
        FACING_CLICKED_OPPOSITE("facing_clicked_opposite"), WATERLOGGED("waterlogged"), HALF("half");
        final String wire;
        Placement(String name) { wire = "axiom:" + name; }
    }
    public CustomBlockDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(translationKey);
        properties = List.copyOf(properties); states = List.copyOf(states); placement = Set.copyOf(placement);
        rotateY = Map.copyOf(rotateY); flipX = Map.copyOf(flipX); flipY = Map.copyOf(flipY); flipZ = Map.copyOf(flipZ);
        int count = 1;
        var names = new HashSet<String>();
        for (Property property : properties) {
            if (!names.add(property.name())) throw new IllegalArgumentException("Duplicate custom property");
            count = Math.multiplyExact(count, property.values().size());
        }
        if (count != states.size() || count > 4096 || new HashSet<>(states).size() != count)
            throw new IllegalArgumentException("Custom state count must match its finite property product with unique carriers");
        states.forEach(EditBufferDecoder::state);
        validateTransform(rotateY, states, 4); validateTransform(flipX, states, 2);
        validateTransform(flipY, states, 2); validateTransform(flipZ, states, 2);
    }
    private static void validateTransform(Map<Integer, Integer> transform, List<Integer> states, int order) {
        var domain = new HashSet<>(states);
        if (!domain.containsAll(transform.keySet()) || !domain.containsAll(transform.values()))
            throw new IllegalArgumentException("Transform leaves this custom block's states");
        for (int state : states) {
            int value = state;
            for (int i = 0; i < order; i++) value = transform.getOrDefault(value, value);
            if (value != state) throw new IllegalArgumentException("Transform does not have the advertised inverse/cycle");
        }
    }
    public void write(NetworkBuffer buffer) {
        buffer.write(STRING, id.asString()); buffer.write(STRING, translationKey);
        buffer.write(VAR_INT, properties.size());
        for (Property property : properties) {
            buffer.write(BYTE, (byte) 2); buffer.write(STRING, property.name());
            buffer.write(STRING.list(), property.values());
        }
        buffer.write(VAR_INT.list(), states);
        buffer.write(BOOLEAN, pickItem != null);
        if (pickItem != null) buffer.write(ItemStack.NETWORK_TYPE, pickItem);
        buffer.write(BOOLEAN, true); // server pick-block where supported
        buffer.write(BOOLEAN, true); // no vanilla carrier interaction
        buffer.write(BOOLEAN, true); // no vanilla carrier shape updates
        var orderedPlacement = placement.stream().sorted().toList();
        buffer.write(VAR_INT, orderedPlacement.size());
        for (Placement logic : orderedPlacement) buffer.write(STRING, logic.wire);
        buffer.write(BOOLEAN, false); // never rotate the borrowed carrier's vanilla properties
        writeMap(buffer, rotateY); writeMap(buffer, flipX); writeMap(buffer, flipY); writeMap(buffer, flipZ);
        buffer.write(VAR_INT, 0);
    }
    private static void writeMap(NetworkBuffer buffer, Map<Integer, Integer> map) {
        buffer.write(VAR_INT, map.size());
        map.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            buffer.write(VAR_INT, entry.getKey()); buffer.write(VAR_INT, entry.getValue());
        });
    }
}
