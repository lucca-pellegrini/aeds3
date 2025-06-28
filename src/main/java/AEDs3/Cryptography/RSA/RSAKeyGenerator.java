package AEDs3.Cryptography.RSA;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * Classe para gerar e salvar chaves RSA em arquivos binários.
 */
public class RSAKeyGenerator {

	/**
	 * Gera um par de chaves RSA e salva nos arquivos indicados.
	 *
	 * @param publicKeyPath  Caminho do arquivo para salvar a chave pública.
	 * @param privateKeyPath Caminho do arquivo para salvar a chave privada.
	 * @throws Exception Caso ocorra erro durante a geração ou salvamento das
	 *                   chaves.
	 */
	public static void generateKeys(String publicKeyPath, String privateKeyPath) throws IOException {
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
