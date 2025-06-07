package AEDs3.DataBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Classe responsável por realizar a ordenação externa por intercalação
 * balanceada. Este algoritmo divide os dados em segmentos menores e utiliza
 * uma estrutura de heap (fila de prioridade) para realizar a intercalação
 * eficiente de grandes quantidades de dados que não cabem na memória RAM de uma
 * vez. A ordenação é realizada usando 2N arquivos temporários e o algoritmo de
 * intercalação balanceada.
 * <p>
 * A classe utiliza a classe {@link TrackDB} para ler e gravar os registros de
 * faixas de música, realizando a ordenação dos dados através de múltiplos
 * arquivos temporários.
 * <p>
 * A ordenação ocorre em duas fases:
 * 1. Distribuição inicial dos registros em 2N arquivos temporários.
 * 2. Intercalação dos arquivos temporários, ordenando os dados e escrevendo-os
 * de volta no banco de dados.
 */
public class BalancedMergeSort {
	/** Banco de dados de faixas de música (TrackDB) a ser ordenado. */
	TrackDB db;

	/** Arquivos temporários onde os dados são armazenados durante a ordenação. */
	TrackDB[] files;

	/** Número de caminhos (número de arquivos será o dobro). */
	int fanout;

	/** Número máximo de registros a armazenar no heap durante a intercalação. */
	int maxHeapNodes;

	/**
	 * Indica se estamos intercalando segmentos do grupo A (primeiro conjunto de N
	 * arquivos) para o grupo B (segundo grupo) ou vice-versa.
	 */
	boolean mergingFromFirstGroup;

	/**
	 * Indica se deve exibir em `System.err` os passos da execução durante a
	 * ordenação.
	 */
	boolean verbose;

	/**
	 * Construtor que inicializa a ordenação com os parâmetros padrão.
	 *
	 * @param db O banco de dados a ser ordenado.
	 */
	public BalancedMergeSort(TrackDB db) {
		this(db, 8, 64);
	}

	/**
	 * Construtor que inicializa a ordenação com parâmetros personalizados.
	 *
	 * @param db           O banco de dados a ser ordenado.
	 * @param fanout       O número de caminhos N, onde o número de arquivos
	 *                     temporários será 2N.
	 * @param maxHeapNodes O número máximo de registros a armazenar no heap.
	 */
	public BalancedMergeSort(TrackDB db, int fanout, int maxHeapNodes) {
		if (fanout > maxHeapNodes)
			throw new IllegalArgumentException("maxHeapNodes deve ser maior que fanout.");

		this.db = db;
		this.fanout = fanout;
		this.maxHeapNodes = maxHeapNodes;
		this.mergingFromFirstGroup = true; // Começamos no grupo A (arquivos 0–(N - 1)).
		this.verbose = false;
		files = new TrackDB[fanout * 2];
	}

	/**
	 * Inicia o processo de ordenação do banco de dados utilizando o algoritmo de
	 * intercalação balanceada. O processo é feito em duas fases: distribuição dos
	 * registros e intercalação dos segmentos.
	 * <p>
	 * Após a ordenação, o banco de dados original é substituído pelos dados
	 * ordenados, e quaisquer índices serão reconstruídos.
	 *
	 * @throws IOException Se ocorrer algum erro de entrada/saída durante a
	 *                     execução.
	 */
	public void sort() throws IOException {
		// Faz a distribuição inicial dos segmentos em N caminhos usando heap.
		distribute();

		// Itera até o método `intercalar` retornar o BD ordenado.
		TrackDB ordenado;
		do
			ordenado = merge();
		while (ordenado == null);

		// Esvazia o arquivo original e insere os elementos ordenados.
		int saveLastId = db.getLastId();
		db.truncate();
		for (Track t : ordenado)
			db.append(t);
		db.setLastId(saveLastId);
		db.setOrdered(true);

		// Se um índice está presente, é necessário reconstruí-lo.
		if (db.hasPrimaryIndex()) {
			if (verbose) {
				System.err.println("Reindexando arquivo.");
				if (db.getNumTracks() >= 50000)
					System.err.println("O arquivo tem muitos registros. Isso pode demorar...");
			}

			db.reindex();
		}

		// Deleta os arquivos temporários.
		for (int i = 0; i < 2 * fanout; ++i) {
			files[i].close();
			Files.delete(Paths.get(files[i].getFilePath()));
		}
	}

	/**
	 * Distribui os registros do banco de dados em 2N arquivos temporários.
	 * Utiliza um heap (PriorityQueue) para armazenar os elementos e realizar a
	 * distribuição de forma balanceada.
	 *
	 * @throws IOException Se ocorrer um erro de entrada/saída durante a
	 *                     distribuição.
	 */
	@SuppressWarnings("resource") // Não emite aviso por não fecharmos os arquivos temporários aqui.
	private void distribute() throws IOException {
		// Inicializa os arquivos temporários.
		for (int i = 0; i < files.length; ++i)
			files[i] = new TrackDB(db.getFilePath() + ".sort." + String.format("0x%02X", i) + ".bin");

		// Obtém o iterador do BD fonte, para maior controle sobre a inserção no heap.
		Iterator<Track> iterator = db.iterator();
		PriorityQueue<WeightedTrack> heap = new PriorityQueue<>(maxHeapNodes);
		int weight = 0; // Peso inicial.

		// Popula o heap inicialmente.
		while (heap.size() < maxHeapNodes && iterator.hasNext())
			heap.add(new WeightedTrack(iterator.next(), weight));

		// Distribui os elementos do arquivo inicial para os arquivos temporários.
		while (!heap.isEmpty()) {
			// Remove um elemento, armazenando seu ID e atualizando o peso atual.
			WeightedTrack tmp = heap.remove();
			int lastId = tmp.track.getId();
			weight = tmp.weight;

			// Apende o elemento ao BD temporário correto.
			files[weight % fanout].append(tmp.track);

			// Se o arquivo não terminou, adiciona o próximo elemento ao heap.
			if (iterator.hasNext()) {
				tmp = new WeightedTrack(iterator.next(), weight);
				if (tmp.track.getId() < lastId)
					tmp.weight += 1;
				heap.add(tmp);
			}

			if (verbose)
				System.err.println("Distribuindo ID " + lastId + ", peso: " + weight + ", arquivo: "
						+ files[weight % fanout].getFilePath() + ", " + heap.size() + " itens no heap");
		}
	}

	/**
	 * Classe auxiliar que agrupa uma Track com um peso, para uso com o
	 * PriorityQueue.
	 */
	private static class WeightedTrack implements Comparable<WeightedTrack> {
		public final Track track;
		public int weight;

		public WeightedTrack(Track track, int weight) {
			this.track = track;
			this.weight = weight;
		}

		/**
		 * Compara duas instâncias de WeightedTrack primeiro pelo peso e, caso sejam
		 * iguais, pela comparação dos IDs das faixas (Track).
		 *
		 * @param other O outro objeto WeightedTrack a ser comparado.
		 * @return O valor da comparação entre os dois objetos.
		 */
		@Override
		public int compareTo(WeightedTrack other) {
			int cmp = Integer.compare(weight, other.weight);
			cmp = (cmp == 0) ? track.compareTo(other.track) : cmp;
			return cmp;
		}
	}

	/**
	 * Intercala os registros de um grupo de N arquivos temporários no outro grupo.
	 * Este método realiza a intercalação balanceada, utilizando um heap para
	 * encontrar o menor registro entre os arquivos e escrevê-lo no arquivo de
	 * destino.
	 *
	 * @return O banco de dados com os dados ordenados, ou {@code null} se a
	 *         ordenação ainda não estiver completa.
	 * @throws IOException Se ocorrer um erro de entrada/saída durante a
	 *                     intercalação.
	 */
	private TrackDB merge() throws IOException {
		// Listas de BDs de origem e destino.
		List<TrackDB> source = new ArrayList<>(fanout);
		List<TrackDB> destination = new ArrayList<>(fanout);

		// Iteradores de cada BD, aqui por conveniência.
		List<Iterator<Track>> sourceIterators = new ArrayList<>(fanout);

		// Resultado que retornaremos.
		TrackDB result = null;

		// Determina se a fonte e o destino são, respectivamente, os arquivos numerados
		// 0–(N - 1), ou N–(2N - 1).
		int firstSourceId = (mergingFromFirstGroup) ? 0 : fanout;
		int firstDestinationId = fanout - firstSourceId;

		// Popula as listas de fontes e destinos a partir dos arquivos temporários.
		source.addAll(Arrays.asList(files).subList(firstSourceId, 2 * fanout - firstDestinationId));
		destination.addAll(Arrays.asList(files).subList(firstDestinationId, 2 * fanout - firstSourceId));
		for (int i = firstSourceId; i < 2 * fanout - firstDestinationId; ++i)
			sourceIterators.add(files[i].iterator());

		// Heap para encontrar o menor registro.
		PriorityQueue<FileTrack> heap = new PriorityQueue<>(fanout);
		int currentDestination = 0; // Segmento atual.

		// Itera para cada conjunto de segmentos, enquanto pelo menos um arquivo fonte
		// ainda tiver registros.
		while (source.stream().anyMatch(tempFile -> !tempFile.isFinished())) {
			// Popula o heap com o primeiro elemento de cada segmento.
			for (int i = 0; i < fanout; ++i) {
				Iterator<Track> currentSegment = sourceIterators.get(i);
				if (currentSegment.hasNext())
					heap.add(new FileTrack(currentSegment.next(), i));
			}

			if (verbose)
				System.err.println("\rIntercalando segmento " + currentDestination + ", grupo: "
						+ (mergingFromFirstGroup ? 'A' : 'B') + ", arquivo: " + currentDestination % fanout);

			// Itera até esgotarem-se os registros em cada segmento.
			while (!heap.isEmpty()) {
				FileTrack tmp = heap.remove();
				int origin = tmp.origin; // Armazena o arquivo de origem.

				// Se ainda há registros no segmento deste arquivo, adiciona-o ao heap.
				if (sourceIterators.get(origin).hasNext()) {
					if (!source.get(origin).isSegmentFinished())
						heap.add(new FileTrack(sourceIterators.get(origin).next(), origin));
					else
						source.get(origin).returnToSegmentStart();
				}

				// Ao escrever o elemento, salvamos em qual BD ele foi escrito.
				result = destination.get(currentDestination % fanout);
				result.append(tmp.track);
			}

			currentDestination += 1; // Incrementa o segmento.
		}

		// Ao fim, trunca todos os arquivos, deletando os elementos.
		for (TrackDB d : source)
			d.truncate();

		// Inverte o grupo, trocando a direção da intercalação.
		mergingFromFirstGroup = !mergingFromFirstGroup;

		// Retorna o BD com os dados ordenados, se a intercalação estiver completa.
		return (currentDestination == 1) ? result : null;
	}

	/**
	 * Classe auxiliar que agrupa uma Track com o índice do arquivo em que está,
	 * para uso com o PriorityQueue.
	 */
		private record FileTrack(Track track, int origin) implements Comparable<FileTrack> {
		/**
		 * Compara duas instâncias de FileTrack com base no registro de Track.
		 *
		 * @param other O outro objeto FileTrack a ser comparado.
		 * @return O valor da comparação entre os dois objetos.
		 */
			@Override
			public int compareTo(FileTrack other) {
				return track.compareTo(other.track);
			}
		}

	// Getters & Setters.

	/**
	 * Retorna o banco de dados de faixas de música.
	 *
	 * @return O banco de dados de faixas de música.
	 */
	public TrackDB getDb() {
		return db;
	}

	/**
	 * Define o banco de dados de faixas de música.
	 *
	 * @param db O banco de dados de faixas de música a ser definido.
	 */
	public void setDb(TrackDB db) {
		this.db = db;
	}

	/**
	 * Retorna os arquivos temporários utilizados durante a ordenação.
	 *
	 * @return Um array de arquivos temporários.
	 */
	public TrackDB[] getFiles() {
		return files;
	}

	/**
	 * Define os arquivos temporários utilizados durante a ordenação.
	 *
	 * @param files Um array de arquivos temporários a ser definido.
	 */
	public void setFiles(TrackDB[] files) {
		this.files = files;
	}

	/**
	 * Retorna o número de caminhos (fanout) utilizado na ordenação.
	 *
	 * @return O número de caminhos (fanout).
	 */
	public int getFanout() {
		return fanout;
	}

	/**
	 * Define o número de caminhos (fanout) utilizado na ordenação.
	 *
	 * @param fanout O número de caminhos (fanout) a ser definido.
	 */
	public void setFanout(int fanout) {
		this.fanout = fanout;
	}

	/**
	 * Retorna o número máximo de registros a serem armazenados no heap durante a intercalação.
	 *
	 * @return O número máximo de registros no heap.
	 */
	public int getMaxHeapNodes() {
		return maxHeapNodes;
	}

	/**
	 * Define o número máximo de registros a serem armazenados no heap durante a intercalação.
	 *
	 * @param maxHeapNodes O número máximo de registros no heap a ser definido.
	 */
	public void setMaxHeapNodes(int maxHeapNodes) {
		this.maxHeapNodes = maxHeapNodes;
	}

	/**
	 * Indica se a intercalação está ocorrendo do primeiro grupo de arquivos.
	 *
	 * @return {@code true} se a intercalação está ocorrendo do primeiro grupo, caso contrário {@code false}.
	 */
	public boolean isMergingFromFirstGroup() {
		return mergingFromFirstGroup;
	}

	/**
	 * Define se a intercalação deve ocorrer do primeiro grupo de arquivos.
	 *
	 * @param grupo {@code true} para intercalar do primeiro grupo, caso contrário {@code false}.
	 */
	public void setMergingFromFirstGroup(boolean grupo) {
		this.mergingFromFirstGroup = grupo;
	}

	/**
	 * Indica se a execução deve exibir passos detalhados no console.
	 *
	 * @return {@code true} se a execução deve ser detalhada, caso contrário {@code false}.
	 */
	public boolean isVerbose() {
		return verbose;
	}

	/**
	 * Define se a execução deve exibir passos detalhados no console.
	 *
	 * @param verbose {@code true} para exibir passos detalhados, caso contrário {@code false}.
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
}
