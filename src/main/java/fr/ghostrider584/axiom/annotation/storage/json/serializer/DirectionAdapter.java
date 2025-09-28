package fr.ghostrider584.axiom.annotation.storage.json.serializer;

import com.google.gson.*;
import net.minestom.server.utils.Direction;

import java.lang.reflect.Type;

public class DirectionAdapter implements JsonSerializer<Direction>, JsonDeserializer<Direction> {
	@Override
	public JsonElement serialize(Direction src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.name());
	}

	@Override
	public Direction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		return Direction.valueOf(json.getAsString());
	}
}