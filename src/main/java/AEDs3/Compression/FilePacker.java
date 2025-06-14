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
			// Lê a quantidade de arquivos e suas informações (nome e posição)
			int numFiles = raf.readInt();
			String[] fileNames = new String[numFiles];
			long[] positions = new long[numFiles];

			for (int i = 0; i < numFiles; i++) {
				fileNames[i] = raf.readUTF();
				positions[i] = raf.readLong();
			}

			// Configura os parametros da barra de progresso
			int outerBarWidth = 50; // progresso total
			int innerBarWidth = 50; // progresso por arquivo
			long lastOuterPrintTime = 0;
			long lastInnerPrintTime = 0;

			// Esconde o cursor e posiciona as barras de progresso na tela
			System.out.print("\033[?25l\n\n\033[A\033[A");

			// Buffer para ler os chunks (ex.: 8KB)
			byte[] buffer = new byte[1 << 13];

			// Processa cada arquivo do arquivo empacotado
			for (int i = 0; i < numFiles; i++) {
				raf.seek(positions[i]);
				long fileSize = raf.readLong();
				long totalBytesRead = 0;

				// Atualiza a barra de progresso total
				long currentTime = System.currentTimeMillis();
				if (currentTime - lastOuterPrintTime >= 50) {
					printProgressBar(i, numFiles, outerBarWidth, "Total");
					lastOuterPrintTime = currentTime;
				}

				// Cria o objeto File para o arquivo de saída
				File outFile = new File(fileNames[i]);
				// Verifica se há diretórios na estrutura de arquivo
				File parentDirectory = outFile.getParentFile();
				if (parentDirectory != null && !parentDirectory.exists())
					if (!parentDirectory.mkdirs())
						throw new RuntimeException("Falha ao criar diretórios para: " + outFile.getAbsolutePath());

				try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile))) {
					// Ajusta o cursor do terminal para a barra de progresso interna
					System.out.print("\033[B\033[2K\033[A");

					long remaining = fileSize;
					while (remaining > 0) {
						int chunkSize = (int) Math.min(buffer.length, remaining);
						int bytesRead = raf.read(buffer, 0, chunkSize);
						if (bytesRead == -1)
							break; // salvaguarda, embora não deva ocorrer aqui

						out.write(buffer, 0, bytesRead);
						totalBytesRead += bytesRead;
						remaining -= bytesRead;

						// Atualiza a barra de progresso interna a cada ~50ms
						currentTime = System.currentTimeMillis();
						if (currentTime - lastInnerPrintTime >= 50) {
							System.out.print("\033[B"); // move o cursor para baixo para a barra de progresso
							printProgressBar(totalBytesRead, fileSize, innerBarWidth, fileNames[i]);
							System.out.print("\033[A"); // volta o cursor para cima
							lastInnerPrintTime = currentTime;
						}
					}
				}
			}

			// Restaura o cursor e limpa a exibição das barras de progresso
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
