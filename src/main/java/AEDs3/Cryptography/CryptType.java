package AEDs3.Cryptography;

import java.lang.reflect.InvocationTargetException;

/**
 * Enumeração que representa os tipos de criptografia disponíveis.
 */
public enum CryptType {
	/**
	 * Criptografia por cifra de Vigenère.
	 */
	VIGENERE("Criptografia por cifra de Vigenère", Vigenere.class, "vig"),

	/**
	 * Criptografia sistema criptográfico RSA.
	 */
	RSA("Criptografia sistema criptográfico RSA", RSAHybridCryptography.class, "rsa");

	/**
	 * Descrição do tipo de criptografia.
	 */
	private final String description;

	/**
	 * Classe que implementa a criptografia associada.
	 */
	private final Class<? extends EncryptionSystem> encryptionClass;

	/**
	 * Extensão de arquivo esperada para esse tipo.
	 */
	private final String extension;

	/**
	 * Construtor para o tipo de criptografia.
	 *
	 * @param description     Descrição do tipo de criptografia.
	 * @param encryptionClass Classe de interface {@link EncryptionSystem} que
	 *                        implementa a compressão e a descompressão associadas.
	 * @param extension       Extensão de arquivo esperada para esse tipo.
	 */
	CryptType(String description, Class<? extends EncryptionSystem> encryptionClass, String extension) {
		this.description = description;
		this.encryptionClass = encryptionClass;
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
	 * Cria uma instância do sistema criptográfico associado a este tipo de
	 * criptografia.
	 *
	 * @return Uma instância de EncryptionSystem.
	 */
	public EncryptionSystem getEncryptionSystem() {
		try {
			return encryptionClass.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException
				| NoSuchMethodException e) {
			throw new RuntimeException("Falha ao instanciar sistema.", e);
		}
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
