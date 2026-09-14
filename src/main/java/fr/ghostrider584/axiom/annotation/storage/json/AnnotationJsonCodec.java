package fr.ghostrider584.axiom.annotation.storage.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.ghostrider584.axiom.annotation.data.AnnotationData;
import net.minestom.server.network.NetworkBuffer;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** File-format codec for host-owned storage. Malformed or unsupported data is never discarded. */
public final class AnnotationJsonCodec {
    private static final Gson GSON = JsonAnnotationStorage.createGson();
    private static final Type TYPE = new TypeToken<Map<UUID, AnnotationData>>() {}.getType();
    private final int maxAnnotations, maxBytes;

    public AnnotationJsonCodec(int maxAnnotations, int maxBytes) {
        if (maxAnnotations < 1 || maxBytes < 1) throw new IllegalArgumentException("Annotation limits must be positive");
        this.maxAnnotations = maxAnnotations; this.maxBytes = maxBytes;
    }

    public byte[] encode(Map<UUID, AnnotationData> annotations) {
        validate(annotations);
        // The explicit interface-valued type is essential: it preserves the polymorphic discriminator.
        byte[] bytes = GSON.toJson(annotations, TYPE).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxBytes) throw new IllegalArgumentException("Annotation document exceeds its byte limit");
        return bytes;
    }

    public Map<UUID, AnnotationData> decode(byte[] bytes) {
        if (bytes.length > maxBytes) throw new IllegalArgumentException("Annotation document exceeds its byte limit");
        Map<UUID, AnnotationData> result = GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), TYPE);
        validate(result);
        // Re-encoding also rejects non-finite JSON numbers before clients receive them.
        encode(result);
        return Map.copyOf(result);
    }

    private void validate(Map<UUID, AnnotationData> annotations) {
        Objects.requireNonNull(annotations, "Annotation document cannot be null");
        if (annotations.size() > maxAnnotations) throw new IllegalArgumentException("Too many annotations in this workspace");
        long wireBytes = 0;
        for (var entry : annotations.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "Annotation identity");
            Objects.requireNonNull(entry.getValue(), "Annotation data");
            // Checks required fields and supported wire types as well as bounding the aggregate payload.
            wireBytes += NetworkBuffer.makeArray(AnnotationData.TYPE, entry.getValue()).length + 32L;
            if (wireBytes > maxBytes) throw new IllegalArgumentException("Annotation wire data exceeds its byte limit");
        }
    }
}
