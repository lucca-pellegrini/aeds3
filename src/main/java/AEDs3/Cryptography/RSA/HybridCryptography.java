package AEDs3.Cryptography.RSA;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * Classe para criptografia híbrida de arquivos usando AES (para conteúdo)
 * e RSA (para criptografar a chave AES).
 */
public class HybridCryptography {

    /**
     * Criptografa um arquivo usando uma chave AES gerada aleatoriamente
     * e criptografa essa chave com RSA (modo manual, via BigInteger).
     *
     * @param originalFilePath    Caminho para o arquivo original a ser criptografado.
     * @param encryptedFilePath   Caminho onde o arquivo criptografado será salvo.
     * @param publicKey           Chave pública RSA usada para criptografar a chave AES.
     * @throws Exception          Se ocorrer erro em qualquer etapa da criptografia.
     */
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

        // Abrir fluxos para leitura e escrita de arquivos
        FileInputStream inputStream = new FileInputStream(originalFilePath);
        FileOutputStream outputStream = new FileOutputStream(encryptedFilePath);

        // Escrever tamanho da chave AES criptografada (2 bytes)
        outputStream.write(encryptedAESKey.length >> 8);
        outputStream.write(encryptedAESKey.length & 0xFF);
        // Escrever chave AES criptografada
        outputStream.write(encryptedAESKey);

        // Criptografar o conteúdo do arquivo com AES
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

    /**
     * Descriptografa um arquivo criptografado com AES e uma chave AES
     * que foi criptografada manualmente com RSA.
     *
     * @param encryptedFilePath   Caminho do arquivo criptografado de entrada.
     * @param outputFilePath      Caminho onde o arquivo restaurado será salvo.
     * @param privateKey          Chave privada RSA usada para recuperar a chave AES.
     * @throws Exception          Se ocorrer erro durante o processo de descriptografia.
     */
    public static void decryptFileWithRSA_AES(String encryptedFilePath, String outputFilePath, PrivateKey privateKey) throws Exception {
        FileInputStream inputStream = new FileInputStream(encryptedFilePath);

        // Ler tamanho da chave AES criptografada (2 bytes)
        int keyLength = (inputStream.read() << 8) | inputStream.read();
        byte[] encryptedAESKey = new byte[keyLength];
        inputStream.read(encryptedAESKey);

        // Descriptografar chave AES usando RSA manual
        byte[] rawAESKey = decryptAESKeyWithRSA(encryptedAESKey, privateKey);
        rawAESKey = fixAESKeyLength(rawAESKey, 16); // Garante 16 bytes
        SecretKey aesKey = new SecretKeySpec(rawAESKey, "AES");

        // Inicializar cifra AES para descriptografar os dados
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey);

        // Ler e restaurar o conteúdo do arquivo
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
     * Realiza a criptografia da chave AES com a chave pública RSA de forma manual,
     * utilizando a operação de exponenciação modular: C = M^e mod n.
     *
     * @param aesKeyBytes  Bytes da chave AES a ser criptografada.
     * @param publicKey    Chave pública RSA.
     * @return             Chave AES criptografada com RSA.
     * @throws Exception   Se ocorrer erro ao acessar os parâmetros da chave RSA.
     */
    public static byte[] encryptAESKeyWithRSA(byte[] aesKeyBytes, PublicKey publicKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec keySpec = keyFactory.getKeySpec(publicKey, RSAPublicKeySpec.class);
        BigInteger modulus = keySpec.getModulus();
        BigInteger exponent = keySpec.getPublicExponent();
        BigInteger message = new BigInteger(1, aesKeyBytes);
        BigInteger encrypted = message.modPow(exponent, modulus);
        return encrypted.toByteArray();
    }

    /**
     * Descriptografa a chave AES criptografada com RSA manualmente,
     * usando a operação de decodificação modular: M = C^d mod n.
     *
     * @param encryptedAESKey  Chave AES criptografada com RSA.
     * @param privateKey       Chave privada RSA.
     * @return                 Bytes da chave AES original.
     * @throws Exception       Se ocorrer erro ao acessar os parâmetros da chave RSA.
     */
    public static byte[] decryptAESKeyWithRSA(byte[] encryptedAESKey, PrivateKey privateKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPrivateKeySpec keySpec = keyFactory.getKeySpec(privateKey, RSAPrivateKeySpec.class);
        BigInteger modulus = keySpec.getModulus();
        BigInteger exponent = keySpec.getPrivateExponent();
        BigInteger encrypted = new BigInteger(1, encryptedAESKey);
        BigInteger decrypted = encrypted.modPow(exponent, modulus);
        return decrypted.toByteArray();
    }

    /**
     * Garante que a chave AES tenha exatamente o tamanho esperado.
     * Se necessário, ajusta removendo ou preenchendo os bytes à esquerda.
     *
     * @param rawBytes        Chave em bytes após a descriptografia.
     * @param expectedLength  Tamanho esperado (ex: 16 para AES-128).
     * @return                Chave ajustada com tamanho correto.
     */
    public static byte[] fixAESKeyLength(byte[] rawBytes, int expectedLength) {
        if (rawBytes.length == expectedLength) return rawBytes;
        byte[] fixed = new byte[expectedLength];
        int start = Math.max(0, rawBytes.length - expectedLength);
        System.arraycopy(rawBytes, start, fixed, expectedLength - (rawBytes.length - start), rawBytes.length - start);
        return fixed;
    }

    /**
     * Método principal de demonstração da criptografia e descriptografia híbrida.
     *
     * @param args Argumentos da linha de comando (não utilizados).
     * @throws Exception Se ocorrer erro durante o processo.
     */
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
