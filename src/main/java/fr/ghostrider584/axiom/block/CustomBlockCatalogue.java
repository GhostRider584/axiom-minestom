package fr.ghostrider584.axiom.block;

import java.util.*;

/** A generation is immutable; the host replaces it whenever its pack/allocation changes. */
public record CustomBlockCatalogue(UUID generation, List<CustomBlockDefinition> blocks) {
    public static final CustomBlockCatalogue EMPTY = new CustomBlockCatalogue(new UUID(0, 0), List.of());
    public CustomBlockCatalogue {
        Objects.requireNonNull(generation); blocks = List.copyOf(blocks);
        if (blocks.size() > 1024) throw new IllegalArgumentException("Custom block catalogue is too large");
        var identities = new HashSet<net.kyori.adventure.key.Key>();
        var carriers = new HashSet<Integer>();
        for (var block : blocks) {
            if (!identities.add(block.id())) throw new IllegalArgumentException("Duplicate custom block identity");
            for (int state : block.states()) if (!carriers.add(state)) throw new IllegalArgumentException("Custom blocks share a carrier state");
        }
    }
}
