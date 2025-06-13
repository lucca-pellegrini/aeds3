package AEDs3.DataBase.Compression.Compressors;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Classe responsável pela compressão e descompressão de dados utilizando o
 * algoritmo de Huffman. Nesta versão, os métodos compress e decompress operam
 * sobre streams, lendo e escrevendo bits de forma incremental para minimizar o
 * uso de memória RAM.
 */
public class HuffmanCompressor implements StreamCompressor {

	/**
	 * Comprime os dados lidos de um InputStream utilizando o algoritmo de Huffman e
	 * os escreve no OutputStream.
	 *
	 * @param in  Stream de entrada com os dados originais.
	 * @param out Stream de saída onde os dados comprimidos serão escritos.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	@Override
	public void compress(InputStream in, OutputStream out) throws IOException {
		// Primeira passagem: contar frequências de cada byte (vetor de tamanho fixo:
		// 256 posições)
		int[] frequencies = new int[256];
		InputStream encodingStream; // Stream que será usada na segunda passagem (para efetuar a codificação)

		if (in.markSupported()) {
			// Se o InputStream suportar reset, marcamos o início, lemos e resetamos
			in.mark(Integer.MAX_VALUE);
			int b;
			while ((b = in.read()) != -1) {
				frequencies[b & 0xFF]++;
			}
			in.reset();
			encodingStream = in;
		} else {
			// Caso não suporte reset, gravamos os dados em um arquivo temporário
			File tempFile = File.createTempFile("huffman", ".tmp");
			tempFile.deleteOnExit();
			try (OutputStream tempOut = new FileOutputStream(tempFile)) {
				int b;
				while ((b = in.read()) != -1) {
					frequencies[b & 0xFF]++;
					tempOut.write(b);
				}
			}
			encodingStream = new FileInputStream(tempFile);
		}

		// Construir a árvore de Huffman com os bytes cuja frequência é > 0
		PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
		for (int i = 0; i < 256; i++) {
			if (frequencies[i] > 0) {
				pq.add(new HuffmanNode((byte) i, frequencies[i]));
			}
		}
		// Se nenhum byte foi lido, não há nada a comprimir
		if (pq.isEmpty()) {
			return;
		}
		// Se houver apenas um símbolo, crie uma árvore “dupla” para permitir um código
		// (por exemplo, "0")
		if (pq.size() == 1) {
			pq.add(new HuffmanNode((byte) 0, 0));
		}
		while (pq.size() > 1) {
			HuffmanNode left = pq.poll();
			HuffmanNode right = pq.poll();
			HuffmanNode parent = new HuffmanNode((byte) 0, left.frequency + right.frequency);
			parent.left = left;
			parent.right = right;
			pq.add(parent);
		}
		HuffmanNode root = pq.poll();

		// Gera a tabela de códigos de Huffman (Map de byte para String)
		HashMap<Byte, String> codes = new HashMap<>();
		generateCodes(root, "", codes);

		// Calcula o total de bits que serão escritos (somando para cada byte:
		// frequência * tamanho do código)
		long totalBits = 0;
		for (int i = 0; i < 256; i++) {
			if (frequencies[i] > 0) {
				String code = codes.get((byte) i);
				totalBits += (long) frequencies[i] * code.length();
			}
		}

		/*
		 * Escreve o header utilizando DataOutputStream.
		 * O header conterá:
		 * - Número de entradas do mapa de códigos
		 * - Para cada entrada: byte e String (UTF) com o código
		 * - O total de bits codificados (long)
		 */
		DataOutputStream dos = new DataOutputStream(out);
		dos.writeInt(codes.size());
		for (Map.Entry<Byte, String> entry : codes.entrySet()) {
			dos.writeByte(entry.getKey());
			dos.writeUTF(entry.getValue());
		}
		dos.writeLong(totalBits);

		// Faz a segunda passagem: lê os bytes novamente e escreve seus códigos
		// utilizando BitOutputStream.
		BitOutputStream bitOut = new BitOutputStream(out);
		int byteRead;
		while ((byteRead = encodingStream.read()) != -1) {
			byte b = (byte) byteRead;
			String code = codes.get(b);
			for (int i = 0; i < code.length(); i++) {
				char c = code.charAt(i);
				// Escreve 1 bit por vez (0 ou 1)
				if (c == '1') {
					bitOut.write(1, 1);
				} else { // assume '0'
					bitOut.write(1, 0);
				}
			}
		}
		bitOut.flush();
		dos.flush();
		encodingStream.close();
	}

	/**
	 * Descomprime os dados lidos do InputStream que foram comprimidos com Huffman e
	 * escreve o resultado no OutputStream.
	 *
	 * O método lê o header (mapa de códigos e total de bits codificados) e
	 * reconstrói a árvore de Huffman para proceder à decodificação bit a bit.
	 *
	 * @param in  Stream de entrada com os dados comprimidos.
	 * @param out Stream de saída onde os dados descomprimidos serão escritos.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	@Override
	public void decompress(InputStream in, OutputStream out) throws IOException {
		DataInputStream dis = new DataInputStream(in);
		// Lê o header: número de entradas na tabela de códigos
		int mapSize = dis.readInt();
		HashMap<Byte, String> codes = new HashMap<>();
		for (int i = 0; i < mapSize; i++) {
			byte b = dis.readByte();
			String code = dis.readUTF();
			codes.put(b, code);
		}
		long totalBits = dis.readLong();

		// Reconstrói a árvore de decodificação a partir do mapa de códigos
		DecodeNode root = buildDecodingTree(codes);

		// Agora, decodifica os bits restantes utilizando BitInputStream.
		// Como o header já foi lido do stream, basta criar o BitInputStream sobre o
		// DataInputStream "dis".
		BitInputStream bitIn = new BitInputStream(dis);
		long bitsRead = 0;
		DecodeNode current = root;
		while (bitsRead < totalBits) {
			int bit = bitIn.read(1);
			if (bit == -1) {
				break; // Não deveria ocorrer antes de totalBits
			}
			bitsRead++;
			current = (bit == 0) ? current.left : current.right;
			if (current.isLeaf()) {
				out.write(current.b);
				current = root;
			}
		}
		out.flush();
	}

	/**
	 * Método auxiliar recursivo para gerar a tabela de códigos de Huffman.
	 *
	 * @param node  Nó atual na árvore de Huffman.
	 * @param code  Código acumulado até o momento.
	 * @param codes Mapa que armazena os códigos resultantes.
	 */
	private static void generateCodes(HuffmanNode node, String code, HashMap<Byte, String> codes) {
		if (node == null) {
			return;
		}
		// Se é folha, insere no mapa (se o código estiver vazio – ocorrendo quando só
		// há um símbolo – define "0")
		if (node.left == null && node.right == null) {
			codes.put(node.b, code.length() > 0 ? code : "0");
		} else {
			generateCodes(node.left, code + "0", codes);
			generateCodes(node.right, code + "1", codes);
		}
	}

	/**
	 * Reconstrói a árvore de decodificação de Huffman a partir da tabela de
	 * códigos.
	 *
	 * @param codes Tabela de códigos (Map de byte para String).
	 * @return A raiz da árvore de decodificação.
	 */
	private static DecodeNode buildDecodingTree(Map<Byte, String> codes) {
		DecodeNode root = new DecodeNode();
		for (Map.Entry<Byte, String> entry : codes.entrySet()) {
			byte b = entry.getKey();
			String code = entry.getValue();
			DecodeNode current = root;
			for (int i = 0; i < code.length(); i++) {
				char c = code.charAt(i);
				if (c == '0') {
					if (current.left == null) {
						current.left = new DecodeNode();
					}
					current = current.left;
				} else { // c == '1'
					if (current.right == null) {
						current.right = new DecodeNode();
					}
					current = current.right;
				}
			}
			current.b = b;
			current.setLeaf(); // Marca como nó folha
		}
		return root;
	}

	/**
	 * Classe auxiliar representando um nó na árvore de Huffman (para compressão).
	 */
	private static class HuffmanNode implements Comparable<HuffmanNode> {
		/**
		 * Byte representado pelo nó.
		 */
		byte b;
		/**
		 * Frequência do byte.
		 */
		int frequency;
		/**
		 * Filho esquerdo na árvore de Huffman.
		 */
		HuffmanNode left;
		/**
		 * Filho direito na árvore de Huffman.
		 */
		HuffmanNode right;

		/**
		 * Construtor para criar um nó de Huffman com um byte e sua frequência.
		 *
		 * @param b         Byte representado pelo nó.
		 * @param frequency Frequência do byte.
		 */
		public HuffmanNode(byte b, int frequency) {
			this.b = b;
			this.frequency = frequency;
		}

		/**
		 * Compara este nó de Huffman com outro nó com base na frequência.
		 *
		 * @param o Outro nó de Huffman a ser comparado.
		 * @return Diferença entre as frequências dos nós.
		 */
		@Override
		public int compareTo(HuffmanNode o) {
			return this.frequency - o.frequency;
		}
	}

	/**
	 * Classe auxiliar para decodificação (árvore de Huffman simplificada).
	 */
	private static class DecodeNode {
		/**
		 * Byte representado pelo nó.
		 */
		byte b;
		/**
		 * Filho esquerdo na árvore de decodificação.
		 */
		DecodeNode left;
		/**
		 * Filho direito na árvore de decodificação.
		 */
		DecodeNode right;

		/**
		 * Marca este nó como folha, removendo quaisquer filhos.
		 */
		public void setLeaf() {
			left = null;
			right = null;
		}

		/**
		 * Verifica se o nó é uma folha (não possui filhos).
		 *
		 * @return true se o nó for uma folha, caso contrário, false.
		 */
		public boolean isLeaf() {
			return left == null && right == null;
		}
	}
}
