package AEDs3.Cryptography.RSA;

import javax.crypto.*;
import java.io.*;
import java.security.*;

/**
 * Classe para criptografia híbrida de arquivos usando RSA e AES.
 */
public class HybridCryptography {
	/**
	 * Criptografa um arquivo usando criptografia híbrida:
	 * AES para o conteúdo e RSA para a chave AES.
	 *
	 * @param originalFilePath  Caminho do arquivo original a ser criptografado.
	 * @param encryptedFilePath Caminho do arquivo onde será salvo o conteúdo
	 *                          criptografado.
	 * @param publicKey         Chave pública RSA usada para criptografar a chave
	 *                          AES.
	 * @throws Exception Caso ocorra erro durante o processo de criptografia.
	 */
	public static void encryptFileWithRSA_AES(String originalFilePath, String encryptedFilePath, PublicKey publicKey)
			throws Exception {

		// Gerar chave AES de 128 bits
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(128);
		SecretKey aesKey = keyGenerator.generateKey();

		// Inicializar cifra AES para criptografar os dados
		Cipher aesCipher = Cipher.getInstance("AES");
		aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);

		// Inicializar cifra RSA para criptografar a chave AES
		Cipher rsaCipher = Cipher.getInstance("RSA");
		rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
		byte[] encryptedAESKey = rsaCipher.doFinal(aesKey.getEncoded());

		// Abrir fluxo do arquivo original para leitura
		FileInputStream inputStream = new FileInputStream(originalFilePath);
		// Abrir fluxo do arquivo de saída para escrita
		FileOutputStream outputStream = new FileOutputStream(encryptedFilePath);

		// Escrever tamanho da chave AES criptografada (2 bytes)
		outputStream.write(encryptedAESKey.length >> 8);
		outputStream.write(encryptedAESKey.length & 0xFF);
		// Escrever a chave AES criptografada
		outputStream.write(encryptedAESKey);

		// Criar CipherOutputStream para criptografar o conteúdo do arquivo com AES
		CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, aesCipher);
		byte[] buffer = new byte[4096];
		int bytesRead;
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			cipherOutputStream.write(buffer, 0, bytesRead);
		}

		// Fechar streams
		inputStream.close();
		cipherOutputStream.close();

		System.out.println("Arquivo criptografado com sucesso.");
	}

	/**
	 * Exemplo de uso da criptografia híbrida.
	 */
	public static void main(String[] args) throws Exception {
		PublicKey publicKey = RSAKeyLoader.loadPublicKey("src/main/java/AEDs3/Cryptography/RSA/publicKey.bin");

		String originalFile = "C:\\Users\\pedro\\OneDrive\\Área de Trabalho\\AEDs-III\\src\\test\\resources\\CSVManagerTestDataset.csv";
		String encryptedFile = "src/main/java/AEDs3/Cryptography/RSA/secure.bin";

		encryptFileWithRSA_AES(originalFile, encryptedFile, publicKey);
	}
}
