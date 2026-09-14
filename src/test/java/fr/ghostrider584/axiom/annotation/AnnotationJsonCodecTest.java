package fr.ghostrider584.axiom.annotation;

import fr.ghostrider584.axiom.annotation.data.*;
import fr.ghostrider584.axiom.annotation.storage.json.AnnotationJsonCodec;
import fr.ghostrider584.axiom.math.Quaternionf;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnnotationJsonCodecTest {
    private final AnnotationJsonCodec codec = new AnnotationJsonCodec(256, 1024 * 1024);

    @Test void everyAnnotationTypeRoundTripsWithItsDiscriminatorAndOwnedArrays() {
        var rotation = new Quaternionf(0, 0, 0, 1);
        List<AnnotationData> examples = List.of(
                new TextAnnotationData("Panel", Vec.ZERO, rotation, Direction.UP, 0, 1, (byte) 0, 1, false),
                new ImageAnnotationData("https://example.com/a.png", Vec.ZERO, rotation, Direction.UP, 0, 1, 1, (byte) 0),
                new BoxOutlineAnnotationData(Vec.ZERO, new Vec(1, 1, 1), 1),
                new LineAnnotationData(Vec.ZERO, 1, 1, new byte[] {1, 2}),
                new LinesOutlineAnnotationData(new long[] {1, 2}, 1),
                new FreehandOutlineAnnotationData(Vec.ZERO, 2, 1, new byte[] {1, 2}));
        for (var example : examples) {
            var id = UUID.randomUUID();
            var encoded = codec.encode(Map.of(id, example));
            var restored = codec.decode(encoded).get(id);
            assertEquals(example.getClass(), restored.getClass());
            assertArrayEquals(encoded, codec.encode(Map.of(id, restored)));
            assertNotSame(example, restored);
        }
    }

    @Test void malformedUnknownOversizedOrIncompleteDataFailsInsteadOfBecomingAnEmptyWorld() {
        assertThrows(RuntimeException.class, () -> codec.decode("null".getBytes(StandardCharsets.UTF_8)));
        assertThrows(RuntimeException.class, () -> codec.decode(("{\"" + UUID.randomUUID() + "\":{\"type\":\"future\",\"data\":{}}}").getBytes(StandardCharsets.UTF_8)));
        assertThrows(RuntimeException.class, () -> codec.encode(Map.of(UUID.randomUUID(), new BoxOutlineAnnotationData(null, Vec.ZERO, 1))));
        assertThrows(RuntimeException.class, () -> codec.encode(Map.of(UUID.randomUUID(), new LineAnnotationData(Vec.ZERO, Float.NaN, 1, new byte[0]))));
        assertThrows(RuntimeException.class, () -> new AnnotationJsonCodec(1, 20).encode(Map.of(UUID.randomUUID(), new BoxOutlineAnnotationData(Vec.ZERO, Vec.ZERO, 1))));
        var two = Map.<UUID, AnnotationData>of(UUID.randomUUID(), new LinesOutlineAnnotationData(new long[0], 1), UUID.randomUUID(), new LinesOutlineAnnotationData(new long[0], 1));
        assertThrows(RuntimeException.class, () -> new AnnotationJsonCodec(1, 1024).encode(two));
    }
}
