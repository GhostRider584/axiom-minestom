package fr.ghostrider584.axiom.metadata;

import net.minestom.server.codec.Codec;

import java.util.HashMap;

public interface ExtraCodecs {

	static Codec<Byte> StringToByte(String... names) {
		final var stringToId = new HashMap<String, Byte>();
		final var idToString = new HashMap<Byte, String>();

		for (int i = 0; i < names.length; i++) {
			final var name = names[i];
			final byte id = (byte) i;
			stringToId.put(name, id);
			idToString.put(id, name);
		}

		return Codec.STRING.transform(
				name -> stringToId.getOrDefault(name, (byte) 0),
				id -> idToString.getOrDefault(id, names[0])
		);
	}

	static <E extends Enum<E>> Codec<Byte> EnumOrdinal(Class<E> enumClass) {
		final var constants = enumClass.getEnumConstants();
		return Codec.STRING.transform(
				name -> (byte) Enum.valueOf(enumClass, name.toUpperCase()).ordinal(),
				ordinal -> constants[ordinal].name().toLowerCase()
		);
	}
}
