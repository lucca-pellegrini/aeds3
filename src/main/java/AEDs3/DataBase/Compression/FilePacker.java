package AEDs3.DataBase.Compression;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A classe FilePacker fornece métodos para empacotar e desempacotar arquivos.
 * O empacotamento envolve a combinação de vários arquivos em um único arquivo,
 * enquanto o desempacotamento extrai os arquivos individuais do arquivo
 * empacotado.
 */
class FilePacker {

	/**
	 * Empacota uma lista de arquivos em um único arquivo de destino.
	 *
	 * @param orig Um array de strings contendo os nomes dos arquivos a serem
	 *             empacotados.
	 * @param dst  O nome do arquivo de destino onde os arquivos serão empacotados.
	 * @throws IOException Se ocorrer um erro de E/S durante o empacotamento.
	 */
	public static void pack(String[] orig, String dst) throws IOException {
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dst)))) {
			// Escreve o número de arquivos no arquivo de destino
			out.writeInt(orig.length);

			// Array para armazenar as posições dos arquivos no arquivo de destino
			long[] positions = new long[orig.length];

			// Escreve os nomes dos arquivos e um espaço reservado para as posições
			for (String fileName : orig) {
				out.writeUTF(fileName);
				out.writeLong(0);
			}

			// Escreve o conteúdo de cada arquivo no arquivo de destino
			for (int i = 0; i < orig.length; i++) {
				// Flush the buffer to ensure all data is written before capturing the position
				out.flush();
				positions[i] = out.size();

				try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(orig[i]))) {
					long fileSize = new File(orig[i]).length();
					out.writeLong(fileSize);

					int byteData;
					while ((byteData = in.read()) != -1) {
						out.write(byteData);
					}
				}
			}

			// Atualiza as posições dos arquivos no arquivo de destino
			try (RandomAccessFile raf = new RandomAccessFile(dst, "rw")) {
				raf.seek(Integer.BYTES);
				for (int i = 0; i < orig.length; i++) {
					raf.readUTF();
					raf.writeLong(positions[i]);
				}
			}
		}
	}

	/**
	 * Desempacota um arquivo empacotado, extraindo os arquivos individuais.
	 *
	 * @param packedFile O nome do arquivo empacotado a ser desempacotado.
	 * @return Um array de strings contendo os nomes dos arquivos extraídos.
	 * @throws IOException Se ocorrer um erro de E/S durante o desempacotamento.
	 */
	public static String[] unpack(String packedFile) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(packedFile, "r")) {
			// Lê o número de arquivos no arquivo empacotado
			int numFiles = raf.readInt();
			String[] fileNames = new String[numFiles];
			long[] positions = new long[numFiles];

			// Lê os nomes dos arquivos e suas posições
			for (int i = 0; i < numFiles; i++) {
				fileNames[i] = raf.readUTF();
				positions[i] = raf.readLong();
			}

			// Extrai cada arquivo do arquivo empacotado
			for (int i = 0; i < numFiles; i++) {
				raf.seek(positions[i]);
				long fileSize = raf.readLong();

				try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileNames[i]))) {
					for (long j = 0; j < fileSize; j++) {
						out.write(raf.read());
					}
				}
			}

			return fileNames;
		}
	}
}
