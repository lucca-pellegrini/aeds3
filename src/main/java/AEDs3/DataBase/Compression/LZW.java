package AEDs3.DataBase.Compression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Esta classe implementa o algoritmo de compressão LZW, que é utilizado para
 * comprimir e descomprimir arquivos binários. O algoritmo LZW é eficiente para
 * compressão de dados que possuem padrões repetitivos.
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
	 * @param dst Caminho do arquivo de destino onde o arquivo comprimido será
	 *            salvo.
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
	 * @param dst Caminho do arquivo de destino onde o arquivo descomprimido será
	 *            salvo.
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
		// Usamos um HashMap para fazer buscas rápidas no dicionário.
		Map<ByteSequence, Integer> dictionary = new HashMap<>();

		// Inicialização do dicionário com todos os bytes possíveis (-128 a 127)
		for (int i = -128; i < 128; i++) {
			ByteSequence seq = new ByteSequence((byte) i);
			dictionary.put(seq, dictionary.size());
		}
		int maxDictSize = (1 << BITS_PER_INDEX) - 1;

		// ArrayList para armazenar os códigos de saída.
		ArrayList<Integer> output = new ArrayList<>();

		// O algoritmo LZW padrão.
		ByteSequence w = null;
		for (byte b : originalBytes) {
			if (w == null) {
				w = new ByteSequence(b);
				continue;
			}
			ByteSequence wc = w.extend(b);
			if (dictionary.containsKey(wc)) {
				w = wc;
			} else {
				output.add(dictionary.get(w));
				if (dictionary.size() < maxDictSize)
					dictionary.put(wc, dictionary.size());

				w = new ByteSequence(b);
			}
		}

		if (w != null)
			output.add(dictionary.get(w));

		// Converte os índices para bits e armazena em um BitArray.
		BitArray bits = new BitArray(output.size() * BITS_PER_INDEX);
		int l = output.size() * BITS_PER_INDEX - 1;
		for (int i = output.size() - 1; i >= 0; i--) {
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
		// Inicializa o BitArray a partir do array de bytes comprimido.
		BitArray bits = new BitArray(encodedBytes);
		ArrayList<Integer> indices = new ArrayList<>();

		// Recupera os índices a partir do BitArray.
		int k = 0;
		for (int i = 0; i < bits.length() / BITS_PER_INDEX; i++) {
			int n = 0;
			for (int j = 0; j < BITS_PER_INDEX; j++)
				n = n * 2 + (bits.get(k++) ? 1 : 0);
			indices.add(n);
		}

		// Inicializa o dicionário com todos os bytes possíveis (-128 a 127).
		ArrayList<ArrayList<Byte>> dictionary = new ArrayList<>();
		for (int j = -128; j < 128; j++) {
			ArrayList<Byte> byteSequence = new ArrayList<>();
			byteSequence.add((byte) j);
			dictionary.add(byteSequence);
		}

		ArrayList<Byte> originalBytes = new ArrayList<>();

		// Decodifica os índices para a sequência de bytes original.
		int firstIndex = indices.get(0);
		ArrayList<Byte> w = new ArrayList<>(dictionary.get(firstIndex));
		originalBytes.addAll(w);

		for (int i = 1; i < indices.size(); i++) {
			int index = indices.get(i);
			ArrayList<Byte> entry;
			if (index < dictionary.size()) {
				entry = new ArrayList<>(dictionary.get(index));
			} else {
				entry = new ArrayList<>(w);
				entry.add(w.get(0));
			}
			originalBytes.addAll(entry);

			ArrayList<Byte> newSequence = new ArrayList<>(w);
			newSequence.add(entry.get(0));
			if (dictionary.size() < ((1 << BITS_PER_INDEX) - 1))
				dictionary.add(newSequence);

			w = entry;
		}

		byte[] outputBytes = new byte[originalBytes.size()];
		for (int i = 0; i < originalBytes.size(); i++)
			outputBytes[i] = originalBytes.get(i);

		return outputBytes;
	}

	/**
	 * Classe auxiliar para representar uma sequência imutável de bytes.
	 * Usada como chave no dicionário de codificação.
	 */
	private static class ByteSequence {
		private final ArrayList<Byte> sequence;

		public ByteSequence(byte b) {
			sequence = new ArrayList<>();
			sequence.add(b);
		}

		public ByteSequence(ArrayList<Byte> seq) {
			sequence = new ArrayList<>(seq);
		}

		/**
		 * Retorna um novo ByteSequence que é esta sequência estendida pelo byte b.
		 */
		public ByteSequence extend(byte b) {
			ArrayList<Byte> newSeq = new ArrayList<>(this.sequence);
			newSeq.add(b);
			return new ByteSequence(newSeq);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ByteSequence that = (ByteSequence) o;
			return sequence.equals(that.sequence);
		}

		@Override
		public int hashCode() {
			return sequence.hashCode();
		}
	}
}
