package AEDs3.Cryptography;

import java.io.*;

public class Vigenere {

    /**
     * Método principal que executa a criptografia e a descriptografia de um arquivo.
     *
     * @param args Argumentos da linha de comando (não utilizados).
     * @throws Exception Caso ocorra erro de leitura ou escrita de arquivos.
     */
    public static void main(String[] args) throws Exception {
        String key = "minhaChave";
        encrypt("C:\\Users\\pedro\\OneDrive\\Área de Trabalho\\AEDs-III\\src\\test\\resources\\CSVManagerTestDataset.csv", key);
        decrypt("C:\\Users\\pedro\\OneDrive\\Área de Trabalho\\AEDs-III\\src\\test\\resources\\CSVManagerTestDataset.csv.vig", key);
    }

    /**
     * Criptografa um arquivo binário utilizando a cifra de Vigenère adaptada para bytes.
     * O arquivo de saída é criado automaticamente com a extensão ".vig".
     *
     * @param inputPath Caminho completo do arquivo de entrada (original).
     * @param key Chave de criptografia (string repetida para criptografar os bytes).
     * @throws IOException Caso ocorra erro ao ler ou gravar os arquivos.
     */
    public static void encrypt(String inputPath, String key) throws IOException {
        FileInputStream inputStream = new FileInputStream(inputPath);
        String outputPath = inputPath + ".vig";
        FileOutputStream outputStream = new FileOutputStream(outputPath);

        byte[] keyBytes = key.getBytes();
        int keyLength = keyBytes.length;

        byte[] buffer = new byte[4096];
        int bytesRead;
        int position = 0;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                int keyIndex = (position + i) % keyLength;
                buffer[i] = (byte) ((buffer[i] + keyBytes[keyIndex]) % 256);
            }
            outputStream.write(buffer, 0, bytesRead);
            position += bytesRead;
        }

        inputStream.close();
        outputStream.close();
        System.out.println("Arquivo criptografado: " + outputPath);
    }

    /**
     * Descriptografa um arquivo criptografado com a cifra de Vigenère adaptada.
     * O arquivo de saída é criado automaticamente com a extensão ".dec".
     *
     * @param inputPath Caminho completo do arquivo criptografado (com extensão ".vig").
     * @param key Chave de criptografia usada para reverter a operação.
     * @throws IOException Caso ocorra erro ao ler ou gravar os arquivos.
     */
    public static void decrypt(String inputPath, String key) throws IOException {
        FileInputStream inputStream = new FileInputStream(inputPath);
        String outputPath = inputPath.replace(".vig", "") + ".dec";
        FileOutputStream outputStream = new FileOutputStream(outputPath);

        byte[] keyBytes = key.getBytes();
        int keyLength = keyBytes.length;

        byte[] buffer = new byte[4096];
        int bytesRead;
        int position = 0;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                int keyIndex = (position + i) % keyLength;
                buffer[i] = (byte) ((buffer[i] - keyBytes[keyIndex] + 256) % 256);
            }
            outputStream.write(buffer, 0, bytesRead);
            position += bytesRead;
        }

        inputStream.close();
        outputStream.close();
        System.out.println("Arquivo descriptografado: " + outputPath);
    }
}
