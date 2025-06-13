package AEDs3.Compression;

import java.lang.reflect.InvocationTargetException;

import AEDs3.Compression.Compressors.*;

/**
 * Enumeração que representa os tipos de compressão disponíveis.
 */
public enum CompressionType {
	/**
	 * Copia dados sem comprimir.
	 */
	COPY("Copia dados sem comprimir", CopyCompressor.class, "pack"),

	/**
	 * Compressão Huffman.
	 */
	HUFFMAN("Compressão Huffman", HuffmanCompressor.class, "huffman"),

	/**
	 * Compressão LZW.
	 */
	LZW("Compressão LZW", LZWCompressor.class, "lzw");

	/**
	 * Descrição do tipo de compressão.
	 */
	private final String description;

	/**
	 * Classe que implementa a compressão.
	 */
	private final Class<? extends StreamCompressor> compressorClass;

	/**
	 * Extensão de arquivo esperada para esse tipo.
	 */
	private final String extension;

	/**
	 * Construtor para o tipo de compressão.
	 *
	 * @param description     Descrição do tipo de compressão.
	 * @param compressorClass Classe que implementa a compressão.
	 * @param extension       Extensão de arquivo esperada para esse tipo.
	 */
	CompressionType(String description, Class<? extends StreamCompressor> compressorClass, String extension) {
		this.description = description;
		this.compressorClass = compressorClass;
		this.extension = extension;
	}

	/**
	 * Obtém a descrição do tipo de compressão.
	 *
	 * @return A descrição do tipo de compressão.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Cria uma instância do compressor associado a este tipo de compressão.
	 *
	 * @return Uma instância de StreamCompressor.
	 */
	public StreamCompressor getCompressor() {
		try {
			return compressorClass.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException
				| NoSuchMethodException e) {
			throw new RuntimeException("Falha ao instanciar compressor.", e);
		}
	}

	/**
	 * Retorna a extensão de arquivo associada a esse tipo de compressão.
	 *
	 * @return A extensão de arquivo esperada.
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * Busca e retorna o CompressionType que possui a extensão informada.
	 *
	 * @param extension A extensão de arquivo para buscar.
	 * @return O enum CompressionType correspondente à extensão.
	 * @throws NoSuchFieldException Se a extensão não corresponder a nenhum tipo
	 *                              de compressão.
	 */
	public static CompressionType fromExtension(String extension) throws NoSuchFieldException {
		for (CompressionType type : CompressionType.values())
			if (type.getExtension().equalsIgnoreCase(extension))
				return type;
		throw new NoSuchFieldException("Nenhum tipo de compressão encontrado para a extensão: " + extension);
	}
}
