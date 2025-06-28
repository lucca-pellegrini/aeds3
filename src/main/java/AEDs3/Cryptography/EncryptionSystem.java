package AEDs3.Cryptography;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Interface para sistemas de criptografia que fornecem métodos para
 * criptografar e descriptografar arquivos.
 */
public interface EncryptionSystem {

	/**
	 * Criptografa o arquivo especificado no caminho de entrada e salva o resultado
	 * no caminho de saída usando a chave de criptografia fornecida.
	 *
	 * @param inputPath     o caminho do arquivo a ser criptografado
	 * @param outputPath    o caminho onde o arquivo criptografado será salvo
	 * @param encryptionKey a chave utilizada para criptografar o arquivo
	 * @throws IOException se ocorrer um erro de I/O durante o processo
	 */
	public void encrypt(String inputPath, String outputPath, PublicKey encryptionKey) throws IOException;

	/**
	 * Descriptografa o arquivo especificado no caminho de entrada e salva o
	 * resultado
	 * no caminho de saída usando a chave de descriptografia fornecida.
	 *
	 * @param inputPath     o caminho do arquivo a ser descriptografado
	 * @param outputPath    o caminho onde o arquivo descriptografado será salvo
	 * @param decryptionKey a chave utilizada para descriptografar o arquivo
	 * @throws IOException se ocorrer um erro de I/O durante o processo
	 */
	public void decrypt(String inputPath, String outputPath, PrivateKey decryptionKey) throws IOException;
}
