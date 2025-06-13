package AEDs3.Compression;

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
	 * Aviso: esse método foi em grande parte otimizado com ajuda de um LLM. O
	 * método em si, além da barra de progresso que ele usa, foram escritos à mão,
	 * mas a implementação do buffer de transcrição, que dramaticamente acelera a
	 * execução, foi inteiramente feita por um LLM.
	 *
	 * @param packedFile O nome do arquivo empacotado a ser desempacotado.
	 * @return Um array de strings contendo os nomes dos arquivos extraídos.
	 * @throws IOException Se ocorrer um erro de E/S durante o desempacotamento.
	 */
	public static String[] unpack(String packedFile) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(packedFile, "r")) {
			// Read the number of files and their metadata (file names and positions)
			int numFiles = raf.readInt();
			String[] fileNames = new String[numFiles];
			long[] positions = new long[numFiles];

			for (int i = 0; i < numFiles; i++) {
				fileNames[i] = raf.readUTF();
				positions[i] = raf.readLong();
			}

			// Set up progress bar parameters
			int outerBarWidth = 50; // for overall progress
			int innerBarWidth = 50; // for each file's progress
			long lastOuterPrintTime = 0;
			long lastInnerPrintTime = 0;

			// Hide the cursor and position the progress bars on-screen
			System.out.print("\033[?25l\n\n\033[A\033[A");

			// Create a buffer to read chunks (e.g., 8KB)
			byte[] buffer = new byte[8192];

			// Process each file in the packed archive
			for (int i = 0; i < numFiles; i++) {
				raf.seek(positions[i]);
				long fileSize = raf.readLong();
				long totalBytesRead = 0;

				// Update outer progress bar occasionally
				long currentTime = System.currentTimeMillis();
				if (currentTime - lastOuterPrintTime >= 50) {
					printProgressBar(i, numFiles, outerBarWidth, "Total");
					lastOuterPrintTime = currentTime;
				}

				try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileNames[i]))) {
					// Adjust the terminal cursor for the inner progress bar (per file)
					System.out.print("\033[B\033[2K\033[A");

					long remaining = fileSize;
					while (remaining > 0) {
						int chunkSize = (int) Math.min(buffer.length, remaining);
						int bytesRead = raf.read(buffer, 0, chunkSize);
						if (bytesRead == -1)
							break; // safeguard, though it shouldn't happen here

						out.write(buffer, 0, bytesRead);
						totalBytesRead += bytesRead;
						remaining -= bytesRead;

						// Update inner progress bar every ~50ms
						currentTime = System.currentTimeMillis();
						if (currentTime - lastInnerPrintTime >= 50) {
							System.out.print("\033[B"); // move the cursor down for the progress bar
							printProgressBar(totalBytesRead, fileSize, innerBarWidth, fileNames[i]);
							System.out.print("\033[A"); // move it back up
							lastInnerPrintTime = currentTime;
						}
					}
				}
			}

			// Restore the cursor and clean up progress bar display
			System.out.println("\033[?25h\033[2K\033[B\033[2K\033[A\033[A");
			return fileNames;
		}
	}

	/**
	 * Imprime uma barra de progresso no console.
	 *
	 * @param current  O progresso atual.
	 * @param total    O valor total para completar o progresso.
	 * @param barWidth A largura da barra de progresso.
	 * @param label    O rótulo associado à barra de progresso.
	 */
	private static void printProgressBar(long current, long total, int barWidth, String label) {
		double progress = (double) current / total;
		int completed = (int) (progress * barWidth);
		StringBuilder bar = new StringBuilder("[");

		for (int i = 0; i < barWidth; i++) {
			if (i < completed) {
				bar.append("=");
			} else {
				bar.append(" ");
			}
		}
		bar.append("]");

		System.out.printf("\r%s % 3d%% %s", bar.toString(), (int) (progress * 100), label);
	}
}
