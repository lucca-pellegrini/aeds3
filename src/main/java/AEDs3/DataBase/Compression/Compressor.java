package AEDs3.DataBase.Compression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Classe responsável por comprimir e descomprimir arquivos utilizando
 * diferentes algoritmos de compressão.
 */
public class Compressor {

	/**
	 * Comprime os arquivos especificados no array de origem para o destino
	 * especificado, utilizando o tipo de compressão fornecido.
	 *
	 * @param src  Array de caminhos dos arquivos a serem comprimidos.
	 * @param dst  Caminho do arquivo de destino onde o resultado será salvo.
	 * @param type Tipo de compressão a ser utilizado. Consulte
	 *             {@link CompressionType}.
	 * @throws IOException Se ocorrer um erro de I/O durante o processo.
	 */
	public static void compress(String[] src, String dst, CompressionType type) throws IOException {
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

	/**
	 * Descomprime o arquivo especificado no caminho de origem utilizando o tipo
	 * de compressão fornecido e retorna um array de caminhos dos arquivos
	 * descomprimidos.
	 *
	 * @param src  Caminho do arquivo comprimido a ser descomprimido.
	 * @param type Tipo de compressão utilizado no arquivo. Consulte
	 *             {@link CompressionType}.
	 * @return Array de caminhos dos arquivos descomprimidos.
	 * @throws IOException Se ocorrer um erro de I/O durante o processo.
	 */
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
