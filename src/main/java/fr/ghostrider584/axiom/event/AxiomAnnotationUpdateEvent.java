package fr.ghostrider584.axiom.event;

import fr.ghostrider584.axiom.annotation.type.AnnotationUpdateAction;
import net.minestom.server.entity.Player;
import java.util.List;

/** Cancel to route an annotation change through an owned world's serialized editing queue. */
public final class AxiomAnnotationUpdateEvent extends AxiomCancellablePlayerInstanceEvent {
    private final List<AnnotationUpdateAction> actions;
    public AxiomAnnotationUpdateEvent(Player player, List<AnnotationUpdateAction> actions) {
        super(player);
        this.actions = List.copyOf(actions);
    }
    public List<AnnotationUpdateAction> actions() { return actions; }
}
