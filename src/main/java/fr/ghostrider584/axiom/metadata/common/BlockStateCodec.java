package fr.ghostrider584.axiom.metadata.common;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

public final class BlockStateCodec {

	@SuppressWarnings("PatternValidation")
	public static final Codec<Block> CODEC = new Codec<>() {
		@Override
		public <D> @NotNull Result<Block> decode(@NotNull Transcoder<D> coder, @NotNull D value) {
			return switch (coder.getMap(value)) {
				case Result.Ok<Transcoder.MapLike<D>>(var map) -> decodeBlockFromMap(coder, map);
				case Result.Error<Transcoder.MapLike<D>> error -> error.cast();
			};
		}

		private <D> Result<Block> decodeBlockFromMap(Transcoder<D> coder, Transcoder.MapLike<D> map) {
			var blockNameResult = extractString(coder, map, "Name");
			if (!(blockNameResult instanceof Result.Ok<String>(var blockName))) {
				return blockNameResult.cast();
			}

			return switch (map.getValue("Properties")) {
				case Result.Ok<D>(var propertiesValue) -> decodeBlockWithProperties(coder, blockName, propertiesValue);
				case Result.Error<D> ignored -> createBlockFromName(blockName);
			};
		}

		private <D> Result<String> extractString(Transcoder<D> coder, Transcoder.MapLike<D> map, String key) {
			return switch (map.getValue(key)) {
				case Result.Ok<D>(var value) -> coder.getString(value);
				case Result.Error<D> error -> error.cast();
			};
		}

		private <D> Result<Block> decodeBlockWithProperties(Transcoder<D> coder, String blockName, D propertiesValue) {
			return switch (coder.getMap(propertiesValue)) {
				case Result.Ok<Transcoder.MapLike<D>>(var propertiesMap) -> {
					var stateString = buildStateString(coder, blockName, propertiesMap);
					var block = Block.fromState(stateString);
					yield block != null
							? new Result.Ok<>(block)
							: new Result.Error<>("Invalid block state: " + stateString);
				}
				case Result.Error<Transcoder.MapLike<D>> error -> error.cast();
			};
		}

		private <D> String buildStateString(Transcoder<D> coder, String blockName, Transcoder.MapLike<D> propertiesMap) {
			var propertiesJoiner = new StringJoiner(",", "[", "]");

			for (var key : propertiesMap.keys()) {
				if (propertiesMap.getValue(key) instanceof Result.Ok<D>(var propValue) &&
						coder.getString(propValue) instanceof Result.Ok<String>(var propString)) {
					propertiesJoiner.add(key + "=" + propString);
				}
			}

			return blockName + propertiesJoiner;
		}

		private Result<Block> createBlockFromName(String blockName) {
			var block = Block.fromKey(blockName);
			return block != null
					? new Result.Ok<>(block)
					: new Result.Error<>("Unknown block: " + blockName);
		}

		@Override
		public <D> @NotNull Result<D> encode(@NotNull Transcoder<D> coder, @Nullable Block value) {
			if (value == null) {
				return new Result.Error<>("Block cannot be null");
			}

			var builder = coder.createMap()
					.put("Name", coder.createString(value.key().asString()));

			var properties = value.properties();
			if (!properties.isEmpty()) {
				var propertiesBuilder = coder.createMap();
				properties.forEach((key, val) ->
						propertiesBuilder.put(key, coder.createString(val)));
				builder.put("Properties", propertiesBuilder.build());
			}

			return new Result.Ok<>(builder.build());
		}
	};
}