package AEDs3.DataBase.Compression;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Representa um nó na árvore de Huffman.
 * Cada nó armazena um byte, sua frequência de ocorrência e referências para os
 * nós filhos.
 * Implementa a interface Comparable para permitir a ordenação com base na
 * frequência.
 */
class HuffmanNode implements Comparable<HuffmanNode> {
	byte b; // Byte armazenado no nó
	int frequency; // Frequência de ocorrência do byte
	HuffmanNode left, right; // Referências para os nós filhos (esquerdo e direito)

	/**
	 * Construtor que inicializa o nó com um byte e sua frequência.
	 *
	 * @param b o byte a ser armazenado no nó
	 * @param f a frequência de ocorrência do byte
	 */
	public HuffmanNode(byte b, int f) {
		this.b = b;
		this.frequency = f;
		left = right = null;
	}

	/**
	 * Compara este nó com outro nó de Huffman com base na frequência.
	 *
	 * @param o o outro nó de Huffman a ser comparado
	 * @return um valor negativo se este nó tiver menor frequência, zero se igual,
	 *         ou positivo se maior
	 */
	@Override
	public int compareTo(HuffmanNode o) {
		return this.frequency - o.frequency;
	}
}

/**
 * Classe responsável pela compressão e descompressão de arquivos utilizando o
 * algoritmo de Huffman.
 * Fornece métodos para comprimir e descomprimir arquivos, além de gerar códigos
 * de Huffman.
 */
public class Huffman {
	/**
	 * Comprime um arquivo usando o algoritmo de Huffman.
	 *
	 * @param src o caminho do arquivo de origem a ser comprimido
	 * @param dst o caminho do arquivo de destino onde o arquivo comprimido será
	 *            salvo
	 * @throws IOException se ocorrer um erro de I/O durante o processo de
	 *                     compressão
	 */
	public static void compressFile(String src, String dst) throws IOException {
		// Lê o arquivo original
		FileInputStream fis = new FileInputStream(src);
		byte[] originalBytes = fis.readAllBytes();
		fis.close();

		// Gera os códigos de Huffman
		HashMap<Byte, String> codes = codeToBit(originalBytes);

		// Codifica os bytes originais usando os códigos de Huffman
		BitArray encodedSequence = new BitArray();
		int index = 0;
		for (byte b : originalBytes) {
			String code = codes.get(b);
			for (char c : code.toCharArray()) {
				if (c == '0') {
					encodedSequence.clear(index++);
				} else {
					encodedSequence.set(index++);
				}
			}
		}

		// Escreve os códigos de Huffman e os dados codificados no arquivo de saída
		FileOutputStream fos = new FileOutputStream(dst);
		DataOutputStream dos = new DataOutputStream(fos);

		// Escreve o tamanho do mapa de códigos
		dos.writeInt(codes.size());

		// Escreve o mapa de códigos de Huffman
		for (Map.Entry<Byte, String> entry : codes.entrySet()) {
			dos.writeByte(entry.getKey());
			dos.writeUTF(entry.getValue());
		}

		// Escreve o array de bytes codificados
		byte[] encodedBytes = encodedSequence.toByteArray();
		dos.writeInt(encodedBytes.length);
		dos.write(encodedBytes);

		dos.close();
		fos.close();
	}

	/**
	 * Descomprime um arquivo que foi comprimido usando o algoritmo de Huffman.
	 *
	 * @param src o caminho do arquivo comprimido de origem
	 * @param dst o caminho do arquivo de destino onde o arquivo descomprimido será
	 *            salvo
	 * @throws IOException se ocorrer um erro de I/O durante o processo de
	 *                     descompressão
	 */
	public static void decompressFile(String src, String dst) throws IOException {
		// Lê o arquivo comprimido
		FileInputStream fis = new FileInputStream(src);
		DataInputStream dis = new DataInputStream(fis);

		// Lê o tamanho do mapa de códigos
		int mapSize = dis.readInt();

		// Lê o mapa de códigos de Huffman
		HashMap<Byte, String> codes = new HashMap<>();
		for (int i = 0; i < mapSize; i++) {
			byte b = dis.readByte();
			String code = dis.readUTF();
			codes.put(b, code);
		}

		// Lê o comprimento do array de bytes codificados
		int encodedLength = dis.readInt();
		byte[] encodedBytes = new byte[encodedLength];
		dis.readFully(encodedBytes);

		dis.close();
		fis.close();

		// Decodifica o array de bytes codificados
		BitArray encodedSequence = new BitArray(encodedBytes);
		String bitString = encodedSequence.bitString();
		byte[] decodedBytes = decode(bitString, codes);

		// Escreve os bytes decodificados no arquivo de saída
		FileOutputStream fos = new FileOutputStream(dst);
		fos.write(decodedBytes);
		fos.close();
	}

	/**
	 * Gera a tabela de códigos de Huffman para cada byte da sequência fornecida.
	 *
	 * @param sequence a sequência de bytes a ser comprimida
	 * @return um HashMap onde a chave é o byte e o valor é seu código binário
	 *         correspondente
	 */
	public static HashMap<Byte, String> codeToBit(byte[] sequence) {
		Map<Byte, Integer> frequencyMap = new HashMap<>();
		for (byte c : sequence) {
			frequencyMap.put(c, frequencyMap.getOrDefault(c, 0) + 1);
		}

		PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
		for (Byte b : frequencyMap.keySet()) {
			pq.add(new HuffmanNode(b, frequencyMap.get(b)));
		}

		while (pq.size() > 1) {
			HuffmanNode left = pq.poll();
			HuffmanNode right = pq.poll();
			HuffmanNode father = new HuffmanNode((byte) 0, left.frequency + right.frequency);
			father.left = left;
			father.right = right;
			pq.add(father);
		}

		HuffmanNode root = pq.poll();
		HashMap<Byte, String> codes = new HashMap<>();
		generateCodes(root, "", codes);

		return codes;
	}

	/**
	 * Gera recursivamente os códigos binários a partir da árvore de Huffman.
	 *
	 * @param node  o nó atual da árvore de Huffman
	 * @param code  o código binário acumulado até o nó atual
	 * @param codes o mapa que armazena os códigos binários gerados
	 */
	private static void generateCodes(HuffmanNode node, String code, HashMap<Byte, String> codes) {
		if (node == null) {
			return;
		}
		if (node.left == null && node.right == null) {
			codes.put(node.b, code);
		}
		generateCodes(node.left, code + "0", codes);
		generateCodes(node.right, code + "1", codes);
	}

	/**
	 * Decodifica uma sequência de bits para o texto original utilizando a tabela de
	 * códigos de Huffman.
	 *
	 * @param codedSequence a sequência de bits codificada
	 * @param codes         o mapa de códigos de Huffman
	 * @return um array de bytes representando o texto original decodificado
	 */
	public static byte[] decode(String codedSequence, Map<Byte, String> codes) {
		ByteArrayOutputStream decodedSequence = new ByteArrayOutputStream();
		StringBuilder currentCode = new StringBuilder();
		Map<String, Byte> reverseCodes = new HashMap<>();

		for (Map.Entry<Byte, String> entry : codes.entrySet()) {
			reverseCodes.put(entry.getValue(), entry.getKey());
		}

		for (int i = 0; i < codedSequence.length(); i++) {
			currentCode.append(codedSequence.charAt(i));
			if (reverseCodes.containsKey(currentCode.toString())) {
				decodedSequence.write(reverseCodes.get(currentCode.toString()));
				currentCode.setLength(0);
			}
		}

		return decodedSequence.toByteArray();
	}

	/**
	 * Método principal que realiza a leitura, compressão e descompressão de um
	 * arquivo binário.
	 * Aceita o caminho do arquivo como argumento e executa o processo de compressão
	 * e descompressão.
	 *
	 * @param args argumentos de linha de comando, onde o primeiro argumento é o
	 *             caminho do arquivo
	 */
	public static void main(String[] args) {
		String caminhoArquivo = args[0];

		try {
			byte[] dados = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(caminhoArquivo));
			System.out.println("Arquivo original lido: " + dados.length + " bytes");

			HashMap<Byte, String> codigos = codeToBit(dados);
			BitArray sequenciaCodificada = new BitArray();
			int i = 0;
			for (byte b : dados) {
				String codigo = codigos.get(b);
				if (codigo == null)
					continue;
				for (char c : codigo.toCharArray()) {
					if (c == '0')
						sequenciaCodificada.clear(i++);
					else
						sequenciaCodificada.set(i++);
				}
			}

			byte[] vb = sequenciaCodificada.toByteArray();
			System.out.println("Tamanho compactado: " + vb.length + " bytes");

			BitArray sequenciaCodificada2 = new BitArray(vb);
			String bitString = sequenciaCodificada2.bitString();
			byte[] dadosDecodificados = decode(bitString, codigos);

			java.nio.file.Files.write(java.nio.file.Paths.get("arquivo_grande_decodificado.bin"), dadosDecodificados);
			System.out.println("Arquivo decodificado salvo como 'arquivo_grande_decodificado.bin'");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
