package AEDs3.DataBase.Compression;

/**
 * Enumeração que representa os tipos de compressão disponíveis.
 */
public enum CompressionType {
	/**
	 * Nenhuma compressão. Simplesmente copia as streams.
	 */
	PACK("Copia dados sem comprimir"),

	/**
	 * Compressão usando o algoritmo de Huffman.
	 */
	HUFFMAN("Compressão Huffman"),

	/**
	 * Compressão usando o algoritmo LZW.
	 */
	LZW("Compressão LZW");

	private final String description;

	/**
	 * Construtor para o tipo de compressão.
	 *
	 * @param description Descrição do tipo de compressão.
	 */
	CompressionType(String description) {
		this.description = description;
	}

	/**
	 * Obtém a descrição do tipo de compressão.
	 *
	 * @return A descrição do tipo de compressão.
	 */
	public String getDescription() {
		return description;
	}
}
