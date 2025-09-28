package fr.ghostrider584.axiom.annotation.storage.json.serializer;

import com.google.gson.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;

import java.lang.reflect.Type;

public class PointAdapter implements JsonSerializer<Point>, JsonDeserializer<Point> {
	@Override
	public JsonElement serialize(Point src, Type typeOfSrc, JsonSerializationContext context) {
		final var obj = new JsonObject();
		obj.addProperty("x", src.x());
		obj.addProperty("y", src.y());
		obj.addProperty("z", src.z());
		return obj;
	}

	@Override
	public Point deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		final var obj = json.getAsJsonObject();
		return new Vec(
				obj.get("x").getAsDouble(),
				obj.get("y").getAsDouble(),
				obj.get("z").getAsDouble()
		);
	}
}