package AEDs3.DataBase.PatternMatching;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Classe principal que executa a busca de um padrão textual em um arquivo binário
 * utilizando o algoritmo de Boyer-Moore.
 */
public class Boyer_Moore {

    private static final int ALPHABET_SIZE = 256; // Tamanho do alfabeto ASCII estendido

    /**
     * Classe interna que implementa o algoritmo de Boyer-Moore para busca eficiente de padrões.
     */
    public static class BoyerMoore {
        private byte[] pattern; // Padrão em bytes
        private int[] badChar;  // Tabela de caracteres ruins para desalinhamento

        /**
         * Construtor da classe BoyerMoore.
         *
         * @param patternText O padrão de texto que será buscado.
         */
        public BoyerMoore(String patternText) {
            this.pattern = patternText.getBytes(StandardCharsets.UTF_8);
            this.badChar = new int[ALPHABET_SIZE];
            preprocess();
        }

        /**
         * Preprocessa o padrão preenchendo a tabela de caracteres ruins.
         * Essa tabela é usada para determinar o deslocamento em caso de falha na correspondência.
         */
        private void preprocess() {
            for (int i = 0; i < ALPHABET_SIZE; i++) badChar[i] = -1;
            for (int i = 0; i < pattern.length; i++) {
                badChar[pattern[i] & 0xFF] = i;
            }
        }

        /**
         * Executa a busca do padrão no texto fornecido a partir de uma posição inicial.
         *
         * @param text  O vetor de bytes onde a busca será realizada.
         * @param start A posição inicial no vetor onde a busca deve começar.
         * @return A posição onde o padrão foi encontrado ou -1 se não encontrado.
         */
        public int search(byte[] text, int start) {
            int n = text.length;
            int m = pattern.length;
            int s = start;

            while (s <= n - m) {
                int j = m - 1;
                while (j >= 0 && pattern[j] == text[s + j]) {
                    j--;
                }
                if (j < 0) return s;
                else s += Math.max(1, j - badChar[text[s + j] & 0xFF]);
            }

            return -1;
        }

        /**
         * Retorna o comprimento do padrão.
         *
         * @return Tamanho do padrão.
         */
        public int getPatternLength() {
            return pattern.length;
        }
    }

    /**
     * Método principal que realiza a leitura de um arquivo binário em blocos e
     * aplica o algoritmo de Boyer-Moore para localizar ocorrências do padrão.
     *
     * @param args Argumentos de linha de comando (não utilizados).
     */
    public static void main(String[] args) {
        // Caminho do arquivo binário
        String filePath = "C:\\Users\\pedro\\OneDrive\\Área de Trabalho\\aeds3\\AEDs-III-2\\src\\main\\java\\AEDs3\\DataBase\\Pattern_Matching\\arquivo(1).bin";
        String patternText = "Talvez"; // Padrão de busca

        BoyerMoore bm = new BoyerMoore(patternText);
        boolean found = false;  

        // Bloco try-with-resources para garantir fechamento do arquivo
        try (FileInputStream fis = new FileInputStream(filePath)) {
            int blockSize = 4096;
            byte[] buffer = new byte[blockSize];
            byte[] previous = new byte[bm.getPatternLength() - 1];
            int offset = 0;
            int bytesRead;

            // Leitura do arquivo em blocos
            while ((bytesRead = fis.read(buffer)) != -1) {
                // Combina o final do bloco anterior com o atual
                byte[] combined = new byte[previous.length + bytesRead];
                System.arraycopy(previous, 0, combined, 0, previous.length);
                System.arraycopy(buffer, 0, combined, previous.length, bytesRead);

                int searchStart = 0;

                // Busca todas as ocorrências do padrão no bloco combinado
                while (searchStart <= combined.length - bm.getPatternLength()) {
                    int pos = bm.search(combined, searchStart);
                    if (pos == -1) break;

                    found = true; // Marca que encontrou o padrão

                    int absoluteOffset = offset - previous.length + pos;
                    System.out.println("Pattern \"" + patternText + "\" found at offset: " + absoluteOffset);

                    // Exibe trecho de contexto após o padrão encontrado
                    int end = Math.min(pos + bm.getPatternLength() + 40, combined.length);
                    String snippet = new String(combined, pos, end - pos, StandardCharsets.UTF_8)
                            .replaceAll("[\\r\\n]", " ");
                    System.out.println("Snippet: \"" + snippet + "\"\n");

                    searchStart = pos + 1;
                }

                // Atualiza o buffer "previous" para incluir o final do bloco atual
                if (bytesRead >= previous.length) {
                    System.arraycopy(buffer, bytesRead - previous.length, previous, 0, previous.length);
                } else {
                    System.arraycopy(previous, previous.length - (previous.length - bytesRead), previous, 0, previous.length - bytesRead);
                    System.arraycopy(buffer, 0, previous, previous.length - bytesRead, bytesRead);
                }

                offset += bytesRead;
            }

            // Se não encontrou o padrão em nenhum bloco
            if (!found) {
            System.out.println("Padrão \"" + patternText + "\" não encontrado no arquivo.");
            }

        } catch (IOException e) {
            /**
             * Captura exceções de entrada/saída e exibe uma mensagem de erro.
             *
             * @throws IOException Caso ocorra um erro ao ler o arquivo.
             */
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}
