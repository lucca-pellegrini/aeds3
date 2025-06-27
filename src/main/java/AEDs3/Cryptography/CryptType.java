package AEDs3.Cryptography;

/**
 * Enumeração que representa os tipos de criptografia disponíveis.
 */
public enum CryptType {
	/**
	 * Criptografia por cifra de Vigenère.
	 */
	VIGENERE("Criptografia por cifra de Vigenère", "vig"),

	/**
	 * Criptografia sistema criptográfico RSA.
	 */
	RSA("Criptografia sistema criptográfico RSA", "rsa");

	/**
	 * Descrição do tipo de criptografia.
	 */
	private final String description;

	/**
	 * Extensão de arquivo esperada para esse tipo.
	 */
	private final String extension;

	/**
	 * Construtor para o tipo de criptografia.
	 *
	 * @param description Descrição do tipo de criptografia.
	 * @param extension   Extensão de arquivo esperada para esse tipo.
	 */
	CryptType(String description, String extension) {
		this.description = description;
		this.extension = extension;
	}

	/**
	 * Obtém a descrição do tipo de criptografia.
	 *
	 * @return A descrição do tipo de criptografia.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Retorna a extensão de arquivo associada a esse tipo de criptografia.
	 *
	 * @return A extensão de arquivo esperada.
	 */
	public String getExtension() {
		return extension;
	}

	/**
	 * Busca e retorna o CryptType que possui a extensão informada.
	 *
	 * @param extension A extensão de arquivo para buscar.
	 * @return O enum CryptType correspondente à extensão.
	 * @throws NoSuchFieldException Se a extensão não corresponder a nenhum tipo
	 *                              de criptografia.
	 */
	public static CryptType fromExtension(String extension) throws NoSuchFieldException {
		for (CryptType type : CryptType.values())
			if (type.getExtension().equalsIgnoreCase(extension))
				return type;
		throw new NoSuchFieldException("Nenhum tipo de criptografia encontrado para a extensão: " + extension);
	}
}
