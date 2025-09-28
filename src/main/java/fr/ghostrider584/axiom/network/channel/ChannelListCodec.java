package fr.ghostrider584.axiom.network.channel;

import java.nio.charset.StandardCharsets;
import java.util.*;

final class ChannelListCodec {

	private ChannelListCodec() {
	}

	static List<String> decode(byte[] data) {
		if (data == null || data.length == 0) {
			return List.of();
		}

		final var channels = new ArrayList<String>();
		int start = 0;

		for (int i = 0; i <= data.length; i++) {
			if (i == data.length || data[i] == 0) { // null byte separator or end
				if (i > start) { // non-empty channel name
					final byte[] channelBytes = Arrays.copyOfRange(data, start, i);
					final var channel = new String(channelBytes, StandardCharsets.UTF_8);
					channels.add(channel);
				}
				start = i + 1;
			}
		}

		return List.copyOf(channels);
	}

	static byte[] encode(Collection<String> channels) {
		if (channels == null || channels.isEmpty()) {
			return new byte[0];
		}

		int totalLength = 0;
		for (final var channel : channels) {
			if (channel == null) {
				throw new IllegalArgumentException("Channel name cannot be null");
			}
			totalLength += channel.getBytes(StandardCharsets.UTF_8).length;
		}
		totalLength += Math.max(0, channels.size() - 1); // separators (no trailing separator)

		final byte[] result = new byte[totalLength];
		int pos = 0;
		boolean first = true;

		for (String channel : channels) {
			if (!first) {
				result[pos++] = 0; // null separator
			}

			final byte[] channelBytes = channel.getBytes(StandardCharsets.UTF_8);
			System.arraycopy(channelBytes, 0, result, pos, channelBytes.length);
			pos += channelBytes.length;
			first = false;
		}

		return result;
	}
}