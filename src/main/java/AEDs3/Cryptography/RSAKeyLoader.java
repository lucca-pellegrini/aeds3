package AEDs3.Cryptography;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Classe responsável por carregar chaves RSA a partir de arquivos binários.
 * Fornece métodos para carregar tanto chaves públicas quanto privadas.
 */
public class RSAKeyLoader {

	/**
	 * Carrega uma chave pública RSA a partir de um arquivo binário especificado.
	 *
	 * @param path Caminho do arquivo que contém a chave pública codificada em
	 *             formato X.509.
	 * @return A chave pública RSA carregada.
	 * @throws InvalidKeySpecException  Se a especificação da chave for inválida.
	 * @throws NoSuchAlgorithmException Se o algoritmo RSA não for encontrado.
	 * @throws IOException              Se ocorrer um erro na leitura do arquivo.
	 */
	public static PublicKey loadPublicKey(String path)
			throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
		try (FileInputStream fis = new FileInputStream(path)) {
			byte[] bytes = fis.readAllBytes();
			X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
			return KeyFactory.getInstance("RSA").generatePublic(spec);
		}
	}

	/**
	 * Carrega uma chave privada RSA a partir de um arquivo binário especificado.
	 *
	 * @param path Caminho do arquivo que contém a chave privada codificada em
	 *             formato PKCS#8.
	 * @return A chave privada RSA carregada.
	 * @throws InvalidKeySpecException  Se a especificação da chave for inválida.
	 * @throws NoSuchAlgorithmException Se o algoritmo RSA não for encontrado.
	 * @throws IOException              Se ocorrer um erro na leitura do arquivo.
	 */
	public static PrivateKey loadPrivateKey(String path)
			throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
		try (FileInputStream fis = new FileInputStream(path)) {
			byte[] bytes = fis.readAllBytes();
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
			return KeyFactory.getInstance("RSA").generatePrivate(spec);
		}
	}
}
