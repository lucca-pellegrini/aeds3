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
 * Classe que representa um nó da árvore de Huffman.
 * Cada nó armazena um byte, sua frequência e referências para os nós filhos.
 */
class HuffmanNode implements Comparable<HuffmanNode> {
	byte b; // Byte armazenado no nó
	int frequency; // Frequência de ocorrência do byte
	HuffmanNode left, right; // Referências para os nós filhos (esquerdo e direito)

	// Construtor para inicializar o nó com um byte e sua frequência
	public HuffmanNode(byte b, int f) {
		this.b = b;
		this.frequency = f;
		left = right = null;
	}

	// Método de comparação para ordenar os nós na fila de prioridade (com base na
	// frequência)
	@Override
	public int compareTo(HuffmanNode o) {
		return this.frequency - o.frequency;
	}
}

/**
 * Classe principal responsável pela compressão e descompressão usando o
 * algoritmo de Huffman.
 */
public class Huffman {
	public static void compressFile(String src, String dst) throws IOException {
		// Read the original file
		FileInputStream fis = new FileInputStream(src);
		byte[] originalBytes = fis.readAllBytes();
		fis.close();

		// Generate Huffman codes
		HashMap<Byte, String> codes = codeToBit(originalBytes);

		// Encode the original bytes using Huffman codes
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

		// Write the Huffman codes and encoded data to the output file
		FileOutputStream fos = new FileOutputStream(dst);
		DataOutputStream dos = new DataOutputStream(fos);

		// Write the size of the map
		dos.writeInt(codes.size());

		// Write the Huffman code map
		for (Map.Entry<Byte, String> entry : codes.entrySet()) {
			dos.writeByte(entry.getKey());
			dos.writeUTF(entry.getValue());
		}

		// Write the encoded byte array
		byte[] encodedBytes = encodedSequence.toByteArray();
		dos.writeInt(encodedBytes.length);
		dos.write(encodedBytes);

		dos.close();
		fos.close();
	}

	public static void decompressFile(String src, String dst) throws IOException {
		// Read the compressed file
		FileInputStream fis = new FileInputStream(src);
		DataInputStream dis = new DataInputStream(fis);

		// Read the size of the map
		int mapSize = dis.readInt();

		// Read the Huffman code map
		HashMap<Byte, String> codes = new HashMap<>();
		for (int i = 0; i < mapSize; i++) {
			byte b = dis.readByte();
			String code = dis.readUTF();
			codes.put(b, code);
		}

		// Read the length of the encoded byte array
		int encodedLength = dis.readInt();
		byte[] encodedBytes = new byte[encodedLength];
		dis.readFully(encodedBytes);

		dis.close();
		fis.close();

		// Decode the encoded byte array
		BitArray encodedSequence = new BitArray(encodedBytes);
		String bitString = encodedSequence.bitString();
		byte[] decodedBytes = decode(bitString, codes);

		// Write the decoded bytes to the output file
		FileOutputStream fos = new FileOutputStream(dst);
		fos.write(decodedBytes);
		fos.close();
	}

	/**
	 * Gera a tabela de códigos de Huffman para cada byte da sequência.
	 *
	 * @param sequence sequência de bytes a ser comprimida.
	 * @return um HashMap com o byte como chave e seu código binário como valor.
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
	 * Método recursivo para gerar os códigos binários a partir da árvore de
	 * Huffman.
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
	 * Decodifica uma sequência de bits para o texto original usando a tabela de
	 * códigos.
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
	 * Método principal para leitura, compressão, e descompressão de um arquivo
	 * binário.
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
