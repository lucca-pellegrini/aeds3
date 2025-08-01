package AEDs3.PatternMatching;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementa o algoritmo de Boyer-Moore para busca eficiente de padrões em
 * textos.
 * Este algoritmo é utilizado para encontrar a posição de um padrão dentro de um
 * texto,
 * utilizando uma técnica de pré-processamento para otimizar a busca.
 */
public class BoyerMoore {
	/**
	 * Tamanho do alfabeto ASCII estendido.
	 */
	private static final int ALPHABET_SIZE = 256;

	/**
	 * Padrão em bytes.
	 */
	private byte[] pattern;

	/**
	 * Tabela de caracteres ruins para desalinhamento.
	 */
	private int[] badCharTbl;

	/**
	 * Construtor da classe BoyerMoore que inicializa o padrão a ser buscado e
	 * preenche a tabela de caracteres ruins.
	 *
	 * @param patternText O padrão de texto que será buscado.
	 */
	public BoyerMoore(String patternText) {
		this.pattern = patternText.getBytes(StandardCharsets.UTF_8);
		this.badCharTbl = new int[ALPHABET_SIZE];
		preprocess();
	}

	/**
	 * Preprocessa o padrão preenchendo a tabela de caracteres ruins.
	 * Esta tabela é utilizada para determinar o deslocamento do padrão em caso de
	 * falha na correspondência durante a busca.
	 */
	private void preprocess() {
		for (int i = 0; i < ALPHABET_SIZE; i++)
			badCharTbl[i] = -1;
		for (int i = 0; i < pattern.length; i++)
			badCharTbl[pattern[i] & 0xFF] = i;
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
			while (j >= 0 && pattern[j] == text[s + j])
				j--;
			if (j < 0)
				return s;
			else
				s += Math.max(1, j - badCharTbl[text[s + j] & 0xFF]);
		}

		return -1;
	}

	/**
	 * Retorna o comprimento do padrão.
	 *
	 * @return O tamanho do padrão em bytes.
	 */
	public int getPatternLength() {
		return pattern.length;
	}

	/**
	 * Verifica se o padrão especificado está presente no texto fornecido.
	 *
	 * @param pattern O padrão a ser buscado.
	 * @param text    O texto onde a busca será realizada.
	 * @return true se o padrão for encontrado no texto, caso contrário, false.
	 * @throws IOException Se ocorrer um erro de leitura do texto.
	 */
	public static boolean match(String pattern, String text) throws IOException {
		InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
		return !searchInStream(in, pattern, 4096).isEmpty();
	}

	/**
	 * Busca todas as ocorrências do padrão em um fluxo de entrada, processando o
	 * texto em blocos.
	 *
	 * @param in        O fluxo de entrada contendo o texto onde a busca será
	 *                  realizada.
	 * @param pattern   O padrão a ser buscado.
	 * @param blockSize O tamanho dos blocos a serem lidos do fluxo de entrada.
	 * @return Uma lista de inteiros representando as posições onde o padrão foi
	 *         encontrado.
	 * @throws IOException Se ocorrer um erro de leitura do fluxo de entrada.
	 */
	public static List<Integer> searchInStream(InputStream in, String pattern, int blockSize) throws IOException {
		ArrayList<Integer> matches = new ArrayList<>();
		BoyerMoore bm = new BoyerMoore(pattern);
		// boolean found = false;

		byte[] buffer = new byte[blockSize];
		byte[] previous = new byte[bm.getPatternLength() - 1];
		int offset = 0;
		int bytesRead;

		// Leitura do arquivo em blocos
		while ((bytesRead = in.read(buffer)) != -1) {
			// Combina o final do bloco anterior com o atual
			byte[] combined = new byte[previous.length + bytesRead];
			System.arraycopy(previous, 0, combined, 0, previous.length);
			System.arraycopy(buffer, 0, combined, previous.length, bytesRead);

			int searchStart = 0;

			// Busca todas as ocorrências do padrão no bloco combinado
			while (searchStart <= combined.length - bm.getPatternLength()) {
				int pos = bm.search(combined, searchStart);
				if (pos == -1)
					break;

				// found = true; // Marca que encontrou o padrão

				int absoluteOffset = offset - previous.length + pos;
				matches.add(absoluteOffset);
				// System.out.println("Pattern \"" + pattern + "\" found at offset: " +
				// absoluteOffset);

				// Exibe trecho de contexto após o padrão encontrado
				// int end = Math.min(pos + bm.getPatternLength() + 40, combined.length);
				// String snippet = new String(combined, pos, end - pos, StandardCharsets.UTF_8)
				// .replaceAll("[\\r\\n]", " ");
				// System.out.println("Snippet: \"" + snippet + "\"\n");

				searchStart = pos + 1;
			}

			// Atualiza o buffer "previous" para incluir o final do bloco atual
			if (bytesRead >= previous.length) {
				System.arraycopy(buffer, bytesRead - previous.length, previous, 0, previous.length);
			} else {
				System.arraycopy(previous, previous.length - (previous.length - bytesRead), previous, 0,
						previous.length - bytesRead);
				System.arraycopy(buffer, 0, previous, previous.length - bytesRead, bytesRead);
			}

			offset += bytesRead;
		}

		// Se não encontrou o padrão em nenhum bloco
		// if (!found)
		// System.out.println("Padrão \"" + pattern + "\" não encontrado no arquivo.");

		return matches;
	}
}
