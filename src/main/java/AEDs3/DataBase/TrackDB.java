package AEDs3.DataBase;

import AEDs3.DataBase.Index.*;
import AEDs3.DataBase.Track.Field;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Representa um banco de dados binário de faixas de música, permitindo
 * operações de CRUD (Criar, Ler, Atualizar e Deletar), filtragem e ordenação.
 * O banco de dados é gerenciado em um arquivo binário, com cada faixa de música
 * armazenada como um registro.
 *
 * <p>
 * Esta classe implementa a interface {@link Iterable}, permitindo que o banco
 * de dados seja percorrido através de um iterador.
 * </p>
 *
 * <p>
 * Além disso, a classe implementa a interface {@link AutoCloseable}, garantindo
 * que o arquivo seja fechado corretamente após o uso.
 * </p>
 *
 * @see Track
 * @see TrackFilter
 */
public class TrackDB implements Iterable<Track>, AutoCloseable {
	/**
	 * Arquivo que contém os dados binários do banco de dados.
	 */
	protected RandomAccessFile file;

	/**
	 * Caminho para o arquivo de banco de dados.
	 */
	protected final String filePath;

	protected Index index;
	protected InvertedListIndex nameIndex;
	protected InvertedListIndex albumIndex;
	protected InvertedListIndex artistIndex;

	/**
	 * Posição do último registro de faixa no banco de dados.
	 */
	protected long lastBinaryTrackPos;

	/**
	 * Filtro de busca aplicado nas faixas do banco de dados. É respeitado pelo
	 * iterador, que ignorará registros que não correspondem.
	 */
	protected TrackFilter searchFilter;

	// Itens do cabeçalho
	/**
	 * ID único do banco de dados.
	 */
	protected UUID uuid;

	/**
	 * Último ID inserido no banco de dados.
	 */
	protected int lastId;

	/**
	 * Número de faixas válidas no banco de dados.
	 */
	protected int numTracks;

	/**
	 * Número de espaços usados no banco de dados, incluindo os com lápides.
	 */
	protected int numSpaces;

	/**
	 * Máscara de bits que contém diversas flags para o banco de dados.
	 */
	protected long flags;

	/**
	 * Tamanho do cabeçalho em bytes, incluindo os metadados do banco de dados.
	 */
	protected static final short HEADER_SIZE = 3 * Long.SIZE / 8 + 3 * Integer.SIZE / 8;

	// Parâmetros para a ordenação
	/**
	 * Indica se o segmento atual de faixas foi completamente processado.
	 */
	protected boolean segmentFinished = false;

	/**
	 * Último ID encontrado pelo iterador.
	 */
	protected int lastIteratorId = 0;

	/**
	 * Posição do início do segmento atual de faixas.
	 */
	protected long segmentStart = HEADER_SIZE;

	/**
	 * Constrói uma instância do banco de dados a partir de um arquivo.
	 *
	 * @param fileName Caminho para o arquivo do banco de dados.
	 * @throws FileNotFoundException Se o arquivo não for encontrado.
	 * @throws IOException           Se ocorrer um erro de leitura ou gravação no
	 *                               arquivo.
	 */
	public TrackDB(String fileName) throws FileNotFoundException, IOException {
		this.filePath = fileName;
		this.open();
	}

	/**
	 * Abre o banco de dados a partir do arquivo especificado, inicializando
	 * os metadados e o cabeçalho do banco de dados.
	 * <p>
	 * Se o arquivo já contiver dados, o banco será carregado e os metadados
	 * serão lidos. Caso contrário, será gerado um novo UUID e o cabeçalho
	 * será inicializado com valores padrão.
	 *
	 * @throws IOException Se ocorrer um erro ao abrir o arquivo ou ler os dados.
	 */
	public void open() throws IOException {
		file = new RandomAccessFile(filePath, "rw");
		file.seek(0);

		// Tenta ler os metadados do arquivo
		try {
			long major = file.readLong();
			long minor = file.readLong();
			uuid = new UUID(major, minor);
			lastId = file.readInt();
			flags = file.readLong();
			numTracks = file.readInt();
			numSpaces = file.readInt();
		} catch (EOFException e) {
			// Caso o arquivo esteja vazio, inicializa os valores
			uuid = UUID.randomUUID();
			lastId = 0;
			flags = 0;
			setOrdered(true); // Arquivo vazio está ordenado
			numTracks = 0;
			numSpaces = 0;
			updateHeader();
		}

		if (isBTreeIndex())
			index = new BTree(filePath + ".BTree");
		else if (isHashIndex())
			index = new HashTableIndex(
					filePath + ".buckets", filePath + ".dir", filePath + ".buckets.meta");

		if (isInvertedIndex()) {
			nameIndex = new InvertedListIndex(filePath + ".name.list.dir", filePath + ".name.list.blocks");
			albumIndex = new InvertedListIndex(
					filePath + ".album.list.dir", filePath + ".album.list.blocks");
			artistIndex = new InvertedListIndex(
					filePath + ".artist.list.dir", filePath + ".artist.list.blocks");
		}
	}

	/**
	 * Fecha o banco de dados, liberando todos os recursos utilizados.
	 *
	 * @throws IOException Se ocorrer um erro ao fechar o arquivo.
	 */
	public void close() throws IOException {
		file.close();
		index = null;
	}

	/**
	 * Adiciona uma nova faixa ao banco de dados.
	 * O ID da faixa é atribuído automaticamente com base no último ID inserido.
	 *
	 * @param track A faixa a ser adicionada.
	 * @return O ID da faixa inserida.
	 * @throws IOException Se ocorrer um erro de leitura ou escrita no arquivo.
	 */
	public int create(Track track) throws IOException {
		lastId += 1;
		track.id = lastId;

		if (index != null)
			index.insert(lastId, file.length());

		if (isInvertedIndex())
			insertInvertedIndexes(track);

		return append(track);
	}

	/**
	 * Adiciona uma faixa ao final do banco de dados, no arquivo binário.
	 * A faixa é escrita no formato binário, incluindo o estado de lápide e o
	 * tamanho.
	 *
	 * @param track A faixa a ser adicionada.
	 * @return O ID da faixa adicionada.
	 * @throws IOException Se ocorrer um erro de leitura ou escrita no arquivo.
	 */
	protected int append(Track track) throws IOException {
		numTracks += 1;
		numSpaces += 1;

		BinaryTrackWriter btw = new BinaryTrackWriter(track);
		file.seek(file.length()); // Move o ponteiro do arquivo para o final
		file.writeBoolean(btw.isTombstone()); // Marca como lápide
		file.writeInt(btw.getSize()); // Escreve o tamanho do registro
		file.write(btw.getStream().toByteArray()); // Escreve os dados binários da faixa

		updateHeader(); // Atualiza os metadados no cabeçalho

		return track.id;
	}

	/**
	 * Lê uma faixa do banco de dados pelo seu ID.
	 * Caso o banco de dados esteja desordenado, a busca é feita por todo o arquivo.
	 * Caso contrário, o método verifica rapidamente com um iterador.
	 *
	 * @param id O ID da faixa a ser lida.
	 * @return A faixa correspondente ao ID, ou {@code null} se não encontrada.
	 * @throws IOException Se ocorrer um erro de leitura no arquivo.
	 */
	public Track read(int id) throws IOException {
		if (index != null) {
			long pos = index.search(id);
			if (pos < 0)
				return null;

			file.seek(index.search(id));
			try {
				return nextTrack();
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Falha ao obter próxima Track", e);
			}
		}

		// Se está desordenado, fazemos a busca pelo arquivo completo.
		if (!isOrdered())
			return readFirst(Track.Field.ID, id);

		// Caso contrário, podemos parar mais cedo.
		for (Track t : this) {
			if (t.getId() > id)
				return null;
			else if (t.getId() == id)
				return t;
		}

		return null;
	}

	/**
	 * Lê a primeira faixa que corresponde ao valor de um campo específico.
	 * A busca é feita pelo arquivo a partir do cabeçalho.
	 *
	 * @param field O campo da faixa a ser comparado.
	 * @param value O valor que o campo deve ter.
	 * @return A primeira faixa que corresponde ao campo e valor especificados, ou
	 *         {@code null} se não encontrada.
	 * @throws IOException Se ocorrer um erro de leitura no arquivo.
	 */
	public Track readFirst(Track.Field field, Object value) throws IOException {
		file.seek(HEADER_SIZE); // Posiciona o cursor no primeiro registro.

		for (Track t : this)
			if (t.matchesField(field, value)) // Verifica se a faixa corresponde ao campo e valor
				return t;

		return null;
	}

	/**
	 * Atualiza uma faixa no banco de dados, identificada pelo seu ID.
	 * Se o tamanho da nova faixa for menor ou igual ao da faixa existente, ela
	 * sobrescreve o registro. Caso contrário, o registro antigo é marcado como
	 * lápide e o novo é adicionado no final.
	 *
	 * @param id      O ID da faixa a ser atualizada.
	 * @param updated A faixa com os dados atualizados.
	 * @throws IOException            Se ocorrer um erro de leitura ou escrita no
	 *                                arquivo.
	 * @throws NoSuchElementException Se não houver uma faixa com o ID fornecido.
	 */
	public void update(int id, Track updated) throws IOException {
		Track oldTrack;

		if ((oldTrack = read(id)) == null)
			throw new NoSuchElementException("Não há elemento com ID " + id);

		updated.setId(id);

		if (isInvertedIndex()) {
			deleteInvertedIndexes(oldTrack);
			insertInvertedIndexes(updated);
		}

		file.seek(lastBinaryTrackPos);
		file.skipBytes(1); // Pula a lápide, pois .read() já validou o registro.
		int oldSize = file.readInt(); // Lê o tamanho do registro antigo
		BinaryTrackWriter writer = new BinaryTrackWriter(updated);

		// Verifica se o registro atualizado é menor ou igual ao anterior
		if (writer.getSize() <= oldSize) {
			// Volta para o começo do registro para sobrescrevê-lo
			file.seek(lastBinaryTrackPos);
			file.writeBoolean(writer.isTombstone());
			file.writeInt(oldSize);
		} else {
			// Remove e reinsere no índice.
			if (index != null) {
				index.delete(id);
				index.insert(id, file.length());
			}

			// Seta a lápide do registro
			file.seek(lastBinaryTrackPos);
			file.writeBoolean(true);
			// Pula para o final do arquivo, para inserir o registro no final
			file.seek(file.length());
			file.writeBoolean(writer.isTombstone());
			file.writeInt(writer.getSize());

			// Indica que o arquivo está agora desordenado.
			setOrdered(false);

			// Atualiza o cabeçalho.
			numSpaces += 1;
			updateHeader();
		}

		// Escreve o novo registro.
		file.write(writer.getStream().toByteArray());
	}

	/**
	 * Deleta uma faixa do banco de dados, identificada pelo seu ID.
	 * O registro da faixa é marcado como lápide e o número de faixas é
	 * decrementado.
	 *
	 * @param id O ID da faixa a ser deletada.
	 * @throws IOException            Se ocorrer um erro de leitura ou escrita no
	 *                                arquivo.
	 * @throws NoSuchElementException Se não houver uma faixa com o ID fornecido.
	 */
	public void delete(int id) throws IOException {
		Track deletedTrack;

		if ((deletedTrack = read(id)) == null)
			throw new NoSuchElementException("Não há elemento com ID " + id);

		file.seek(lastBinaryTrackPos); // Volta para o começo do registro
		file.writeBoolean(true); // Marca como lápide

		if (index != null)
			index.delete(id);

		if (isInvertedIndex())
			deleteInvertedIndexes(deletedTrack);

		numTracks -= 1; // Decrementa o contador de faixas.
		updateHeader(); // Atualiza o cabeçalho.
	}

	/**
	 * Deleta faixas do banco de dados que correspondem a um valor específico de
	 * campo. Todos os registros correspondentes são marcados como lápide.
	 *
	 * @param field O campo da faixa a ser comparado.
	 * @param value O valor que o campo deve ter para que a faixa seja deletada.
	 * @throws IOException Se ocorrer um erro de leitura ou escrita no arquivo.
	 */
	public void delete(Track.Field field, Object value) throws IOException {
		file.seek(HEADER_SIZE); // Posiciona o cursor no primeiro registro.

		for (Track t : this) {
			if (t.matchesField(field, value)) {
				long pos = file.getFilePointer(); // Salva a posição atual.
				file.seek(lastBinaryTrackPos); // Volta para o começo do registro.
				file.writeBoolean(true); // Marca como lápide.
				file.seek(pos); // Retorna para a posição salva.
				numTracks -= 1; // Decrementa o contador de faixas.
			}
		}

		// Atualiza o cabeçalho.
		updateHeader();
	}

	/**
	 * Ordena as faixas do banco de dados utilizando o algoritmo de ordenação
	 * Balanced Merge Sort (intercalação balanceada).
	 * O método permite configurar o fanout (quantidade de faixas que podem ser
	 * processadas por vez) e o número máximo de nós da heap para a ordenação.
	 *
	 * @param fanout       Número de faixas processadas em cada fase do algoritmo de
	 *                     ordenação.
	 * @param maxHeapNodes Número máximo de nós da heap para a ordenação.
	 * @throws IOException Se ocorrer um erro de leitura ou escrita no arquivo
	 *                     durante a ordenação.
	 */
	public void sort(int fanout, int maxHeapNodes) throws IOException {
		new BalancedMergeSort(this, fanout, maxHeapNodes).sort();
	}

	/**
	 * Ordena as faixas do banco de dados utilizando o algoritmo de ordenação
	 * Balanced Merge Sort com parâmetros padrão (sem configuração de fanout e
	 * maxHeapNodes).
	 *
	 * @throws IOException Se ocorrer um erro de leitura ou escrita no arquivo
	 *                     durante a ordenação.
	 */
	public void sort() throws IOException {
		new BalancedMergeSort(this).sort();
	}

	/**
	 * Imprime um sumário da faixa do banco de dados correspondente ao ID fornecido.
	 *
	 * @param id O ID da faixa a ser impressa.
	 * @throws IOException Se ocorrer um erro de leitura no arquivo.
	 */
	public void print(int id) throws IOException {
		print(Track.Field.ID, id);
	}

	/**
	 * Imprime um sumário de todas as faixas do banco de dados que correspondem a um
	 * valor específico de campo.
	 *
	 * @param field O campo da faixa a ser comparado.
	 * @param value O valor que o campo deve ter para que a faixa seja impressa.
	 * @throws IOException Se ocorrer um erro de leitura no arquivo.
	 */
	public void print(Track.Field field, Object value) throws IOException {
		file.seek(HEADER_SIZE); // Posiciona o cursor no primeiro registro.

		for (Track t : this) {
			if (t.matchesField(field, value)) // Verifica se a faixa corresponde ao campo e valor
				System.out.println(t); // Imprime a faixa correspondente
		}
	}

	/**
	 * Imprime todas as faixas do banco de dados.
	 *
	 * @throws IOException Se ocorrer um erro de leitura no arquivo.
	 */
	public void printAll() throws IOException {
		file.seek(HEADER_SIZE); // Posiciona o cursor no primeiro registro.

		for (Track t : this)
			System.out.println(t); // Imprime cada faixa do banco de dados
	}

	/**
	 * Retorna a próxima faixa válida do banco de dados, considerando o filtro de
	 * busca, se houver. Se o filtro de busca estiver definido, ele irá procurar a
	 * próxima faixa que corresponda ao campo e valor fornecidos. Caso contrário,
	 * ele retorna a próxima faixa válida, ignorando as lápides.
	 *
	 * @return A próxima faixa válida, ou {@code null} se não houver mais faixas
	 *         válidas.
	 * @throws EOFException           Se o fim do arquivo for alcançado.
	 * @throws IOException            Se ocorrer um erro de leitura no arquivo.
	 * @throws ClassNotFoundException Se ocorrer um erro ao ler a classe da faixa.
	 */
	protected Track nextTrack() throws EOFException, IOException, ClassNotFoundException {
		if (searchFilter == null)
			return nextValidBinaryTrackReader().getTrack();

		Track result;

		do
			result = nextValidBinaryTrackReader().getTrack();
		while (!(result.matchesField(searchFilter.searchField, searchFilter.searchValue)));

		return result;
	}

	/**
	 * Retorna o próximo leitor de faixa válido do banco de dados.
	 * Um leitor de faixa válido é aquele que não é uma lápide (registro deletado).
	 * O método continua buscando até encontrar um leitor de faixa válido.
	 *
	 * @return Um {@link BinaryTrackReader} para a próxima faixa válida.
	 * @throws EOFException Se o fim do arquivo for alcançado.
	 * @throws IOException  Se ocorrer um erro de leitura no arquivo.
	 */
	private BinaryTrackReader nextValidBinaryTrackReader() throws EOFException, IOException {
		BinaryTrackReader result;

		do
			result = nextBinaryTrackReader();
		while (result == null);

		return result;
	}

	/**
	 * Retorna o próximo leitor de faixa, independentemente de ser válido ou não
	 * (pode ser uma lápide). Este método lê o próximo registro do arquivo, mas não
	 * verifica se ele é uma faixa válida.
	 *
	 * @return Um {@link BinaryTrackReader} para a próxima faixa, ou {@code null} se
	 *         o registro for inválido (lápide).
	 * @throws EOFException Se o fim do arquivo for alcançado.
	 * @throws IOException  Se ocorrer um erro de leitura no arquivo.
	 */
	private BinaryTrackReader nextBinaryTrackReader() throws EOFException, IOException {
		lastBinaryTrackPos = file.getFilePointer();
		boolean tombstone = file.readBoolean();
		int size = file.readInt();

		if (tombstone) {
			file.skipBytes(size); // Pula os dados da lápide.
			return null;
		} else {
			byte[] buf = new byte[size];
			file.read(buf);
			return new BinaryTrackReader(false, size, new ByteArrayInputStream(buf));
		}
	}

	/**
	 * Retorna o ponteiro do arquivo ao início do segmento atual durante a
	 * intercalação balanceada, necessário para que o iterador não descarte o
	 * primeiro registro do próximo segmento
	 *
	 * @throws IOException Se ocorrer um erro ao posicionar o ponteiro no arquivo.
	 */
	protected void returnToSegmentStart() throws IOException {
		file.seek(segmentStart);
	}

	/**
	 * Trunca o arquivo, removendo todos os dados, exceto o UUID.
	 * Este método apaga todos os registros e reinicia os contadores do banco de
	 * dados mantendo apenas o identificador único do banco de dados.
	 *
	 * @throws IOException Se ocorrer um erro ao truncar o arquivo.
	 */
	protected void truncate() throws IOException {
		boolean isOpened = (file != null);
		if (!isOpened)
			open();

		file.getChannel().truncate(0); // Trunca o arquivo para o início.
		lastId = numTracks = numSpaces = 0; // Reseta os contadores.
		updateHeader(); // Atualiza o cabeçalho.

		if (!isOpened)
			close();
	}

	/**
	 * Verifica se o ponteiro do arquivo chegou ao final.
	 * Este método verifica se a posição do ponteiro de leitura no arquivo é igual
	 * ao comprimento do arquivo, indicando que não há mais dados para serem lidos.
	 *
	 * @return {@code true} se o ponteiro de leitura estiver no final do arquivo,
	 *         {@code false} caso contrário.
	 */
	public boolean isFinished() {
		try {
			return file.getFilePointer() == file.length();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Problema ao verificar se o arquivo acabou.");
		}
	}

	/**
	 * Atualiza o cabeçalho do banco de dados no arquivo, incluindo informações como
	 * o UUID, o último ID, as flags e o número de faixas e espaços. Este método
	 * sobrescreve as informações do cabeçalho no início do arquivo.
	 *
	 * @throws IOException Se ocorrer um erro de leitura ou escrita ao atualizar o
	 *                     cabeçalho no
	 *                     arquivo.
	 */
	protected void updateHeader() throws IOException {
		long pos = file.getFilePointer();
		file.seek(0);
		file.writeLong(uuid.getMostSignificantBits());
		file.writeLong(uuid.getLeastSignificantBits());
		file.writeInt(lastId);
		file.writeLong(flags);
		file.writeInt(numTracks);
		file.writeInt(numSpaces);
		file.seek(pos);
	}

	/**
	 * Define o filtro de busca a ser utilizado para operações de pesquisa.
	 * Este filtro pode ser usado para buscar faixas que correspondam a um campo e
	 * valor específicos.
	 *
	 * @param searchFilter O filtro de busca a ser aplicado.
	 */
	public void setFilter(TrackFilter searchFilter) {
		this.searchFilter = searchFilter;
	}

	/**
	 * Define o filtro de busca baseado em um campo e um valor específicos.
	 *
	 * @param field O campo da faixa a ser filtrado.
	 * @param value O valor que o campo deve ter para que a faixa seja retornada.
	 */
	public void setFilter(Track.Field field, Object value) {
		setFilter(new TrackFilter(field, value));
	}

	/**
	 * Retorna o filtro de busca atual.
	 *
	 * @return O filtro de busca atualmente definido, ou {@code null} se não houver
	 *         filtro.
	 */
	public TrackFilter getFilter() {
		return searchFilter;
	}

	/**
	 * Limpa o filtro de busca atual.
	 * Após chamar este método, nenhum filtro será aplicado nas buscas.
	 */
	public void clearFilter() {
		searchFilter = null;
	}

	/**
	 * Classe interna que representa um filtro de busca para faixas.
	 * O filtro consiste em um campo da faixa e um valor a ser comparado.
	 */
	static public class TrackFilter {
		public Track.Field searchField;
		public Object searchValue;

		/**
		 * Construtor para criar um filtro de busca com um campo e um valor específicos.
		 *
		 * @param searchField O campo da faixa a ser filtrado.
		 * @param searchValue O valor que o campo deve ter para que a faixa seja
		 *                    retornada.
		 * @throws InvalidParameterException Se o campo ou o valor fornecido for
		 *                                   {@code null}.
		 */
		public TrackFilter(Field searchField, Object searchValue) {
			if (searchField == null || searchValue == null)
				throw new InvalidParameterException("Os valores devem ser não-nulos");

			this.searchField = searchField;
			this.searchValue = searchValue;
		}
	}

	/**
	 * Retorna um iterador para percorrer as faixas armazenadas no banco de dados.
	 * O iterador percorre as faixas em sequência, respeitando a ordem de leitura
	 * dos registros no arquivo. Durante a iteração, as faixas que correspondem ao
	 * filtro de busca (se definido) serão retornadas.
	 *
	 * @return Um iterador para as faixas armazenadas no banco de dados.
	 * @throws RuntimeException Se houver um erro ao posicionar o ponteiro no
	 *                          primeiro registro ou
	 *                          ao obter a próxima faixa.
	 */
	@Override
	public Iterator<Track> iterator() throws RuntimeException {
		try {
			// Posiciona o ponteiro de leitura no início do segmento de dados.
			file.seek(HEADER_SIZE);
		} catch (IOException e) {
			throw new RuntimeException("Erro ao posicionar cursor no primeiro registro");
		}

		return new Iterator<>() {
			private Track currentTrack = null;

			/**
			 * Verifica se há uma próxima faixa no banco de dados.
			 *
			 * @return {@code true} se houver uma próxima faixa válida a ser retornada,
			 *         correspondente ao filtro aplicado, se houver,
			 *         {@code false} caso contrário.
			 */
			@Override
			public boolean hasNext() {
				try {
					long pos = file.getFilePointer();
					// Tenta ler a próxima faixa. Se for válida, retorna true.
					currentTrack = nextTrack();

					// Verifica se o segmento atual já terminou, útil para a intercalação
					// balanceada.
					if (currentTrack != null)
						segmentFinished = (currentTrack.getId() < lastIteratorId);
					else
						segmentFinished = false;

					// Se o segmento terminou, salva o início do próximo segmento.
					if (segmentFinished)
						segmentStart = pos;

					return currentTrack != null;
				} catch (EOFException e) {
					return false; // Se chegar ao fim do arquivo, não há mais faixas.
				} catch (IOException | ClassNotFoundException e) {
					throw new RuntimeException("Falha ao obter próxima Track", e);
				}
			}

			/**
			 * Retorna a próxima faixa no banco de dados.
			 *
			 * @return A próxima faixa válida, correspondente ao filtro aplicado, se houver.
			 * @throws NoSuchElementException Se não houver mais faixas para iterar.
			 * @throws RuntimeException       Se ocorrer um erro ao obter a próxima faixa.
			 */
			@Override
			public Track next() throws RuntimeException {
				if (currentTrack == null)
					throw new NoSuchElementException("TrackDB chegou ao fim");

				Track track = currentTrack;
				lastIteratorId = track.getId(); // Atualiza o último ID encontrado.
				currentTrack = null; // Reset para null até o próximo hasNext()
				return track;
			}
		};
	}

	// Getters & Setters.

	public UUID getUUID() {
		return uuid;
	}

	public int getLastId() {
		return lastId;
	}

	public void setLastId(int lastId) throws IOException {
		this.lastId = lastId;
		updateHeader();
	}

	public int getNumTracks() {
		return numTracks;
	}

	public int getNumSpaces() {
		return numSpaces;
	}

	public String getFilePath() {
		return filePath;
	}

	public boolean isSegmentFinished() {
		return segmentFinished;
	}

	/**
	 * Verifica se o banco de dados possui um índice.
	 *
	 * @return {@code true} se o banco de dados estiver indexado, {@code false} caso
	 *         contrário.
	 */
	public boolean isIndexed() {
		return index != null;
	}

	/**
	 * Verifica se o banco de dados utiliza um índice do tipo Árvore B.
	 *
	 * @return {@code true} se o índice for do tipo Árvore B, {@code false} caso
	 *         contrário.
	 */
	public boolean isBTreeIndex() {
		return (flags & Flag.INDEXED_BTREE.getBitmask()) != 0;
	}

	/**
	 * Verifica se o banco de dados utiliza um índice do tipo Hash.
	 *
	 * @return {@code true} se o índice for do tipo Hash, {@code false} caso
	 *         contrário.
	 */
	public boolean isHashIndex() {
		return (flags & Flag.INDEXED_HASH.getBitmask()) != 0;
	}

	/**
	 * Verifica se o banco de dados utiliza um índice de Lista Invertida.
	 *
	 * @return {@code true} se o índice for de Lista Invertida, {@code false} caso
	 *         contrário.
	 */
	public boolean isInvertedIndex() {
		return (flags & Flag.INDEXED_INVERSE_LIST.getBitmask()) != 0;
	}

	/**
	 * Configura o uso de um índice do tipo Árvore B no banco de dados.
	 *
	 * @param value {@code true} para habilitar o índice Árvore B, {@code false}
	 *              para desabilitar.
	 * @throws IOException Se ocorrer um erro de leitura ou escrita no arquivo.
	 */
	public void setBTreeIndex(boolean value) throws IOException {
		setBTreeIndex(value, 16);
	}

	/**
	 * Configura o uso de um índice do tipo Árvore B no banco de dados com uma ordem
	 * específica.
	 *
	 * @param value {@code true} para habilitar o índice Árvore B, {@code false}
	 *              para desabilitar.
	 * @param order A ordem da Árvore B.
	 * @throws IOException Se ocorrer um erro de leitura ou escrita no arquivo.
	 */
	public void setBTreeIndex(boolean value, int order) throws IOException {
		if (value) {
			if (isBTreeIndex())
				throw new IllegalStateException("O índice por Árvore B já está habilitado.");
			if (order <= 2)
				throw new InvalidBTreeOrderException(
						order, InvalidBTreeOrderException.Reason.TOO_SMALL);
			if (order % 2 != 0)
				throw new InvalidBTreeOrderException(
						order, InvalidBTreeOrderException.Reason.NOT_EVEN);

			flags |= Flag.INDEXED_BTREE.getBitmask();
			setHashIndex(false);

			index = new BTree(order / 2, filePath + ".BTree");
			for (Track t : this)
				index.insert(t.getId(), lastBinaryTrackPos);
		} else {
			flags &= ~Flag.INDEXED_BTREE.getBitmask();

			if (index != null)
				index.destruct();
			index = null;
		}
		updateHeader();
	}

	public void setHashIndex(boolean value) throws IOException {
		setHashIndex(value, 16);
	}

	public void setHashIndex(boolean value, int bucketCapacity) throws IOException {
		if (value) {
			if (isHashIndex())
				throw new IllegalStateException("O índice por tabela hash já está habilitado.");
			if (bucketCapacity >= Short.MAX_VALUE)
				throw new InvalidHashTableCapacityException(
						bucketCapacity, InvalidHashTableCapacityException.Reason.TOO_LARGE);

			flags |= Flag.INDEXED_HASH.getBitmask();
			setBTreeIndex(false);

			index = new HashTableIndex(bucketCapacity, filePath + ".buckets", filePath + ".dir",
					filePath + ".buckets.meta");
			for (Track t : this)
				index.insert(t.getId(), lastBinaryTrackPos);
		} else {
			flags &= ~Flag.INDEXED_HASH.getBitmask();

			if (index != null)
				index.destruct();
			index = null;
		}
		updateHeader();
	}

	public int[] readInvertedIndexes(String name, String album, String artist) throws IOException {
		int[] matchingName = nameIndex.read(name != null ? name.toLowerCase() : null);
		int[] matchingAlbum = albumIndex.read(album != null ? album.toLowerCase() : null);
		int[] matchingArtist = artistIndex.read(artist != null ? artist.toLowerCase() : null);
		return findIntersection(matchingName, matchingAlbum, matchingArtist);
	}

	/**
	 * Encontra a intersecção de N arrays, podendo ser nulos.
	 */
	private static int[] findIntersection(int[]... arrays) {
		HashSet<Integer> intersectionSet = new HashSet<>();

		// Verifique se há arrays não nulos e adicione o primeiro array não nulo ao
		// conjunto de interseção.
		for (int[] array : arrays)
			if (array != null && intersectionSet.isEmpty())
				for (int num : array)
					intersectionSet.add(num);

		// Para cada array não nulo subsequente, mantenha apenas os elementos que já
		// estão no conjunto de interseção.
		for (int[] array : arrays) {
			if (array != null) {
				HashSet<Integer> currentSet = new HashSet<>();
				for (int num : array)
					if (intersectionSet.contains(num))
						currentSet.add(num);
				intersectionSet.retainAll(currentSet);
			}
		}

		// Converta o conjunto de interseção em um array.
		int[] result = new int[intersectionSet.size()];
		int index = 0;
		for (int num : intersectionSet)
			result[index++] = num;

		return result;
	}

	private static String[][] invertedIndexSplit(Track t) {
		String[] nameParts = Arrays.stream(t.getName().split(" "))
				.map(String::trim)
				.map(String::toLowerCase)
				.filter(s -> s.length() > 3 && s.matches("[\\p{L}]+"))
				.toArray(String[]::new);
		String[] albumParts = Arrays.stream(t.getAlbumName().split(" "))
				.map(String::trim)
				.map(String::toLowerCase)
				.filter(s -> s.length() > 3 && s.matches("[\\p{L}]+"))
				.toArray(String[]::new);
		List<String> artistPartsList = new ArrayList<>();
		for (String artist : t.getTrackArtists()) {
			String[] parts = artist.split(" ");
			artistPartsList.addAll(Arrays.asList(parts));
		}
		String[] artistParts = artistPartsList.stream()
				.map(String::trim)
				.map(String::toLowerCase)
				.filter(s -> s.length() > 3 && s.matches("[\\p{L}]+"))
				.toArray(String[]::new);
		String[][] res = new String[3][];
		res[0] = nameParts;
		res[1] = albumParts;
		res[2] = artistParts;
		return res;
	}

	private void insertInvertedIndexes(Track t) throws IOException {
		int id = t.getId();
		String[][] parts = invertedIndexSplit(t);
		for (String s : parts[0])
			nameIndex.create(s, id);
		for (String s : parts[1])
			albumIndex.create(s, id);
		for (String s : parts[2])
			artistIndex.create(s, id);
	}

	private void deleteInvertedIndexes(Track t) throws IOException {
		int id = t.getId();
		String[][] parts = invertedIndexSplit(t);
		for (String s : parts[0])
			nameIndex.delete(s, id);
		for (String s : parts[1])
			albumIndex.delete(s, id);
		for (String s : parts[2])
			artistIndex.delete(s, id);
	}

	public void setInvertedIndex(boolean value) throws IOException {
		if (value) {
			if (isInvertedIndex())
				throw new IllegalStateException(
						"Os índices por listas invertidas já estão habilitados.");

			flags |= Flag.INDEXED_INVERSE_LIST.getBitmask();

			nameIndex = new InvertedListIndex(filePath + ".name.list.dir", filePath + ".name.list.blocks");
			albumIndex = new InvertedListIndex(
					filePath + ".album.list.dir", filePath + ".album.list.blocks");
			artistIndex = new InvertedListIndex(
					filePath + ".artist.list.dir", filePath + ".artist.list.blocks");
			int i = 0;
			for (Track t : this) {
				System.err.println(
						"Inserindo elemento " + ++i + "/" + numTracks + "\tID: " + t.getId());
				insertInvertedIndexes(t);
			}
		} else {
			flags &= ~Flag.INDEXED_INVERSE_LIST.getBitmask();

			if (nameIndex != null) {
				nameIndex.destruct();
				albumIndex.destruct();
				artistIndex.destruct();
			}
			nameIndex = albumIndex = artistIndex = null;
		}
		updateHeader();
	}

	/**
	 * Desabilita todos os índices no banco de dados.
	 *
	 * @throws IOException Se ocorrer um erro de leitura ou escrita no arquivo.
	 */
	public void disableIndex() throws IOException {
		if ((flags
				& (Flag.INDEXED_BTREE.getBitmask() | Flag.INDEXED_HASH.getBitmask()
						| Flag.INDEXED_INVERSE_LIST.getBitmask())) == 0)
			throw new IllegalStateException("Nenhum índice está habilitado.");

		setBTreeIndex(false);
		setHashIndex(false);
		setInvertedIndex(false);
	}

	/**
	 * Reindexa o banco de dados, recriando o índice atual.
	 *
	 * @throws IOException Se ocorrer um erro de leitura ou escrita no arquivo.
	 */
	public void reindex() throws IOException {
		if ((flags
				& (Flag.INDEXED_BTREE.getBitmask() | Flag.INDEXED_HASH.getBitmask()
						| Flag.INDEXED_INVERSE_LIST.getBitmask())) == 0)
			throw new IllegalStateException("Nenhum índice está habilitado.");

		if (isBTreeIndex()) {
			if (!(index instanceof BTree))
				throw new AssertionError("Índice tem tipo inválido!");

			int saveOrder = ((BTree) index).getHalfPageCapacity();
			index.destruct();
			index = new BTree(saveOrder, filePath + ".BTree");

			for (Track t : this)
				index.insert(t.getId(), lastBinaryTrackPos);

		} else if (isHashIndex()) {
			if (!(index instanceof HashTableIndex))
				throw new AssertionError("Índice tem tipo inválido!");

			int saveCapacity = ((HashTableIndex) index).getBucketCapacity();
			index.destruct();
			index = new HashTableIndex(
					saveCapacity, filePath + ".buckets", filePath + ".dir", filePath + ".buckets.meta");

			for (Track t : this)
				index.insert(t.getId(), lastBinaryTrackPos);
		}

		// if (isInvertedIndex()) {
	}

	public boolean isOrdered() {
		return (flags & Flag.ORDERED.getBitmask()) != 0;
	}

	public void setOrdered(boolean value) throws IOException {
		flags = value ? (flags | Flag.ORDERED.getBitmask()) : (flags & ~Flag.ORDERED.getBitmask());
		updateHeader();
	}
}

/**
 * Classe abstrata que representa uma faixa binária no banco de dados.
 * Contém informações básicas sobre a faixa, como o estado de "tombstone"
 * (lápide) e o tamanho dos dados.
 *
 * @see BinaryTrackReader
 * @see BinaryTrackWriter
 */
abstract class BinaryTrack {
	protected boolean tombstone;
	protected int size;

	/**
	 * Verifica se a faixa é uma lápide (registro excluído).
	 *
	 * @return {@code true} se a faixa for uma lápide, {@code false} caso contrário.
	 */
	public boolean isTombstone() {
		return tombstone;
	}

	/**
	 * Define se a faixa é uma lápide (registro excluído).
	 *
	 * @param tombstone O estado da lápide a ser definido.
	 */
	public void setTombstone(boolean tombstone) {
		this.tombstone = tombstone;
	}

	/**
	 * Obtém o tamanho dos dados binários da faixa.
	 *
	 * @return O tamanho dos dados binários da faixa.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Define o tamanho dos dados binários da faixa.
	 *
	 * @param size O tamanho dos dados binários da faixa a ser definido.
	 */
	public void setSize(int size) {
		this.size = size;
	}
}

/**
 * Classe auxiliar para leitura de faixas binárias a partir de um fluxo de
 * entrada. Esta classe herda de {@link BinaryTrack} e é usada para ler os dados
 * binários de uma faixa, além de desserializar os dados para um objeto
 * {@link Track}.
 *
 * @see BinaryTrack
 */
class BinaryTrackReader extends BinaryTrack {
	protected ByteArrayInputStream stream;
	private Track track;

	/**
	 * Construtor para criar um leitor de faixa binária.
	 *
	 * @param tombstone Indica se o registro é uma lápide (excluído).
	 * @param size      O tamanho dos dados binários da faixa.
	 * @param stream    O fluxo de entrada com os dados da faixa.
	 */
	public BinaryTrackReader(boolean tombstone, int size, ByteArrayInputStream stream) {
		track = null;
		this.tombstone = tombstone;
		this.size = size;
		this.stream = stream;
	}

	/**
	 * Lê os dados da faixa e desserializa para um objeto {@link Track}.
	 *
	 * @throws IOException Se ocorrer um erro durante a leitura dos
	 *                     dados.
	 */
	private void readTrack() throws IOException {
		track = new Track();
		track.readExternal(new ObjectInputStream(stream));
	}

	/**
	 * Obtém o fluxo de entrada que contém os dados da faixa.
	 *
	 * @return O fluxo de entrada dos dados da faixa.
	 */
	public ByteArrayInputStream getStream() {
		return stream;
	}

	/**
	 * Define o fluxo de entrada que contém os dados da faixa.
	 *
	 * @param stream O fluxo de entrada dos dados da faixa.
	 */
	public void setStream(ByteArrayInputStream stream) {
		this.stream = stream;
	}

	/**
	 * Retorna a faixa desserializada a partir do fluxo de entrada.
	 *
	 * @return A faixa desserializada.
	 * @throws IOException Se ocorrer um erro ao ler os dados da faixa.
	 */
	public Track getTrack() throws IOException {
		if (track == null)
			readTrack();

		stream = null;
		return track;
	}

	/**
	 * Define a faixa a ser associada a este leitor binário.
	 *
	 * @param track A faixa a ser definida.
	 */
	public void setTrack(Track track) {
		this.track = track;
	}
}

/**
 * Classe auxiliar para escrita de faixas binárias em um fluxo de saída.
 * Esta classe herda de {@link BinaryTrack} e é usada para escrever os dados
 * binários de uma faixa em um fluxo de saída, serializando o objeto
 * {@link Track}.
 *
 * @see BinaryTrack
 */
class BinaryTrackWriter extends BinaryTrack {
	protected ByteArrayOutputStream stream;

	/**
	 * Construtor para criar um escritor de faixa binária a partir de um objeto
	 * {@link Track}.
	 *
	 * @param track O objeto {@link Track} a ser escrito.
	 * @throws IOException Se ocorrer um erro durante a serialização da faixa.
	 */
	public BinaryTrackWriter(Track track) throws IOException {
		stream = new ByteArrayOutputStream();

		try (ObjectOutputStream objOutStream = new ObjectOutputStream(stream)) {
			track.writeExternal(objOutStream);
		}

		tombstone = false;
		size = stream.size();
	}

	/**
	 * Obtém o fluxo de saída que contém os dados binários da faixa.
	 *
	 * @return O fluxo de saída com os dados binários da faixa.
	 */
	public ByteArrayOutputStream getStream() {
		return stream;
	}

	/**
	 * Define o fluxo de saída que contém os dados binários da faixa.
	 *
	 * @param stream O fluxo de saída com os dados binários da faixa.
	 */
	public void setStream(ByteArrayOutputStream stream) {
		this.stream = stream;
	}
}

/**
 * Enumeração que define as flags (marcas) utilizadas no cabeçalho do arquivo de
 * banco de dados. As flags são representadas por um valor de bitmask e são
 * usadas para indicar diferentes propriedades ou estados do arquivo de banco de
 * dados.
 * <p>
 * No momento, há apenas uma flag disponível:
 * - {@link Flag#ORDERED}: Indica que os registros no banco de dados estão
 * sequencialmente ordenados por ID.
 * <p>
 * A utilização de bitmasks permite que novas flags sejam adicionadas no futuro,
 * se necessário, sem impactar a estrutura existente do banco de dados.
 */
enum Flag {
	/**
	 * Indica que o arquivo de banco de dados está ordenado, ou seja, os registros
	 * estão armazenados de maneira sequencial, ordenados por ID. Esta flag é usada
	 * para otimizar operações de busca e escrita.
	 * <p>
	 * O valor de bitmask associado a essa flag é {@code 1L << 0}.
	 */
	ORDERED(1L), // Indica se o arquivo está ordenado.

	/**
	 * Indica que o banco de dados utiliza um índice do tipo Árvore B.
	 * Esta flag é usada para otimizar operações de busca e escrita.
	 * <p>
	 * O valor de bitmask associado a essa flag é {@code 1L << 1}.
	 */
	INDEXED_BTREE(1L << 1),

	/**
	 * Indica que o banco de dados utiliza um índice do tipo Hash Dinâmica.
	 * Esta flag é usada para otimizar operações de busca e escrita.
	 * <p>
	 * O valor de bitmask associado a essa flag é {@code 1L << 2}.
	 */
	INDEXED_HASH(1L << 2),

	/**
	 * Indica que o banco de dados utiliza um índice de Lista Invertida.
	 * Esta flag é usada para otimizar operações de busca e escrita.
	 * <p>
	 * O valor de bitmask associado a essa flag é {@code 1L << 3}.
	 */
	INDEXED_INVERSE_LIST(1L << 3);

	private final long bitmask;

	/**
	 * Construtor para associar um valor de bitmask à flag.
	 *
	 * @param bitmask O valor de bitmask que representa a flag.
	 */
	Flag(long bitmask) {
		this.bitmask = bitmask;
	}

	/**
	 * Retorna o valor de bitmask associado a essa flag.
	 *
	 * @return O valor de bitmask da flag.
	 */
	public long getBitmask() {
		return bitmask;
	}
}
