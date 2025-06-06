package AEDs3.DataBase.Pattern_Matching;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class KMP {

    /**
     * Constrói a tabela de prefixo (também chamada de LPS - Longest Prefix Suffix)
     * usada pelo algoritmo de Knuth-Morris-Pratt.
     *
     * @param pattern O padrão a ser buscado.
     * @return Um array contendo os comprimentos dos maiores prefixos próprios que são também sufixos.
     */
    public static int[] buildPrefixTable(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0, i = 1;

        while (i < pattern.length()) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i++] = len;
            } else if (len > 0) {
                len = lps[len - 1];
            } else {
                lps[i++] = 0;
            }
        }
        return lps;
    }

    /**
     * Classe que representa o resultado de uma ocorrência do padrão no texto.
     */
    public static class MatchResult {
        long position;     // Posição do padrão no arquivo
        String context;    // Trecho de contexto onde o padrão foi encontrado

        MatchResult(long position, String context) {
            this.position = position;
            this.context = context;
        }

        @Override
        public String toString() {
            return "Position: " + position + " | Context: \"" + context + "\"";
        }
    }

    /**
     * Realiza a busca do padrão em um fluxo de entrada binário, usando o algoritmo KMP com leitura em blocos.
     * A busca é feita de forma insensível a maiúsculas/minúsculas e preserva um contexto ao redor do padrão encontrado.
     *
     * @param inputStream Fluxo de entrada para leitura do arquivo binário.
     * @param pattern      O padrão (palavra) a ser buscado.
     * @param blockSize    Tamanho do bloco de leitura (em bytes).
     * @param contextSize  Quantidade de caracteres de contexto antes e depois do padrão.
     * @return Uma lista com os resultados das ocorrências encontradas.
     * @throws IOException Em caso de erro de leitura do arquivo.
     */
    public static List<MatchResult> searchInStream(InputStream inputStream, String pattern, int blockSize, int contextSize) throws IOException {
        List<MatchResult> results = new ArrayList<>();

        pattern = pattern.toLowerCase(); // Ignora diferenças de maiúsculas/minúsculas
        int[] lps = buildPrefixTable(pattern);
        byte[] buffer = new byte[blockSize + pattern.length()];
        StringBuilder overlap = new StringBuilder(); // Armazena o final do bloco anterior
        long totalOffset = 0; // Posição absoluta no arquivo

        int bytesRead;
        while ((bytesRead = inputStream.read(buffer, 0, blockSize)) != -1) {
            // Junta o final do bloco anterior com o atual
            String chunk = overlap.toString() + new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            String chunkLower = chunk.toLowerCase();

            // Executa o algoritmo KMP sobre o bloco atual
            int i = 0, j = 0;
            while (i < chunkLower.length()) {
                if (chunkLower.charAt(i) == pattern.charAt(j)) {
                    i++;
                    j++;
                }
                if (j == pattern.length()) {
                    int matchIndex = i - j;

                    // Calcula a posição real no arquivo
                    long realPosition = totalOffset + matchIndex - overlap.length();

                    // Extrai o trecho de contexto em torno do padrão encontrado
                    int contextStart = Math.max(0, matchIndex - contextSize);
                    int contextEnd = Math.min(chunk.length(), matchIndex + pattern.length() + contextSize);
                    String context = chunk.substring(contextStart, contextEnd).replaceAll("\n", "\\n");

                    results.add(new MatchResult(realPosition, context));
                    j = lps[j - 1]; // Continua procurando a próxima ocorrência
                } else if (i < chunkLower.length() && chunkLower.charAt(i) != pattern.charAt(j)) {
                    if (j != 0) {
                        j = lps[j - 1];
                    } else {
                        i++;
                    }
                }
            }

            // Atualiza o trecho final do bloco atual para usar como sobreposição no próximo bloco
            overlap = new StringBuilder(chunk);
            if (overlap.length() > pattern.length() + contextSize) {
                overlap = new StringBuilder(overlap.substring(overlap.length() - (pattern.length() + contextSize)));
            }

            totalOffset += bytesRead; // Avança o deslocamento total
        }

        return results;
    }

    /**
     * Função principal para execução do programa.
     * Define o padrão a ser buscado, o caminho do arquivo, e exibe os resultados encontrados.
     */
    public static void main(String[] args) {
    String pattern = "pedro"; // Palavra a ser buscada
    String filePath = "C:\\Users\\pedro\\OneDrive\\Área de Trabalho\\aeds3\\AEDs-III-2\\src\\main\\java\\AEDs3\\DataBase\\Pattern_Matching\\arquivo(1).bin";
    int contextSize = 20; // Número de caracteres de contexto antes/depois do padrão

    try (InputStream input = new BufferedInputStream(new FileInputStream(filePath))) {
        List<MatchResult> results = searchInStream(input, pattern, 4096, contextSize);

        if (results.isEmpty()) {
            System.out.println("Padrão \"" + pattern + "\" não encontrado no arquivo.");
        } else {
            for (MatchResult r : results) {
                System.out.println(r);
            }
        }
    } catch (IOException e) {
        System.err.println("Erro ao ler o arquivo: " + e.getMessage());
    }
}
}
