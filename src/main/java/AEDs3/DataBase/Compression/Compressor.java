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

	public static String[] decompress(String src, CompressionType type) throws IOException {
		String packPath = src + ".pack";

		switch (type) {
		case HUFFMAN:
			Huffman.decompressFile(src, packPath);
			break;
		case LZW:
			LZW.decompressFile(src, packPath);
			break;
		}

		String[] res = FilePacker.unpack(packPath);
		Files.delete(Paths.get(packPath));

		return res;
	}
}
