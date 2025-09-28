package fr.ghostrider584.axiom.network;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.minestom.server.network.NetworkBuffer.BYTE;

public interface IdentifiedNetworkType<T> {
	NetworkTypeEntry<? extends T> entry();

	record NetworkTypeEntry<T>(byte id, Class<T> type, NetworkBuffer.Type<T> networkType) {
		public static <T> NetworkTypeEntry<T> entry(int id, Class<T> type, NetworkBuffer.Type<T> networkType) {
			return new NetworkTypeEntry<>((byte) id, type, networkType);
		}
	}

	static <E extends IdentifiedNetworkType<T>, T> @Nullable NetworkTypeEntry<? extends T> findById(E[] values, byte id) {
		for (final var enumValue : values) {
			if (enumValue.entry().id() == id) {
				return enumValue.entry();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	static <E extends IdentifiedNetworkType<T>, T> @Nullable NetworkTypeEntry<T> findByInstance(E[] values, T instance) {
		final var instanceClass = instance.getClass();
		for (final var enumValue : values) {
			if (enumValue.entry().type() == instanceClass) {
				return (NetworkTypeEntry<T>) enumValue.entry();
			}
		}
		return null;
	}

	static <E extends Enum<E> & IdentifiedNetworkType<T>, T> NetworkBuffer.Type<E> Enum(Class<E> enumClass, E[] values) {
		return new NetworkBuffer.Type<>() {
			@Override
			public void write(@NotNull NetworkBuffer buffer, @NotNull E value) {
				buffer.write(BYTE, value.entry().id());
			}

			@Override
			public @NotNull E read(@NotNull NetworkBuffer buffer) {
				final byte id = buffer.read(BYTE);
				for (final var enumValue : values) {
					if (enumValue.entry().id() == id) {
						return enumValue;
					}
				}
				throw new IllegalArgumentException("Unknown " + enumClass.getSimpleName() + " ID: " + id);
			}
		};
	}

	static <E extends IdentifiedNetworkType<T>, T> NetworkBuffer.Type<T> Polymorphic(E[] values) {
		return new NetworkBuffer.Type<>() {
			@Override
			public void write(@NotNull NetworkBuffer buffer, @NotNull T instance) {
				final var entry = IdentifiedNetworkType.findByInstance(values, instance);
				Objects.requireNonNull(entry, "Cannot write unknown instance of type " + instance.getClass().getName());

				buffer.write(BYTE, entry.id());
				buffer.write(entry.networkType(), instance);
			}

			@Override
			public @NotNull T read(@NotNull NetworkBuffer buffer) {
				final byte id = buffer.read(BYTE);
				final var entry = IdentifiedNetworkType.findById(values, id);
				Objects.requireNonNull(entry, "Unknown type ID: " + id);

				return buffer.read(entry.networkType());
			}
		};
	}
}