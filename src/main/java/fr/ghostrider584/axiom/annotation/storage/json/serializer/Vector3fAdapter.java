package fr.ghostrider584.axiom.annotation.storage.json.serializer;

import com.google.gson.*;
import org.joml.Vector3f;

import java.lang.reflect.Type;

public class Vector3fAdapter implements JsonSerializer<Vector3f>, JsonDeserializer<Vector3f> {
	@Override
	public JsonElement serialize(Vector3f src, Type typeOfSrc, JsonSerializationContext context) {
		final var obj = new JsonObject();
		obj.addProperty("x", src.x);
		obj.addProperty("y", src.y);
		obj.addProperty("z", src.z);
		return obj;
	}

	@Override
	public Vector3f deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		final var obj = json.getAsJsonObject();
		return new Vector3f(
				obj.get("x").getAsFloat(),
				obj.get("y").getAsFloat(),
				obj.get("z").getAsFloat()
		);
	}
}