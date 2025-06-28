package AEDs3.Cryptography.RSA;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * Classe responsável por gerar e salvar chaves RSA em arquivos binários.
 * Esta classe utiliza o algoritmo RSA para criar um par de chaves (pública e
 * privada) e as armazena nos caminhos de arquivos especificados.
 */
public class RSAKeyGenerator {

	/**
	 * Gera um par de chaves RSA e salva nos arquivos especificados.
	 *
	 * @param privateKeyPath Caminho do arquivo onde a chave privada será salva.
	 * @param publicKeyPath  Caminho do arquivo onde a chave pública será salva.
	 * @throws IOException      Se ocorrer um erro durante o salvamento das chaves
	 *                          nos arquivos.
	 * @throws RuntimeException Se o algoritmo RSA não estiver disponível no
	 *                          ambiente de execução.
	 */
	public static void generateKeys(String privateKeyPath, String publicKeyPath) throws IOException {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			KeyPair keyPair = generator.generateKeyPair();

			try (FileOutputStream out = new FileOutputStream(publicKeyPath)) {
				out.write(keyPair.getPublic().getEncoded());
			}

			try (FileOutputStream out = new FileOutputStream(privateKeyPath)) {
				out.write(keyPair.getPrivate().getEncoded());
			}
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
