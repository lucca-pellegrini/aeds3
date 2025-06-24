package AEDs3.Cryptography.RSA;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;

/**
 * Classe para descriptografia híbrida de arquivos usando RSA e AES.
 */
public class HybridDecryption {

    /**
     * Descriptografa um arquivo criptografado usando criptografia híbrida:
     * a chave AES é descriptografada com RSA, e o conteúdo com AES.
     *
     * @param encryptedFilePath Caminho do arquivo criptografado.
     * @param outputFilePath Caminho do arquivo onde será salvo o conteúdo descriptografado.
     * @param privateKey Chave privada RSA usada para descriptografar a chave AES.
     * @throws Exception Caso ocorra erro durante o processo de descriptografia.
     */
    public static void decryptFileWithRSA_AES(String encryptedFilePath, String outputFilePath, PrivateKey privateKey) throws Exception {
        FileInputStream inputStream = new FileInputStream(encryptedFilePath);

        // Lê os dois bytes que representam o tamanho da chave AES criptografada
        int size1 = inputStream.read();
        int size2 = inputStream.read();
        int encryptedKeyLength = (size1 << 8) + size2;

        // Lê a chave AES criptografada
        byte[] encryptedAESKey = inputStream.readNBytes(encryptedKeyLength);

        // Inicializa cifra RSA para descriptografar a chave AES
        Cipher rsaCipher = Cipher.getInstance("RSA");
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAESKey);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        // Inicializa cifra AES para descriptografar os dados
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey);

        // CipherInputStream para ler os dados descriptografados com AES
        CipherInputStream cipherInputStream = new CipherInputStream(inputStream, aesCipher);
        FileOutputStream outputStream = new FileOutputStream(outputFilePath);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        cipherInputStream.close();
        outputStream.close();

        System.out.println("Arquivo descriptografado com sucesso.");
    }

    /**
     * Exemplo de uso da descriptografia híbrida.
     */
    public static void main(String[] args) throws Exception {
        PrivateKey privateKey = RSAKeyLoader.loadPrivateKey("src/main/java/AEDs3/Cryptography/RSA/privateKey.bin");
        decryptFileWithRSA_AES("src/main/java/AEDs3/Cryptography/RSA/secure.bin", "src/main/java/AEDs3/Cryptography/RSA/recovered_original.bin", privateKey);
    }
}
