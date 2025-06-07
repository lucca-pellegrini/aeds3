package AEDs3.DataBase.Compression;

import java.util.BitSet;

/**
 * A classe BitArray representa um vetor de bits utilizando a classe BitSet.
 * Oferece métodos para manipulação de bits individuais e conversão para array
 * de bytes.
 */
class BitArray {

	/** Representa o vetor de bits principal */
	private BitSet bitSet;

	/**
	 * Construtor padrão: inicializa o BitSet e define o primeiro bit.
	 */
	public BitArray() {
		bitSet = new BitSet();
		bitSet.set(0);
	}

	/**
	 * Construtor com tamanho especificado: inicializa o BitSet e define o último
	 * bit.
	 *
	 * @param n o tamanho inicial do BitSet
	 */
	public BitArray(int n) {
		bitSet = new BitSet(n);
		bitSet.set(n);
	}

	/**
	 * Construtor que inicializa o BitSet a partir de um vetor de bytes.
	 *
	 * @param byteArray o vetor de bytes para inicializar o BitSet
	 */
	public BitArray(byte[] byteArray) {
		bitSet = BitSet.valueOf(byteArray);
	}

	/**
	 * Retorna o BitSet como um array de bytes.
	 *
	 * @return o array de bytes representando o BitSet
	 */
	public byte[] toByteArray() {
		return bitSet.toByteArray();
	}

	/**
	 * Define um bit na posição 'i' como 1 (true).
	 *
	 * @param i a posição do bit a ser definido
	 */
	public void set(int i) {
		if (i >= bitSet.length() - 1) {
			bitSet.clear(bitSet.length() - 1);
			bitSet.set(i + 1);
		}
		bitSet.set(i);
	}

	/**
	 * Define um bit na posição 'i' como 0 (false).
	 *
	 * @param i a posição do bit a ser limpo
	 */
	public void clear(int i) {
		if (i >= bitSet.length() - 1) {
			bitSet.clear(bitSet.length() - 1);
			bitSet.set(i + 1);
		}
		bitSet.clear(i);
	}

	/**
	 * Retorna o valor do bit na posição 'i'.
	 *
	 * @param i a posição do bit a ser retornado
	 * @return true se o bit na posição 'i' estiver definido, caso contrário false
	 */
	public boolean get(int i) {
		return bitSet.get(i);
	}

	/**
	 * Retorna o comprimento do BitSet (número de bits utilizados).
	 *
	 * @return o comprimento do BitSet
	 */
	public int length() {
		return bitSet.length() - 1;
	}

	/**
	 * Retorna o tamanho do BitSet (capacidade alocada internamente).
	 *
	 * @return o tamanho do BitSet
	 */
	public int size() {
		return bitSet.size();
	}

	/**
	 * Retorna uma representação em string dos bits (0 e 1).
	 *
	 * @return a representação em string dos bits
	 */
	public String bitString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < this.length(); i++) {
			sb.append(this.get(i) ? '1' : '0');
		}
		return sb.toString();
	}
}
