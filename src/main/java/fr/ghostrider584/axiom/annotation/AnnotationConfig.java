package fr.ghostrider584.axiom.annotation;

public record AnnotationConfig(
		boolean enabled,
		int maxAnnotationsPerWorld,
		boolean saveOnShutdown
) {
	public static AnnotationConfig defaultConfig() {
		return new AnnotationConfig(true, 10000, true);
	}

	public static AnnotationConfig disabled() {
		return new AnnotationConfig(false, 0, false);
	}
}