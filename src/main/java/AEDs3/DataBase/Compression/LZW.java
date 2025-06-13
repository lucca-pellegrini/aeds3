package AEDs3.DataBase.Compression;

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
public class LZW {
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
	public static void compress(InputStream in, OutputStream out) throws IOException {
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
	public static void decompress(InputStream in, OutputStream out) throws IOException {
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

	/**
	 * Classe auxiliar para escrita de bits em uma stream de saída.
	 * Esta classe acumula bits e escreve bytes completos à medida que são formados.
	 */
	private static class BitOutputStream {
		private final OutputStream out;
		private int currentByte;
		private int numBitsFilled;

		public BitOutputStream(OutputStream out) {
			this.out = out;
			this.currentByte = 0;
			this.numBitsFilled = 0;
		}

		/**
		 * Escreve 'numBits' bits do valor 'value' na stream, do bit mais significativo
		 * ao menos significativo.
		 *
		 * @param numBits Número de bits a escrever.
		 * @param value   Valor que contém os bits.
		 * @throws IOException Se ocorrer um erro de I/O.
		 */
		public void write(int numBits, int value) throws IOException {
			for (int i = numBits - 1; i >= 0; i--) {
				int bit = (value >> i) & 1;
				currentByte = (currentByte << 1) | bit;
				numBitsFilled += 1;
				if (numBitsFilled == 8) {
					out.write(currentByte);
					numBitsFilled = 0;
					currentByte = 0;
				}
			}
		}

		/**
		 * Finaliza a escrita, preenchendo com zeros os bits não utilizados e
		 * realizando o flush na stream subjacente.
		 *
		 * @throws IOException Se ocorrer um erro de I/O.
		 */
		public void flush() throws IOException {
			if (numBitsFilled > 0) {
				currentByte <<= (8 - numBitsFilled);
				out.write(currentByte);
				numBitsFilled = 0;
				currentByte = 0;
			}
			out.flush();
		}
	}

	/**
	 * Classe auxiliar para leitura de bits de uma stream de entrada.
	 * Permite ler exatamente o número de bits solicitados, carregando apenas
	 * um byte de cada vez em memória.
	 */
	private static class BitInputStream {
		private final InputStream in;
		private int currentByte;
		private int numBitsRemaining;

		public BitInputStream(InputStream in) {
			this.in = in;
			this.currentByte = 0;
			this.numBitsRemaining = 0;
		}

		/**
		 * Lê 'numBits' bits da stream e retorna o valor correspondente.
		 * Retorna -1 se não houver bits suficientes (fim da stream).
		 *
		 * @param numBits Número de bits a serem lidos.
		 * @return Valor lido ou -1 se o fim da stream for atingido.
		 * @throws IOException Se ocorrer um erro de I/O.
		 */
		public int read(int numBits) throws IOException {
			int result = 0;
			for (int i = 0; i < numBits; i++) {
				int bit = readBit();
				if (bit == -1)
					return -1;
				result = (result << 1) | bit;
			}
			return result;
		}

		/**
		 * Lê um único bit da stream.
		 *
		 * @return O bit lido (0 ou 1) ou -1 se o fim da stream for atingido.
		 * @throws IOException Se ocorrer um erro de I/O.
		 */
		private int readBit() throws IOException {
			if (numBitsRemaining == 0) {
				currentByte = in.read();
				if (currentByte == -1)
					return -1;
				numBitsRemaining = 8;
			}
			numBitsRemaining -= 1;
			return (currentByte >> numBitsRemaining) & 1;
		}
	}
}
