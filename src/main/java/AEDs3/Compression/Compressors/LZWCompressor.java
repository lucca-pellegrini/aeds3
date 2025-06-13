package AEDs3.Compression.Compressors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Esta classe implementa o algoritmo de compressão LZW para arquivos binários.
 * Nesta versão, os métodos compress e decompress operam sobre streams, lendo e
 * escrevendo bits de forma incremental para minimizar o uso de memória.
 */
public class LZWCompressor implements StreamCompressor {
	/**
	 * Tamanho do índice em bits utilizado no algoritmo LZW.
	 */
	public static final int BITS_PER_INDEX = 12; // Tamanho do índice em bits

	/**
	 * Comprime os dados lidos do InputStream utilizando o algoritmo LZW e os
	 * escreve no OutputStream.
	 * As streams são processadas de forma incremental, evitando a carga de todos os
	 * dados em memória.
	 *
	 * @param in  Stream de entrada com os dados originais.
	 * @param out Stream de saída onde os dados comprimidos serão escritos.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	@Override
	public void compress(InputStream in, OutputStream out) throws IOException {
		// Inicializa o dicionário com todos os bytes possíveis (-128 a 127)
		Map<ByteSequence, Integer> dictionary = new HashMap<>();
		for (int i = -128; i < 128; i++) {
			ByteSequence seq = new ByteSequence((byte) i);
			dictionary.put(seq, dictionary.size());
		}
		int maxDictSize = (1 << BITS_PER_INDEX) - 1;

		BitOutputStream bitOut = new BitOutputStream(out);

		// Leitura do primeiro byte para iniciar o algoritmo
		int next = in.read();
		if (next == -1) { // arquivo vazio
			bitOut.flush();
			return;
		}
		ByteSequence w = new ByteSequence((byte) next);

		int b;
		while ((b = in.read()) != -1) {
			byte currentByte = (byte) b;
			ByteSequence wc = w.extend(currentByte);
			if (dictionary.containsKey(wc)) {
				w = wc;
			} else {
				int code = dictionary.get(w);
				bitOut.write(BITS_PER_INDEX, code);
				if (dictionary.size() < maxDictSize) {
					dictionary.put(wc, dictionary.size());
				}
				w = new ByteSequence(currentByte);
			}
		}
		if (w != null) {
			int code = dictionary.get(w);
			bitOut.write(BITS_PER_INDEX, code);
		}
		bitOut.flush();
	}

	/**
	 * Descomprime os dados lidos do InputStream utilizando o algoritmo LZW e os
	 * escreve no OutputStream. A leitura dos códigos é feita de forma incremental,
	 * de modo que apenas os bits necessários sejam carregados em memória a cada
	 * momento.
	 *
	 * @param in  Stream de entrada com os dados comprimidos.
	 * @param out Stream de saída onde os dados descomprimidos serão escritos.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	@Override
	public void decompress(InputStream in, OutputStream out) throws IOException {
		BitInputStream bitIn = new BitInputStream(in);

		// Inicializa o dicionário com todos os bytes possíveis (-128 a 127)
		ArrayList<ArrayList<Byte>> dictionary = new ArrayList<>();
		for (int i = -128; i < 128; i++) {
			ArrayList<Byte> seq = new ArrayList<>();
			seq.add((byte) i);
			dictionary.add(seq);
		}

		int maxDictSize = (1 << BITS_PER_INDEX) - 1;

		int firstCode = bitIn.read(BITS_PER_INDEX);
		if (firstCode == -1)
			return;
		ArrayList<Byte> w = new ArrayList<>(dictionary.get(firstCode));

		// Escreve a sequência correspondente ao primeiro código
		for (byte byteVal : w)
			out.write(byteVal);

		int code;
		while ((code = bitIn.read(BITS_PER_INDEX)) != -1) {
			ArrayList<Byte> entry;
			if (code < dictionary.size()) {
				entry = new ArrayList<>(dictionary.get(code));
			} else {
				// Caso especial: código ainda não inserido no dicionário
				entry = new ArrayList<>(w);
				entry.add(w.get(0));
			}
			// Escreve os bytes da sequência decodificada
			for (byte byteVal : entry)
				out.write(byteVal);

			ArrayList<Byte> newSequence = new ArrayList<>(w);
			newSequence.add(entry.get(0));
			if (dictionary.size() < maxDictSize)
				dictionary.add(newSequence);
			w = entry;
		}
		out.flush();
	}

	/**
	 * Classe auxiliar para representar uma sequência imutável de bytes.
	 * Usada como chave no dicionário de codificação.
	 */
	private static class ByteSequence {
		/**
		 * Sequência imutável de bytes.
		 */
		private final ArrayList<Byte> sequence;

		/**
		 * Construtor que cria uma sequência com um único byte.
		 *
		 * @param b Byte inicial da sequência.
		 */
		public ByteSequence(byte b) {
			sequence = new ArrayList<>();
			sequence.add(b);
		}

		/**
		 * Construtor que cria uma sequência a partir de uma lista de bytes.
		 *
		 * @param seq Lista de bytes para inicializar a sequência.
		 */
		public ByteSequence(ArrayList<Byte> seq) {
			sequence = new ArrayList<>(seq);
		}

		/**
		 * Retorna um novo ByteSequence que é esta sequência estendida pelo byte b.
		 *
		 * @param b Byte a ser adicionado à sequência.
		 * @return Novo ByteSequence com o byte adicionado.
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
