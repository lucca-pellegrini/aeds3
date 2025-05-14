package AEDs3.DataBase.Compression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Implementação do algoritmo de compressão LZW.
 * Realiza a compressão e descompressão de arquivos binários.
 */
public class LZW {
    public static final int BITS_PER_INDEX = 12; // Tamanho do índice em bits

    /**
     * Método principal para execução do algoritmo LZW.
     */
    public static void main(String[] args) {
        try {
            File file = new File("C:\\Users\\pedro\\OneDrive\\Área de Trabalho\\AEDs-III\\AEDs-III-8\\src\\main\\java\\AEDs3\\DataBase\\Compression\\arquivo_grande.bin");
            FileInputStream fis = new FileInputStream(file);

            byte[] originalBytes = fis.readAllBytes();
            fis.close();

            // Codificação (Compressão)
            byte[] encodedBytes = encode(originalBytes);

            // Grava o arquivo comprimido
            FileOutputStream fos = new FileOutputStream("compressed_file.lzw");
            fos.write(encodedBytes);
            fos.close();

            // Lê o arquivo comprimido
            fis = new FileInputStream("compressed_file.lzw");
            byte[] encodedCopy = fis.readAllBytes();
            fis.close();

            // Decodificação (Descompressão)
            byte[] decodedBytes = decode(encodedCopy);

            // Grava o arquivo descomprimido
            fos = new FileOutputStream("decompressed_file.bin");
            fos.write(decodedBytes);
            fos.close();

            // Estatísticas de compressão
            System.out.println("\nOriginal Size: " + originalBytes.length + " bytes");
            System.out.println("Compressed Size: " + encodedBytes.length + " bytes");
            System.out.println("Compression Efficiency: " + (100 * (1 - (float) encodedBytes.length / (float) originalBytes.length)) + "%");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Método para codificar os bytes usando LZW.
     * @param originalBytes sequência de bytes original.
     * @return array de bytes codificados.
     */
    public static byte[] encode(byte[] originalBytes) throws Exception {

        ArrayList<ArrayList<Byte>> dictionary = new ArrayList<>();
        ArrayList<Byte> byteSequence;
        byte b;

        // Inicialização do dicionário com todos os bytes possíveis (-128 a 127)
        for (int j = -128; j < 128; j++) {
            b = (byte) j;
            byteSequence = new ArrayList<>();
            byteSequence.add(b);
            dictionary.add(byteSequence);
        }

        ArrayList<Integer> output = new ArrayList<>();
        int i = 0;

        while (i < originalBytes.length) {
            byteSequence = new ArrayList<>();
            b = originalBytes[i];
            byteSequence.add(b);

            int index = dictionary.indexOf(byteSequence);
            int lastIndex = index;

            // Expande a sequência até não encontrar no dicionário
            while (index != -1 && i < originalBytes.length - 1) {
                i++;
                b = originalBytes[i];
                byteSequence.add(b);
                index = dictionary.indexOf(byteSequence);
                if (index != -1) lastIndex = index;
            }

            output.add(lastIndex);

            if (dictionary.size() < (Math.pow(2, BITS_PER_INDEX) - 1))
                dictionary.add(byteSequence);

            if (index != -1 && i == originalBytes.length - 1)
                break;
        }

        BitArray bits = new BitArray(output.size() * BITS_PER_INDEX);
        int l = output.size() * BITS_PER_INDEX - 1;

        // Converte os índices para bits e armazena em um BitArray
        for (i = output.size() - 1; i >= 0; i--) {
            int n = output.get(i);
            for (int m = 0; m < BITS_PER_INDEX; m++) {
                if (n % 2 == 0)
                    bits.clear(l);
                else
                    bits.set(l);
                l--;
                n /= 2;
            }
        }

        return bits.toByteArray();
    }

    /**
     * Método para decodificar os bytes comprimidos usando LZW.
     * @param encodedBytes sequência de bytes comprimida.
     * @return array de bytes descomprimidos.
     */
    public static byte[] decode(byte[] encodedBytes) throws Exception {

        BitArray bits = new BitArray(encodedBytes);
        ArrayList<Integer> indices = new ArrayList<>();

        // Recupera os índices a partir do BitArray
        int k = 0;
        for (int i = 0; i < bits.length() / BITS_PER_INDEX; i++) {
            int n = 0;
            for (int j = 0; j < BITS_PER_INDEX; j++) {
                n = n * 2 + (bits.get(k++) ? 1 : 0);
            }
            indices.add(n);
        }

        ArrayList<ArrayList<Byte>> dictionary = new ArrayList<>();
        byte b;

        // Inicializa o dicionário com todos os bytes possíveis
        for (int j = -128; j < 128; j++) {
            b = (byte) j;
            ArrayList<Byte> byteSequence = new ArrayList<>();
            byteSequence.add(b);
            dictionary.add(byteSequence);
        }

        ArrayList<Byte> originalBytes = new ArrayList<>();

        // Decodifica os índices para a sequência de bytes original
        for (int index : indices) {
            ArrayList<Byte> byteSequence = dictionary.get(index);
            originalBytes.addAll(byteSequence);
        }

        byte[] outputBytes = new byte[originalBytes.size()];
        for (int i = 0; i < originalBytes.size(); i++)
            outputBytes[i] = originalBytes.get(i);

        return outputBytes;
    }
}
