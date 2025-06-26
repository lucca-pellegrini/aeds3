package AEDs3.Cryptography.RSA;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * Classe para criptografia híbrida de arquivos usando RSA e AES.
 */
public class HybridCryptography {

    public static void encryptFileWithRSA_AES(String originalFilePath, String encryptedFilePath, PublicKey publicKey) throws Exception {
        // Gerar chave AES de 128 bits
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey aesKey = keyGenerator.generateKey();

        // Inicializar cifra AES para criptografar os dados
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);

        // Criptografar a chave AES com RSA manualmente
        byte[] encryptedAESKey = encryptAESKeyWithRSA(aesKey.getEncoded(), publicKey);

        // Abrir fluxo do arquivo original para leitura
        FileInputStream inputStream = new FileInputStream(originalFilePath);
        FileOutputStream outputStream = new FileOutputStream(encryptedFilePath);

        // Escrever tamanho da chave AES criptografada (2 bytes)
        outputStream.write(encryptedAESKey.length >> 8);
        outputStream.write(encryptedAESKey.length & 0xFF);
        outputStream.write(encryptedAESKey);

        // Criptografar o conteúdo com AES
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, aesCipher);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            cipherOutputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        cipherOutputStream.close();

        System.out.println("Arquivo criptografado com sucesso.");
    }

    public static void decryptFileWithRSA_AES(String encryptedFilePath, String outputFilePath, PrivateKey privateKey) throws Exception {
        FileInputStream inputStream = new FileInputStream(encryptedFilePath);

        // Ler o tamanho da chave RSA criptografada
        int keyLength = (inputStream.read() << 8) | inputStream.read();
        byte[] encryptedAESKey = new byte[keyLength];
        inputStream.read(encryptedAESKey);

        // Descriptografar chave AES manualmente com RSA
        byte[] rawAESKey = decryptAESKeyWithRSA(encryptedAESKey, privateKey);
        rawAESKey = fixAESKeyLength(rawAESKey, 16);
        SecretKey aesKey = new SecretKeySpec(rawAESKey, "AES");

        // Inicializar cifra AES para descriptografar o conteúdo
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey);

        // Criar CipherInputStream para descriptografar os dados
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

    // --- Métodos auxiliares para RSA manual ---

    public static byte[] encryptAESKeyWithRSA(byte[] aesKeyBytes, PublicKey publicKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);
        BigInteger modulus = keySpec.getModulus();
        BigInteger exponent = keySpec.getPublicExponent();
        BigInteger message = new BigInteger(1, aesKeyBytes);
        BigInteger encrypted = message.modPow(exponent, modulus);
        return encrypted.toByteArray();
    }

    public static byte[] decryptAESKeyWithRSA(byte[] encryptedAESKey, PrivateKey privateKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = keyFactory.getKeySpec(privateKey, RSAPrivateKeySpec.class);
        BigInteger modulus = keySpec.getModulus();
        BigInteger exponent = keySpec.getPrivateExponent();
        BigInteger encrypted = new BigInteger(1, encryptedAESKey);
        BigInteger decrypted = encrypted.modPow(exponent, modulus);
        return decrypted.toByteArray();
    }

    public static byte[] fixAESKeyLength(byte[] rawBytes, int expectedLength) {
        if (rawBytes.length == expectedLength) return rawBytes;
        byte[] fixed = new byte[expectedLength];
        int start = Math.max(0, rawBytes.length - expectedLength);
        System.arraycopy(rawBytes, start, fixed, expectedLength - (rawBytes.length - start), rawBytes.length - start);
        return fixed;
    }

    public static void main(String[] args) throws Exception {
        PublicKey publicKey = RSAKeyLoader.loadPublicKey("src/main/java/AEDs3/Cryptography/RSA/publicKey.bin");
        PrivateKey privateKey = RSAKeyLoader.loadPrivateKey("src/main/java/AEDs3/Cryptography/RSA/privateKey.bin");

        String originalFile = "C:\\Users\\pedro\\OneDrive\\Área de Trabalho\\AEDs-III\\src\\test\\resources\\CSVManagerTestDataset.csv";
        String encryptedFile = "src/main/java/AEDs3/Cryptography/RSA/secure.bin";
        String decryptedFile = "src/main/java/AEDs3/Cryptography/RSA/decrypted.bin";

        encryptFileWithRSA_AES(originalFile, encryptedFile, publicKey);
        decryptFileWithRSA_AES(encryptedFile, decryptedFile, privateKey);
    }
}
