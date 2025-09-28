package fr.ghostrider584.axiom.annotation.type;

import fr.ghostrider584.axiom.network.IdentifiedNetworkType;
import net.minestom.server.network.NetworkBuffer;

public enum AnnotationType implements IdentifiedNetworkType<AnnotationUpdateAction> {
	CREATE(NetworkTypeEntry.entry(0, CreateAnnotation.class, CreateAnnotation.TYPE)),
	DELETE(NetworkTypeEntry.entry(1, DeleteAnnotation.class, DeleteAnnotation.TYPE)),
	MOVE(NetworkTypeEntry.entry(2, MoveAnnotation.class, MoveAnnotation.TYPE)),
	CLEAR(NetworkTypeEntry.entry(3, ClearAllAnnotations.class, ClearAllAnnotations.TYPE)),
	ROTATE(NetworkTypeEntry.entry(4, RotateAnnotation.class, RotateAnnotation.TYPE));

	public static final NetworkBuffer.Type<AnnotationType> TYPE = IdentifiedNetworkType.Enum(AnnotationType.class, values());

	private final NetworkTypeEntry<? extends AnnotationUpdateAction> entry;

	AnnotationType(NetworkTypeEntry<? extends AnnotationUpdateAction> entry) {
		this.entry = entry;
	}

	@Override
	public NetworkTypeEntry<? extends AnnotationUpdateAction> entry() {
		return entry;
	}
}