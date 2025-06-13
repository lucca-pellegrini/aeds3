package AEDs3.DataBase.PatternMatching;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A classe KMP implementa o algoritmo de Knuth-Morris-Pratt para busca de
 * padrões em texto. Este algoritmo é eficiente para encontrar todas as
 * ocorrências de um padrão em um texto, utilizando uma tabela de prefixos para
 * otimizar a busca.
 *
 * A classe oferece métodos para construir a tabela de prefixos e realizar a
 * busca em um fluxo de entrada binário, preservando o contexto ao redor do
 * padrão encontrado.
 */
public class KMP {
	/**
	 * Classe interna que representa o resultado de uma ocorrência do padrão no
	 * texto. Contém a posição do padrão no arquivo e o contexto em que foi
	 * encontrado.
	 */
	protected static class MatchResult {
		/** Posição do padrão no arquivo */
		long pos;
		/** Trecho de contexto onde o padrão foi encontrado */
		String ctx;

		/**
		 * Construtor para inicializar um resultado de correspondência com a posição e o
		 * contexto.
		 *
		 * @param pos A posição do padrão no arquivo.
		 * @param ctx O contexto em que o padrão foi encontrado.
		 */
		MatchResult(long pos, String ctx) {
			this.pos = pos;
			this.ctx = ctx;
		}

		@Override
		public String toString() {
			return "Position: " + pos + " | Context: \"" + ctx + "\"";
		}
	}

	/**
	 * Constrói a tabela de prefixo, também conhecida como LPS (Longest Prefix
	 * Suffix),
	 * que é utilizada pelo algoritmo de Knuth-Morris-Pratt para otimizar a busca.
	 *
	 * @param pattern O padrão a ser buscado no texto.
	 * @return Um array de inteiros contendo os comprimentos dos maiores prefixos
	 *         próprios
	 *         que também são sufixos para cada posição do padrão.
	 */
	public static int[] buildPrefixTable(String pattern) {
		int[] lps = new int[pattern.length()];

		for (int i = 1, len = 0; i < pattern.length();) {
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
	 * Realiza a busca do padrão em um fluxo de entrada binário utilizando o
	 * algoritmo
	 * KMP com leitura em blocos. A busca é insensível a maiúsculas/minúsculas e
	 * preserva um contexto ao redor do padrão encontrado.
	 *
	 * @param inputStream Fluxo de entrada para leitura do arquivo binário.
	 * @param pattern     O padrão (palavra) a ser buscado no texto.
	 * @param blockSize   Tamanho do bloco de leitura em bytes.
	 * @param contextSize Quantidade de caracteres de contexto a serem exibidos
	 *                    antes e
	 *                    depois do padrão encontrado.
	 * @return Uma lista de objetos MatchResult contendo as ocorrências encontradas.
	 * @throws IOException Se ocorrer um erro de leitura do arquivo.
	 */
	protected static List<MatchResult> searchInStream(InputStream inputStream, String pattern, int blockSize,
			int contextSize) throws IOException {
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
			for (int i = 0, j = 0; i < chunkLower.length();) {
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

			// Atualiza o trecho final do bloco atual para usar como sobreposição no próximo
			// bloco
			overlap = new StringBuilder(chunk);
			if (overlap.length() > pattern.length() + contextSize) {
				overlap = new StringBuilder(overlap.substring(overlap.length() - (pattern.length() + contextSize)));
			}

			totalOffset += bytesRead; // Avança o deslocamento total
		}

		return results;
	}

	/**
	 * Verifica se um padrão está presente em um texto utilizando o algoritmo KMP.
	 * A busca é insensível a maiúsculas/minúsculas.
	 *
	 * @param pattern O padrão a ser buscado no texto.
	 * @param text    O texto onde o padrão será procurado.
	 * @return {@code true} se o padrão for encontrado no texto, {@code false} caso
	 *         contrário.
	 * @throws IOException Se ocorrer um erro de leitura do fluxo de entrada.
	 */
	public static boolean match(String pattern, String text) throws IOException {
		InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
		return !searchInStream(in, pattern, 4096, 20).isEmpty();
	}
}
