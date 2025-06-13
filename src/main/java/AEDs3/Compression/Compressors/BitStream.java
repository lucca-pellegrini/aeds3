package AEDs3.Compression.Compressors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Classe auxiliar para escrita de bits em uma stream de saída.
 * Esta classe acumula bits e escreve bytes completos à medida que são formados.
 */
class BitOutputStream {
	/**
	 * Stream de saída para escrita de bits.
	 */
	private final OutputStream out;

	/**
	 * Byte atual em construção.
	 */
	private int currentByte;

	/**
	 * Número de bits preenchidos no byte atual.
	 */
	private int numBitsFilled;

	/**
	 * Construtor para inicializar o BitOutputStream com uma stream de saída.
	 *
	 * @param out Stream de saída para escrita de bits.
	 */
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
class BitInputStream {
	/**
	 * Stream de entrada para leitura de bits.
	 */
	private final InputStream in;

	/**
	 * Byte atual em leitura.
	 */
	private int currentByte;

	/**
	 * Número de bits restantes no byte atual.
	 */
	private int numBitsRemaining;

	/**
	 * Construtor para inicializar o BitInputStream com uma stream de entrada.
	 *
	 * @param in Stream de entrada para leitura de bits.
	 */
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
