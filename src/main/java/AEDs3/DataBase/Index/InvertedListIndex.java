package AEDs3.DataBase.Index;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe que implementa um índice reverso por Lista Invertida.
 *
 * Gerencia um índice invertido no disco, mantendo um cache limitado (LRU)
 * na memória com número ajustável de entradas. O restante é armazenado nos
 * arquivos em disco.
 */
public class InvertedListIndex implements AutoCloseable {

	/**
	 * Tamanho padrão do cache, definido como 1024.
	 */
	private static final int DEFAULT_CACHE_SIZE = 1 << 10;

	/**
	 * Frequência máxima permitida para uma palavra, definida como 4096.
	 */
	private static final int MAX_FREQUENCY = 1 << 12;

	/**
	 * Caminho para o arquivo que armazena as postagens reais (palavra para lista de
	 * IDs).
	 */
	private final String blocksFilePath;

	/**
	 * Caminho para o arquivo que armazena o dicionário mapeando palavra para offset
	 * nos blocos.
	 */
	private final String directoryFilePath;

	/**
	 * Caminho para o arquivo que armazena o dicionário mapeando palavra para
	 * frequência.
	 */
	private final String frequencyFilePath;

	/**
	 * Acesso aleatório ao arquivo de blocos.
	 */
	private final RandomAccessFile blkRaf;

	/**
	 * Acesso aleatório ao arquivo de diretório.
	 */
	private final RandomAccessFile dirRaf;

	/**
	 * Acesso aleatório ao arquivo de frequência.
	 */
	private final RandomAccessFile freqRaf;

	/**
	 * Tamanho atual do cache.
	 */
	private long cacheSize;

	/**
	 * Este cache armazena as postagens para até {@link cacheSize} palavras.
	 * Evicção baseada em LRU (LinkedHashMap com ordem de acesso = true).
	 */
	private final Map<String, CachedPosting> cache = new LinkedHashMap<>(16, 0.75f, true) {
		/**
		 * Remove a entrada mais antiga do cache se o tamanho do cache exceder o limite
		 * definido.
		 *
		 * @param eldest A entrada mais antiga no cache.
		 * @return true se a entrada mais antiga deve ser removida, false caso
		 *         contrário.
		 */
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, CachedPosting> eldest) {
			if (size() > getCacheSize()) {
				// Escreve as postagens da palavra removida de volta no disco
				flushPostingToDisk(eldest.getKey(), eldest.getValue());
				return true;
			}
			return false;
		}
	};

	/**
	 * Representa uma postagem em cache, contendo uma lista de IDs associados a uma
	 * palavra e a frequência dessa palavra.
	 */
	private static class CachedPosting {
		/**
		 * Lista de IDs associados a uma palavra específica.
		 */
		List<Integer> ids = new ArrayList<>();
		/**
		 * Frequência da palavra associada à lista de IDs.
		 */
		int frequency;
	}

	/**
	 * Construtor único.
	 *
	 * @param blocksFilePath    Caminho para o arquivo de blocos (postagens).
	 * @param directoryFilePath Caminho para o arquivo de diretório (palavra para
	 *                          offset).
	 * @param frequencyFilePath Caminho para o arquivo de frequência (palavra para
	 *                          frequência).
	 *
	 * @throws IllegalStateException se alguns arquivos existirem enquanto outros
	 *                               não. Cria novos arquivos vazios se nenhum
	 *                               existir; caso contrário, recarrega os arquivos
	 *                               existentes minimamente (não os lê inteiramente
	 *                               na memória).
	 * @throws IOException           se ocorrer um erro de entrada/saída durante a
	 *                               inicialização.
	 */
	public InvertedListIndex(
			String blocksFilePath,
			String directoryFilePath,
			String frequencyFilePath) throws IOException {
		this.blocksFilePath = blocksFilePath;
		this.directoryFilePath = directoryFilePath;
		this.frequencyFilePath = frequencyFilePath;
		this.blkRaf = new RandomAccessFile(blocksFilePath, "rw");
		this.dirRaf = new RandomAccessFile(directoryFilePath, "rw");
		this.freqRaf = new RandomAccessFile(frequencyFilePath, "rw");
		this.cacheSize = DEFAULT_CACHE_SIZE;

		initFiles();
	}

	/**
	 * Cria (insere) uma nova entrada.
	 *
	 * @param word A palavra a ser inserida.
	 * @param id   O ID do documento/entidade a ser associado a essa palavra.
	 * @return true se a entrada foi criada com sucesso.
	 * @throws IllegalStateException se a frequência da palavra for > 10000 ou
	 *                               qualquer outro erro lógico.
	 */
	public boolean create(String word, int id) {
		if (word == null)
			return false;

		// 1) Obter a postagem do cache ou disco
		CachedPosting posting = getPosting(word);

		// 2) Verificar se a frequência já está muito alta
		if (posting.frequency >= MAX_FREQUENCY) {
			throw new IllegalStateException("Palavra '" + word + "' excede a frequência máxima de " + MAX_FREQUENCY);
		}

		// 3) Inserir o ID se ainda não estiver presente
		if (!posting.ids.contains(id)) {
			posting.ids.add(id);
			posting.frequency++;
		}

		// Possivelmente descarregar para o disco se o cache LRU decidir remover
		cache.put(word, posting);

		// 4) Retornar sucesso
		return true;
	}

	/**
	 * Lê (busca) uma entrada.
	 *
	 * @param word A palavra a ser buscada.
	 * @return Todos os IDs correspondentes, ou um array vazio se nenhum for
	 *         encontrado.
	 * @throws IllegalStateException se a frequência da palavra for > 10000.
	 */
	public int[] read(String word) {
		if (word == null)
			return new int[0];

		// 1) Verifique se já temos a postagem
		CachedPosting posting = getPosting(word);

		// 2) Se a frequência for maior que o máximo, lance uma exceção
		if (posting.frequency > MAX_FREQUENCY) {
			throw new IllegalStateException("Palavra '" + word + "' excede a frequência máxima de " + MAX_FREQUENCY);
		}

		if (posting.ids.isEmpty())
			return new int[0];

		// 3) Retorne os IDs como um array de inteiros
		return posting.ids.stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Deleta uma associação (palavra para id).
	 *
	 * @param word a palavra a ser atualizada
	 * @param id   o ID a ser removido de suas postagens
	 * @return true se foi realmente removido; false se não encontrado
	 */
	public boolean delete(String word, int id) {
		if (word == null)
			return false;

		// 1) Recuperar ou criar do disco
		CachedPosting posting = getPosting(word);

		// 2) Tentar remover o ID
		boolean removed = posting.ids.remove((Integer) id);
		if (removed) {
			posting.frequency--;
			if (posting.frequency < 0)
				posting.frequency = 0; // Pra garantir
		}

		// 3) Salvar de volta no cache
		cache.put(word, posting);
		return removed;
	}

	/**
	 * Destrói o índice invertido.
	 *
	 * Este método deleta todos os arquivos associados a este índice do disco,
	 * garantindo que todos os dados em cache sejam primeiro descarregados para o
	 * disco.
	 *
	 * @throws IOException se ocorrer um erro de entrada/saída durante o processo.
	 */
	public void destruct() throws IOException {
		this.close();
		new File(blocksFilePath).delete();
		new File(directoryFilePath).delete();
		new File(frequencyFilePath).delete();
	}

	/**
	 * Lista todos os caminhos de arquivo
	 *
	 * @return um array de caminhos de arquivo que este objeto gerencia
	 */
	public String[] listFilePaths() {
		return new String[] { blocksFilePath, directoryFilePath, frequencyFilePath };
	}

	/**
	 * Inicializa os arquivos necessários para o índice invertido.
	 *
	 * Verifica a existência dos arquivos de blocos, diretório e frequência.
	 * Se nenhum dos arquivos existir, cria novos arquivos vazios.
	 * Se apenas alguns dos arquivos existirem, lança uma exceção indicando
	 * que não é possível inicializar o índice devido à inconsistência.
	 *
	 * @throws RuntimeException      se ocorrer um erro ao criar novos arquivos.
	 * @throws IllegalStateException se alguns arquivos existirem enquanto outros
	 *                               não.
	 */
	private void initFiles() {
		File blocksFile = new File(blocksFilePath);
		File directoryFile = new File(directoryFilePath);
		File freqFile = new File(frequencyFilePath);

		boolean blocksExists = blocksFile.exists();
		boolean directoryExists = directoryFile.exists();
		boolean freqExists = freqFile.exists();

		// Cria arquivos se não existirem
		if (!blocksExists && !directoryExists && !freqExists) {
			try {
				blocksFile.createNewFile();
				directoryFile.createNewFile();
				freqFile.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException("Could not create index files", e);
			}
		} else if (!(blocksExists && directoryExists && freqExists)) {
			// Se nem todos existem, lança um erro.
			throw new IllegalStateException("Some index files exist while others do not. Cannot initialize.");
		}
	}

	/**
	 * Recupera uma postagem do cache ou a carrega do disco se não estiver em cache.
	 * Se não houver entrada no disco (palavra não encontrada no diretório), retorna
	 * uma nova postagem.
	 *
	 * @param word A palavra cuja postagem deve ser recuperada.
	 * @return A postagem em cache correspondente à palavra, ou uma nova postagem se
	 *         não encontrada.
	 */
	private CachedPosting getPosting(String word) {
		if (cache.containsKey(word))
			return cache.get(word);

		// Se não estiver no cache, lê do disco e posta no cache.
		CachedPosting posting = loadPostingFromDisk(word);
		cache.put(word, posting);
		return posting;
	}

	/**
	 * Classe auxiliar para armazenar informações sobre a localização de um bloco no
	 * disco.
	 *
	 * @param offset    Posição inicial do bloco no arquivo.
	 * @param blockSize Tamanho do bloco em bytes.
	 */
	private static record DirectoryEntry(long offset, int blockSize) {
	}

	/**
	 * Método auxiliar que lê uma string codificada em UTF-8 a partir de um arquivo
	 * de acesso aleatório. A string é precedida por um inteiro que indica seu
	 * comprimento.
	 *
	 * @param raf O arquivo de acesso aleatório de onde a string será lida.
	 * @return A string lida do arquivo.
	 * @throws IOException Se ocorrer um erro de entrada/saída durante a leitura.
	 */
	private static String readString(RandomAccessFile raf) throws IOException {
		int len = raf.readInt();
		byte[] buf = new byte[len];
		raf.readFully(buf);
		return new String(buf, StandardCharsets.UTF_8);
	}

	/**
	 * Método auxiliar que escreve uma string codificada em UTF-8 em um arquivo de
	 * acesso aleatório, precedida por um inteiro que indica o comprimento da
	 * string.
	 *
	 * @param raf O arquivo de acesso aleatório onde a string será escrita.
	 * @param s   A string a ser escrita no arquivo.
	 * @throws IOException Se ocorrer um erro de entrada/saída durante a escrita.
	 */
	private static void writeString(RandomAccessFile raf, String s) throws IOException {
		byte[] buf = s.getBytes(StandardCharsets.UTF_8);
		raf.writeInt(buf.length);
		raf.write(buf);
	}

	/**
	 * Carrega ou cria a lista de postagens e frequência para uma palavra específica
	 * do disco.
	 *
	 * @param word A palavra cuja lista de postagens deve ser recuperada.
	 * @return A postagem em cache correspondente à palavra, ou uma nova postagem se
	 *         não encontrada.
	 * @throws RuntimeException Se ocorrer um erro de entrada/saída ao carregar a
	 *                          palavra.
	 */
	private CachedPosting loadPostingFromDisk(String word) {
		CachedPosting posting = new CachedPosting();
		try {
			dirRaf.seek(0);
			freqRaf.seek(0);

			// Encontra o último registro da palavra no diretório.
			DirectoryEntry last = null;
			while (dirRaf.getFilePointer() < dirRaf.length()) {
				String w = readString(dirRaf);
				long off = dirRaf.readLong();
				int sz = dirRaf.readInt();
				if (w.equals(word))
					last = new DirectoryEntry(off, sz);
			}

			// Se encontrou, lê o bloco do arquivo de blocos.
			if (last != null) {
				blkRaf.seek(last.offset);
				int count = blkRaf.readInt();
				for (int i = 0; i < count; i++)
					posting.ids.add(blkRaf.readInt());
			}

			// Finalmente, lê o último registro de frequência para a palavra.
			while (freqRaf.getFilePointer() < freqRaf.length()) {
				String w = readString(freqRaf);
				int f = freqRaf.readInt();
				if (w.equals(word))
					posting.frequency = f;
			}
		} catch (IOException e) {
			throw new RuntimeException("I/O error loading '" + word + "'", e);
		}
		return posting;
	}

	/**
	 * Salva a postagem que está na memória de volta para o disco, adicionando novos
	 * registros.
	 *
	 * @param word    A palavra cuja postagem deve ser salva.
	 * @param posting A postagem em cache que contém a lista de IDs e a frequência
	 *                da palavra.
	 */
	private void flushPostingToDisk(String word, CachedPosting posting) {
		try {
			// Apende ao arquivo de blocos.
			long offset;
			int blockSize;
			offset = blkRaf.length();
			blkRaf.seek(offset);
			int count = posting.ids.size();
			blkRaf.writeInt(count);
			for (int id : posting.ids)
				blkRaf.writeInt(id);
			blockSize = 4 + 4 * count;

			// Apende um novo registro de diretório.
			dirRaf.seek(dirRaf.length());
			writeString(dirRaf, word);
			dirRaf.writeLong(offset);
			dirRaf.writeInt(blockSize);

			// Apende um novo registro de frequência.
			freqRaf.seek(freqRaf.length());
			writeString(freqRaf, word);
			freqRaf.writeInt(posting.frequency);

		} catch (IOException e) {
			throw new RuntimeException("I/O error flushing '" + word + "'", e);
		}
	}

	/**
	 * Descarrega todas as postagens que estão atualmente no cache para o disco.
	 *
	 * Este método itera sobre todas as entradas no cache, salvando cada postagem
	 * no disco e, em seguida, limpa o cache. Isso garante que todas as alterações
	 * feitas nas postagens em cache sejam persistidas no armazenamento permanente.
	 */
	public void flushAllPostingsToDisk() {
		for (Map.Entry<String, CachedPosting> e : cache.entrySet())
			flushPostingToDisk(e.getKey(), e.getValue());
		cache.clear();
	}

	/**
	 * Fecha o índice invertido, garantindo que todas as postagens em cache sejam
	 * descarregadas para o disco antes de fechar os arquivos de acesso aleatório.
	 *
	 * @throws IOException se ocorrer um erro de entrada/saída ao fechar os
	 *                     arquivos.
	 */
	public void close() throws IOException {
		flushAllPostingsToDisk();
		blkRaf.close();
		dirRaf.close();
		freqRaf.close();
	}

	/**
	 * Retorna o tamanho padrão do cache.
	 *
	 * @return O tamanho padrão do cache.
	 */
	public static int getDefaultCacheSize() {
		return DEFAULT_CACHE_SIZE;
	}

	/**
	 * Retorna o tamanho atual do cache.
	 *
	 * @return O tamanho atual do cache.
	 */
	public long getCacheSize() {
		return cacheSize;
	}

	/**
	 * Define o tamanho do cache.
	 *
	 * @param cacheSize O novo tamanho do cache.
	 */
	public void setCacheSize(long cacheSize) {
		this.cacheSize = cacheSize;
	}
}
