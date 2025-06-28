package AEDs3.Cryptography;

/**
 * A classe Vigenere fornece métodos para criptografar e descriptografar
 * arquivos usando o método de cifra de Vigenère.
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * A classe Vigenere fornece métodos para criptografar e descriptografar
 * arquivos usando a cifra de Vigenère. A cifra de Vigenère é um método de
 * criptografia que utiliza uma série de diferentes cifras de César baseadas
 * nas letras de uma palavra-chave. Esta classe garante que a chave utilizada
 * contenha apenas caracteres ASCII.
 */
public class Vigenere implements EncryptionSystem {
	/**
	 * Criptografa um arquivo de entrada usando a cifra de Vigenère e salva o
	 * resultado em um arquivo de saída.
	 *
	 * @param inputPath  o caminho do arquivo de entrada a ser criptografado.
	 * @param outputPath o caminho do arquivo onde o resultado criptografado será
	 *                   salvo.
	 * @param key        a chave de criptografia, que deve conter apenas caracteres
	 *                   ASCII.
	 * @throws IOException se ocorrer um erro de I/O durante o processo.
	 */
	public void encrypt(String inputPath, String outputPath, PublicKey key) throws IOException {
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(inputPath));
				OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputPath))) {

			byte[] keyBytes = key.getEncoded();
			int keyLength = keyBytes.length;
			int data;

			for (int pos = 0; (data = inputStream.read()) != -1; ++pos) {
				int keyIndex = pos % keyLength;
				data = (data + keyBytes[keyIndex]) % 256;
				outputStream.write(data);
			}
		}
	}

	/**
	 * Descriptografa um arquivo de entrada que foi criptografado usando a cifra
	 * de Vigenère e salva o resultado em um arquivo de saída.
	 *
	 * @param inputPath  o caminho do arquivo de entrada a ser descriptografado
	 * @param outputPath o caminho do arquivo onde o resultado descriptografado será
	 *                   salvo.
	 * @param key        a chave de descriptografia, que deve conter apenas
	 *                   caracteres ASCII.
	 * @throws IOException se ocorrer um erro de I/O durante o processo.
	 */
	public void decrypt(String inputPath, String outputPath, PrivateKey key) throws IOException {
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(inputPath));
				OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputPath))) {

			byte[] keyBytes = key.getEncoded();
			int keyLength = keyBytes.length;
			int data;

			for (int pos = 0; (data = inputStream.read()) != -1; ++pos) {
				int keyIndex = pos % keyLength;
				data = (data - keyBytes[keyIndex] + 256) % 256; // Garante valores positivos.
				outputStream.write(data);
			}
		}
	}
}
