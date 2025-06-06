package AEDs3.DataBase.Compression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Compressor {
	public static void compress(String[] src, String dst, CompressionType type) throws IOException{
		String packPath = dst + ".pack";
		FilePacker.pack(src, packPath);

		switch (type) {
		case HUFFMAN:
			Huffman.compressFile(packPath, dst);
			break;
		case LZW:
			LZW.compressFile(packPath, dst);
			break;
		}

		Files.delete(Paths.get(packPath));
	}

	public static void decompress(String src, String dst, CompressionType type) throws IOException {
		String unpackPath = dst + ".unpack";

		switch (type) {
		case HUFFMAN:
			Huffman.decompressFile(src, unpackPath);
			break;
		case LZW:
			LZW.decompressFile(src, unpackPath);
			break;
		}

		FilePacker.unpack(unpackPath);
		Files.delete(Paths.get(unpackPath));
	}
}
