package AEDs3.DataBase.Compression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Esta classe implementa o algoritmo de compressão LZW, que é utilizado para
 * comprimir e descomprimir arquivos binários. O algoritmo LZW é eficiente para
 * compressão de dados que possuem padrões repetitivos.
 */
/**
 * Classe que implementa o algoritmo de compressão LZW.
 * Utilizada para comprimir e descomprimir arquivos binários.
 */
public class LZW {
/**
 * Tamanho do índice em bits utilizado no algoritmo LZW.
 */
public static final int BITS_PER_INDEX = 12; // Tamanho do índice em bits

/**
 * Comprime um arquivo utilizando o algoritmo LZW.
 *
 * @param src Caminho do arquivo de origem a ser comprimido.
 * @param dst Caminho do arquivo de destino onde o arquivo comprimido será salvo.
 * @throws IOException Se ocorrer um erro de I/O durante a compressão.
 */
public static void compressFile(String src, String dst) throws IOException {
		FileInputStream fis = new FileInputStream(new File(src));
		byte[] originalBytes = fis.readAllBytes();
		fis.close();

		byte[] encodedBytes = encode(originalBytes);

		FileOutputStream fos = new FileOutputStream(dst);
		fos.write(encodedBytes);
		fos.close();
	}

/**
 * Descomprime um arquivo utilizando o algoritmo LZW.
 *
 * @param src Caminho do arquivo de origem a ser descomprimido.
 * @param dst Caminho do arquivo de destino onde o arquivo descomprimido será salvo.
 * @throws IOException Se ocorrer um erro de I/O durante a descompressão.
 */
public static void decompressFile(String src, String dst) throws IOException {
		FileInputStream fis = new FileInputStream(src);
		byte[] encodedCopy = fis.readAllBytes();
		fis.close();

		byte[] decodedBytes = decode(encodedCopy);

		FileOutputStream fos = new FileOutputStream(dst);
		fos.write(decodedBytes);
		fos.close();
	}

	/**
	 * Codifica uma sequência de bytes utilizando o algoritmo LZW.
	 *
	 * @param originalBytes A sequência de bytes original a ser comprimida.
	 * @return Um array de bytes que representa os dados comprimidos.
	 * @throws IOException Se ocorrer um erro de I/O durante a compressão.
	 */
	public static byte[] encode(byte[] originalBytes) throws IOException {

		ArrayList<ArrayList<Byte>> dictionary = new ArrayList<>();
		ArrayList<Byte> byteSequence;
		byte b;

		// Inicialização do dicionário com todos os bytes possíveis (-128 a 127)
		for (int j = -128; j < 128; j++) {
			b = (byte) j;
			byteSequence = new ArrayList<>();
			byteSequence.add(b);
			dictionary.add(byteSequence);
		}

		ArrayList<Integer> output = new ArrayList<>();
		int i = 0;

		while (i < originalBytes.length) {
			// System.out.println("Iteração № " + i + "/" + originalBytes.length);
			int progress = (int) ((i / (float) originalBytes.length) * 50);
			StringBuilder progressBar = new StringBuilder("[");
			for (int j = 0; j < 50; j++) {
				if (j < progress) {
					progressBar.append("=");
				} else {
					progressBar.append(" ");
				}
			}
			progressBar.append("] ").append(progress * 2).append("%");
			System.out.print("\r" + progressBar.toString());

			byteSequence = new ArrayList<>();
			b = originalBytes[i];
			byteSequence.add(b);

			int index = dictionary.indexOf(byteSequence);
			int lastIndex = index;

			// Expande a sequência até não encontrar no dicionário
			while (index != -1 && i < originalBytes.length - 1) {
				i++;
				b = originalBytes[i];
				byteSequence.add(b);
				index = dictionary.indexOf(byteSequence);
				if (index != -1)
					lastIndex = index;
			}

			output.add(lastIndex);

			if (dictionary.size() < (Math.pow(2, BITS_PER_INDEX) - 1))
				dictionary.add(byteSequence);

			if (index != -1 && i == originalBytes.length - 1)
				break;
		}

		System.out.println();

		BitArray bits = new BitArray(output.size() * BITS_PER_INDEX);
		int l = output.size() * BITS_PER_INDEX - 1;

		// Converte os índices para bits e armazena em um BitArray
		for (i = output.size() - 1; i >= 0; i--) {
			int n = output.get(i);
			for (int m = 0; m < BITS_PER_INDEX; m++) {
				if (n % 2 == 0)
					bits.clear(l);
				else
					bits.set(l);
				l--;
				n /= 2;
			}
		}

		return bits.toByteArray();
	}

	/**
	 * Decodifica uma sequência de bytes comprimidos utilizando o algoritmo LZW.
	 *
	 * @param encodedBytes A sequência de bytes comprimida a ser descomprimida.
	 * @return Um array de bytes que representa os dados descomprimidos.
	 * @throws IOException Se ocorrer um erro de I/O durante a descompressão.
	 */
	public static byte[] decode(byte[] encodedBytes) throws IOException {

		// Inicializa o BitArray a partir do array de bytes comprimido
		BitArray bits = new BitArray(encodedBytes);
		ArrayList<Integer> indices = new ArrayList<>();

		// Recupera os índices a partir do BitArray
		int k = 0;
		for (int i = 0; i < bits.length() / BITS_PER_INDEX; i++) {
			int n = 0;
			for (int j = 0; j < BITS_PER_INDEX; j++) {
				n = n * 2 + (bits.get(k++) ? 1 : 0);
			}
			indices.add(n);
		}

		// Inicializa o dicionário com todos os bytes possíveis (-128 a 127)
		ArrayList<ArrayList<Byte>> dictionary = new ArrayList<>();
		for (int j = -128; j < 128; j++) {
			ArrayList<Byte> byteSequence = new ArrayList<>();
			byteSequence.add((byte) j);
			dictionary.add(byteSequence);
		}

		ArrayList<Byte> originalBytes = new ArrayList<>();

		// Decodifica os índices para a sequência de bytes original
		// Pega o primeiro índice e recupera sua sequência
		int firstIndex = indices.get(0);
		ArrayList<Byte> w = new ArrayList<>(dictionary.get(firstIndex));
		originalBytes.addAll(w);

		// Processa cada índice restante reconstruindo o dicionário
		for (int i = 1; i < indices.size(); i++) {
			int index = indices.get(i);
			ArrayList<Byte> entry;

			// Se o índice existe no dicionário, recupera sua sequência
			if (index < dictionary.size()) {
				entry = new ArrayList<>(dictionary.get(index));
			} else {
				// Caso especial: o índice não existe ainda no dicionário.
				// Nesse caso, a sequência é a do índice anterior (w) acrescida do primeiro byte
				// de w.
				entry = new ArrayList<>(w);
				entry.add(w.get(0));
			}

			// Acrescenta a sequência encontrada à saída original
			originalBytes.addAll(entry);

			// Adiciona uma nova entrada ao dicionário: w + primeiro byte da entrada
			// corrente
			ArrayList<Byte> newSequence = new ArrayList<>(w);
			newSequence.add(entry.get(0));
			if (dictionary.size() < (Math.pow(2, BITS_PER_INDEX) - 1))
				dictionary.add(newSequence);

			// Atualiza w para a próxima iteração
			w = entry;
		}

		// Converte a lista de bytes para um array de bytes
		byte[] outputBytes = new byte[originalBytes.size()];
		for (int i = 0; i < originalBytes.size(); i++)
			outputBytes[i] = originalBytes.get(i);

		return outputBytes;
	}
}
