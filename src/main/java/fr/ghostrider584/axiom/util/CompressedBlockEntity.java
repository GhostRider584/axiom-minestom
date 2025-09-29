package fr.ghostrider584.axiom.util;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDictCompress;
import com.github.luben.zstd.ZstdDictDecompress;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

public record CompressedBlockEntity(int originalSize, byte compressionDict, byte[] compressedBytes) {
	private static final byte COMPRESSION_DICT_ID = 0;

	private static final ZstdDictCompress COMPRESSION_DICTIONARY;
	private static final ZstdDictDecompress DECOMPRESSION_DICTIONARY;

	static {
		System.out.println(CompressedBlockEntity.class.getClassLoader().getResourceAsStream("block_entities_v1.dict"));
		try (final var stream = Objects.requireNonNull(CompressedBlockEntity.class.getClassLoader().getResourceAsStream("block_entities_v1.dict"))) {
			final byte[] bytes = stream.readAllBytes();
			COMPRESSION_DICTIONARY = new ZstdDictCompress(bytes, Zstd.defaultCompressionLevel());
			DECOMPRESSION_DICTIONARY = new ZstdDictDecompress(bytes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static CompoundBinaryTag decompress(CompressedBlockEntity compressed) throws IOException {
		if (compressed.compressionDict() != COMPRESSION_DICT_ID) {
			throw new UnsupportedOperationException("Unknown compression dict: " + compressed.compressionDict());
		}

		final byte[] tagBytes = Zstd.decompress(compressed.compressedBytes(), DECOMPRESSION_DICTIONARY, compressed.originalSize());
		return BinaryTagIO.reader().read(new ByteArrayInputStream(tagBytes));
	}

	public static CompressedBlockEntity compress(CompoundBinaryTag tag) throws IOException {
		final var baos = new ByteArrayOutputStream();
		BinaryTagIO.writer().write(tag, baos);

		final byte[] tagBytes = baos.toByteArray();
		final byte[] compressed = Zstd.compress(tagBytes, COMPRESSION_DICTIONARY);
		return new CompressedBlockEntity(tagBytes.length, (byte) 0, compressed);
	}
}
