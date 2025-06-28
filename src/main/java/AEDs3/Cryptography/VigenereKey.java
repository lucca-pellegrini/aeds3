package AEDs3.Cryptography;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * A classe {@code VigenereKey} representa uma chave para o algoritmo de cifra
 * de Vigenère. Esta classe implementa a interface {@code Key} e fornece métodos
 * para obter a chave codificada e o formato da chave.
 */
public class VigenereKey implements PublicKey, PrivateKey {
	private byte[] keyBytes;

	/**
	 * Construtor que inicializa a chave Vigenère a partir de uma string ASCII.
	 *
	 * @param keyASCII a chave em formato ASCII.
	 * @throws IllegalArgumentException se a chave contiver caracteres não ASCII.
	 */
	public VigenereKey(String keyASCII) {
		validateKey(keyASCII);
		this.keyBytes = keyASCII.getBytes(StandardCharsets.US_ASCII);
	}

	/**
	 * Retorna o nome do algoritmo de cifra.
	 *
	 * @return o nome do algoritmo, que é "Vigenère".
	 */
	public String getAlgorithm() {
		return "Vigenère";
	}

	/**
	 * Retorna a chave codificada em bytes.
	 *
	 * @return a chave codificada como um array de bytes.
	 */
	public byte[] getEncoded() {
		return this.keyBytes;
	}

	/**
	 * Retorna o formato da chave.
	 *
	 * @return o formato da chave, que é "ASCII".
	 */
	public String getFormat() {
		return "ASCII";
	}

	/**
	 * Valida se a chave fornecida contém apenas caracteres ASCII.
	 *
	 * @param key a chave a ser validada.
	 * @throws IllegalArgumentException se a chave contiver caracteres não ASCII.
	 */
	private static void validateKey(String key) {
		for (char c : key.toCharArray())
			if (c < 0 || c > 127)
				throw new IllegalArgumentException("Key contains non-ASCII characters: " + c);
	}
}
