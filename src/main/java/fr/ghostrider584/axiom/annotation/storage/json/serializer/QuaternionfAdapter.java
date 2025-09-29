package fr.ghostrider584.axiom.annotation.storage.json.serializer;

import com.google.gson.*;
import fr.ghostrider584.axiom.math.Quaternionf;

import java.lang.reflect.Type;

public class QuaternionfAdapter implements JsonSerializer<Quaternionf>, JsonDeserializer<Quaternionf> {
	@Override
	public JsonElement serialize(Quaternionf src, Type typeOfSrc, JsonSerializationContext context) {
		final var obj = new JsonObject();
		obj.addProperty("x", src.x());
		obj.addProperty("y", src.y());
		obj.addProperty("z", src.z());
		obj.addProperty("w", src.w());
		return obj;
	}

	@Override
	public Quaternionf deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		final var obj = json.getAsJsonObject();
		return new Quaternionf(
				obj.get("x").getAsFloat(),
				obj.get("y").getAsFloat(),
				obj.get("z").getAsFloat(),
				obj.get("w").getAsFloat()
		);
	}
}