package AEDs3.DataBase.Compression;

import AEDs3.DataBase.Compression.Compressors.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Classe responsável por comprimir e descomprimir arquivos utilizando
 * diferentes algoritmos de compressão.
 */
public class Compressor {
	/**
	 * Tamanho do buffer em bytes utilizado para operações de entrada e saída.
	 */
	private static final int BUFFER_SIZE_BYTES = 2 * (1 << 20); // 2MB.

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

		InputStream in = new BufferedInputStream(new FileInputStream(packPath), BUFFER_SIZE_BYTES / 2);
		OutputStream out = new BufferedOutputStream(new FileOutputStream(dst), BUFFER_SIZE_BYTES / 2);

		type.getCompressor().compress(in, out);

		Files.delete(Paths.get(packPath));
		out.flush();
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
		InputStream in = new BufferedInputStream(new FileInputStream(src), BUFFER_SIZE_BYTES / 2);
		OutputStream out = new BufferedOutputStream(new FileOutputStream(packPath), BUFFER_SIZE_BYTES / 2);

		type.getCompressor().decompress(in, out);

		// Um flush da stream de saída é necessário para garantir que os dados foram escritos.
		out.flush();
		String[] res = FilePacker.unpack(packPath);
		Files.delete(Paths.get(packPath));

		return res;
	}
}
