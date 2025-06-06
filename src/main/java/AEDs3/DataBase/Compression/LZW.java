package AEDs3.DataBase.Compression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Implementação do algoritmo de compressão LZW.
 * Realiza a compressão e descompressão de arquivos binários.
 */
public class LZW {
	public static final int BITS_PER_INDEX = 12; // Tamanho do índice em bits

	/**
	 * Método principal para execução do algoritmo LZW.
	 */
	public static void main(String[] args) {
		try {
			File file = new File(args[0]);
			FileInputStream fis = new FileInputStream(file);

			byte[] originalBytes = fis.readAllBytes();
			fis.close();

			System.out.println("Comprimindo!");

			// Codificação (Compressão)
			byte[] encodedBytes = encode(originalBytes);

			System.out.println("Salvando!");

			// Grava o arquivo comprimido
			FileOutputStream fos = new FileOutputStream("compressed_file.lzw");
			fos.write(encodedBytes);
			fos.close();

			System.out.println("Lendo o arquivo comprimido");

			// Lê o arquivo comprimido
			fis = new FileInputStream("compressed_file.lzw");
			byte[] encodedCopy = fis.readAllBytes();
			fis.close();

			System.out.println("Decodificando");

			// Decodificação (Descompressão)
			byte[] decodedBytes = decode(encodedCopy);

			System.out.println("Salvando o arquivo descomprimido");

			// Grava o arquivo descomprimido
			fos = new FileOutputStream("decompressed_file.bin");
			fos.write(decodedBytes);
			fos.close();

			// Estatísticas de compressão
			System.out.println("\nOriginal Size: " + originalBytes.length + " bytes");
			System.out.println("Compressed Size: " + encodedBytes.length + " bytes");
			System.out.println("Compression Efficiency: "
					+ (100 * (1 - (float) encodedBytes.length / (float) originalBytes.length)) + "%");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Método para codificar os bytes usando LZW.
	 *
	 * @param originalBytes sequência de bytes original.
	 * @return array de bytes codificados.
	 */
	public static byte[] encode(byte[] originalBytes) throws Exception {

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
			System.out.println("Iteração № " + i + "/" + originalBytes.length);

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
	 * Método para decodificar os bytes comprimidos usando LZW.
	 *
	 * @param encodedBytes sequência de bytes comprimida.
	 * @return array de bytes descomprimidos.
	 */
	public static byte[] decode(byte[] encodedBytes) throws Exception {

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
