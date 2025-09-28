package fr.ghostrider584.axiom.annotation.data;

import fr.ghostrider584.axiom.network.IdentifiedNetworkType;
import net.minestom.server.network.NetworkBuffer;

public enum AnnotationDataType implements IdentifiedNetworkType<AnnotationData> {
	LINE(NetworkTypeEntry.entry(0, LineAnnotationData.class, LineAnnotationData.TYPE)),
	TEXT(NetworkTypeEntry.entry(1, TextAnnotationData.class, TextAnnotationData.TYPE)),
	IMAGE(NetworkTypeEntry.entry(2, ImageAnnotationData.class, ImageAnnotationData.TYPE)),
	FREEHAND_OUTLINE(NetworkTypeEntry.entry(3, FreehandOutlineAnnotationData.class, FreehandOutlineAnnotationData.TYPE)),
	LINES_OUTLINE(NetworkTypeEntry.entry(4, LinesOutlineAnnotationData.class, LinesOutlineAnnotationData.TYPE)),
	BOX_OUTLINE(NetworkTypeEntry.entry(5, BoxOutlineAnnotationData.class, BoxOutlineAnnotationData.TYPE));

	public static final NetworkBuffer.Type<AnnotationDataType> TYPE = IdentifiedNetworkType.Enum(AnnotationDataType.class, values());

	private final NetworkTypeEntry<? extends AnnotationData> entry;

	AnnotationDataType(NetworkTypeEntry<? extends AnnotationData> entry) {
		this.entry = entry;
	}

	@Override
	public NetworkTypeEntry<? extends AnnotationData> entry() {
		return entry;
	}
}