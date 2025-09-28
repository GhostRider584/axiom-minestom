package fr.ghostrider584.axiom.util;

import fr.ghostrider584.axiom.metadata.MetadataCodecRegistry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.component.DataComponent;
import net.minestom.server.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class EntityNbtHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(EntityNbtHelper.class);

	@SuppressWarnings("unchecked")
	public static void applyComponents(Entity entity, CompoundBinaryTag tag) {
		for (final var key : tag.keySet()) {
			if (!Key.parseable(key)) {
				continue;
			}

			@SuppressWarnings("PatternValidation") final var dataComponent = DataComponent.fromKey(key);
			if (dataComponent != null) {
				final var componentNbt = tag.get(key);
				LOGGER.trace("Processing component with key {}, component is {}", key, componentNbt);
				if (componentNbt != null) {
					final var value = dataComponent.decode(Transcoder.NBT, componentNbt).orElseThrow();
					LOGGER.trace("Valid component {} will be set to {}", key, value);
					entity.set((DataComponent<? super Object>) dataComponent, value);
				}
			}
		}
	}

	public static void applyNbtChanges(Entity entity, CompoundBinaryTag nbt) {
		var axiomData = nbt.getCompound("data");
		if (axiomData != CompoundBinaryTag.empty()) {
			axiomData.remove("axiom:modify"); // FIXME: not working

			// marker name basic color support
			final var markerName = axiomData.getString("name");
			if (!markerName.isEmpty()) {
				axiomData = axiomData.putString("name", markerName.replace("&", "§"));
			}

			entity.tagHandler().updateContent(axiomData);
		}

		applyComponents(entity, axiomData);
		MetadataCodecRegistry.applyNBT(entity.getEntityMeta(), nbt);

		if (LOGGER.isTraceEnabled()) {
			try {
				LOGGER.trace("Applied NBT to entity {} ({}):\n{}",
						entity.getEntityType().key(), entity.getUuid(), TagStringIO.tagStringIO().asString(nbt));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		final var entityPosition = entity.getPosition();
		float pitch = entityPosition.pitch();
		if (axiomData.get("pitch") != null) {
			pitch = axiomData.getFloat("pitch");
		}

		float yaw = entityPosition.pitch();
		if (axiomData.get("yaw") != null) {
			yaw = axiomData.getFloat("yaw");
		}
		entity.setView(yaw, pitch);

		LOGGER.trace("Applied NBT changes to entity {}", entity.getUuid());
	}
}
