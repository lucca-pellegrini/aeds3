package AEDs3.Cryptography.RSA;

import java.io.*;
import java.security.*;
import java.security.spec.*;

/**
 * Classe para carregar chaves RSA a partir de arquivos binários.
 */
public class RSAKeyLoader {

	/**
	 * Carrega uma chave pública RSA a partir do arquivo especificado.
	 *
	 * @param path Caminho do arquivo que contém a chave pública codificada.
	 * @return A chave pública RSA carregada.
	 * @throws Exception Caso ocorra erro na leitura do arquivo ou na geração da
	 *                   chave.
	 */
	public static PublicKey loadPublicKey(String path) throws Exception {
		byte[] bytes = new FileInputStream(path).readAllBytes();
		X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
		return KeyFactory.getInstance("RSA").generatePublic(spec);
	}

	/**
	 * Carrega uma chave privada RSA a partir do arquivo especificado.
	 *
	 * @param path Caminho do arquivo que contém a chave privada codificada.
	 * @return A chave privada RSA carregada.
	 * @throws Exception Caso ocorra erro na leitura do arquivo ou na geração da
	 *                   chave.
	 */
	public static PrivateKey loadPrivateKey(String path) throws Exception {
		byte[] bytes = new FileInputStream(path).readAllBytes();
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
		return KeyFactory.getInstance("RSA").generatePrivate(spec);
	}
}
