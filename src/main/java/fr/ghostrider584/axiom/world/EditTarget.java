package fr.ghostrider584.axiom.world;

import fr.ghostrider584.axiom.network.packet.client.SetBlockMessage;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;

/** Host seam for writable/owned worlds; no GameServer or Mortar dependency. */
@FunctionalInterface
public interface EditTarget {
    /**
     * Apply through the world's serialized owner in bounded slices. Recheck current before every
     * slice; complete only after writes/client corrections settle. An error may follow a partial
     * edit. Never resolve another target from the player's current instance after capture.
     */
    CompletionStage<Void> apply(Request request, EditBatch batch);

    record Request(Player player, Instance instance, BooleanSupplier current, boolean allowNbt,
                   SetBlockMessage singleBlockMessage) {
        public void requireCurrent() {
            if (!current.getAsBoolean()) throw new IllegalStateException("Editing session changed or permission was revoked");
        }
    }
}
