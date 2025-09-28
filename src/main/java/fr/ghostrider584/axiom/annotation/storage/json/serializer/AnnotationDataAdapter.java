package fr.ghostrider584.axiom.annotation.storage.json.serializer;

import com.google.gson.*;
import fr.ghostrider584.axiom.annotation.data.*;

import java.lang.reflect.Type;

public class AnnotationDataAdapter implements JsonSerializer<AnnotationData>, JsonDeserializer<AnnotationData> {
	@Override
	public JsonElement serialize(AnnotationData src, Type typeOfSrc, JsonSerializationContext context) {
		final var obj = new JsonObject();

		if (src instanceof LineAnnotationData line) {
			obj.addProperty("type", "line");
			obj.add("data", context.serialize(line));
		} else if (src instanceof TextAnnotationData text) {
			obj.addProperty("type", "text");
			obj.add("data", context.serialize(text));
		} else if (src instanceof ImageAnnotationData image) {
			obj.addProperty("type", "image");
			obj.add("data", context.serialize(image));
		} else if (src instanceof BoxOutlineAnnotationData box) {
			obj.addProperty("type", "box");
			obj.add("data", context.serialize(box));
		} else if (src instanceof FreehandOutlineAnnotationData freehand) {
			obj.addProperty("type", "freehand");
			obj.add("data", context.serialize(freehand));
		} else if (src instanceof LinesOutlineAnnotationData lines) {
			obj.addProperty("type", "lines");
			obj.add("data", context.serialize(lines));
		}

		return obj;
	}

	@Override
	public AnnotationData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		final var obj = json.getAsJsonObject();
		final var type = obj.get("type").getAsString();
		final var data = obj.get("data");

		return switch (type) {
			case "line" -> context.deserialize(data, LineAnnotationData.class);
			case "text" -> context.deserialize(data, TextAnnotationData.class);
			case "image" -> context.deserialize(data, ImageAnnotationData.class);
			case "box" -> context.deserialize(data, BoxOutlineAnnotationData.class);
			case "freehand" -> context.deserialize(data, FreehandOutlineAnnotationData.class);
			case "lines" -> context.deserialize(data, LinesOutlineAnnotationData.class);
			default -> throw new JsonParseException("Unknown annotation type: " + type);
		};
	}
}