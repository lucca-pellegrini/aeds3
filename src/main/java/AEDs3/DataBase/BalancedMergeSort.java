package AEDs3.DataBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

// Ordenação externa por intercalação balanceada, usando 2N arquivos e filas de prioridade nativas.
public class BalancedMergeSort {
	TrackDB db;
	TrackDB[] files;
	int fanout; // Número de caminhos (o número de arquivos será o dobro).
	int maxHeapNodes; // Máximo de registros a se armazenar no Heap.

	// Indica se estamos intercalando segmentos do grupo A no grupo B, ou segmentos
	// do grupo B no grupo A.
	boolean grupo;

	// Indica que devemos printar os passos da execução em stdout.
	boolean verbose;

	public BalancedMergeSort(TrackDB db) {
		this(db, 8, 64);
	}

	// Inicializa o objeto.
	public BalancedMergeSort(TrackDB db, int fanout, int maxHeapNodes) {
		// Fanout deve ser menor, pois, se não, será necessário exceder maxHeapNodes
		// para comparar qual dos elementos é o menor, dentre os próximos dos N
		// segmentos.
		if (fanout > maxHeapNodes)
			throw new IllegalArgumentException("maxHeapNodes deve ser maior que fanout.");

		this.db = db;
		this.fanout = fanout;
		this.maxHeapNodes = maxHeapNodes;
		this.grupo = true; // Começamos no grupo A (arquivos 0–(N - 1)).
		this.verbose = false;
		files = new TrackDB[fanout * 2];
	}

	// Executa a ordenação.
	public void sort() throws IOException {
		// Faz a distribuição inicial dos segmentos em N caminhos usando heap.
		distribuir();

		// Itera até o método `intercalar` retornar o BD ordenado.
		TrackDB ordenado = null;
		do
			ordenado = intercalar();
		while (ordenado == null);

		// Esvazia o arquivo original e insere os elementos ordeandos.
		int saveLastId = db.getLastId();
		db.truncate();
		for (Track t : ordenado)
			db.append(t);
		db.setLastId(saveLastId);
		db.setOrdered(true);

		// Deleta os arquivos temporários.
		for (int i = 0; i < 2 * fanout; ++i) {
			files[i].close();
			Files.delete(Paths.get(files[i].getFilePath()));
		}
	}

	// Distribui os elementos em 2N arquivos, usando um PriorityQueue para
	// possibilitar segmentos maiores.
	private void distribuir() throws IOException {
		// Inicializa os arquivos temporários.
		for (int i = 0; i < files.length; ++i)
			files[i] = new TrackDB(db.getFilePath() + ".sort." + String.format("0x%02X", i) + ".bin");

		// Obtém o iterador do BD fonte, para que tenhamos maior controle sobre como
		// inserir os elementos no PriorityQueue.
		Iterator<Track> iterator = db.iterator();
		PriorityQueue<TrackPonderada> heap = new PriorityQueue<>(maxHeapNodes);
		int weight = 0; // Peso inicial.

		// Popula o heap inicialmente.
		while (heap.size() < maxHeapNodes && iterator.hasNext())
			heap.add(new TrackPonderada(iterator.next(), weight));

		// Distribui os elementos do arquivo inicial.
		while (heap.size() > 0) {
			// Remove um elemento, armazenando seu ID e atualizando o peso atual.
			TrackPonderada tmp = heap.remove();
			int lastId = tmp.track.getId();
			weight = tmp.weight;

			// Apende o elemento ao BD temporário correto.
			files[weight % fanout].append(tmp.track);

			// Se o arquivo não terminou, adiciona o próximo elemento ao PriorityQueue.
			if (iterator.hasNext()) {
				tmp = new TrackPonderada(iterator.next(), weight);
				if (tmp.track.getId() < lastId)
					tmp.weight += 1;
				heap.add(tmp);
			}

			if (verbose)
				System.err.println("Distribuindo ID " + lastId + ", peso: " + weight + ", arquivo: "
						+ files[weight % fanout].getFilePath() + ", " + heap.size() + " itens no heap");
		}

		// Garante que o PriorityQueue foi corretamente esvaziado.
		if (heap.size() != 0)
			throw new AssertionError("Erro interno na distribuição dos registros.");
	}

	// Classe auxiliar que agrupa uma Track com um peso, para uso com PriorityQueue.
	private class TrackPonderada implements Comparable<TrackPonderada> {
		public Track track;
		public int weight;

		public TrackPonderada(Track track, int weight) {
			this.track = track;
			this.weight = weight;
		}

		// Compara primeiro por peso, só depois por ID.
		@Override
		public int compareTo(TrackPonderada other) {
			int cmp = Integer.compare(weight, other.weight);
			cmp = (cmp == 0) ? track.compareTo(other.track) : cmp;
			return cmp;
		}
	}

	// Intercala os elementos de um grupo de N arquivos temporários no outro.
	private TrackDB intercalar() throws IOException {
		// Listas de BDs de origem e destino.
		List<TrackDB> source = new ArrayList<>(fanout);
		List<TrackDB> destination = new ArrayList<>(fanout);

		// Iteradores de cada BD, aqui por conveniência.
		List<Iterator<Track>> sourceIterators = new ArrayList<Iterator<Track>>(fanout);

		// Resultado que retornaremos. Corresponderá ao arquivo de `destination` em que
		// escrevemos, contanto que tenhamos escrito em somente um. Caso contrário, é
		// null.
		TrackDB result = null;

		// Determina se a fonte e o destino são, respectivamente, os arquivos numerados
		// 0–(N - 1), ou N–(2N - 1).
		int firstSourceId = (grupo) ? 0 : fanout;
		int firstDestinationId = fanout - firstSourceId;

		// Popula as listas a partir do resultado obtido acima.
		for (int i = firstSourceId; i < 2 * fanout - firstDestinationId; ++i) {
			source.add(files[i]);
			sourceIterators.add(files[i].iterator());
		}
		for (int i = firstDestinationId; i < 2 * fanout - firstSourceId; ++i)
			destination.add(files[i]);

		// Heap para achar o menor dos registros.
		PriorityQueue<TrackArquivo> heap = new PriorityQueue<>(fanout);
		int currentDestination = 0; // Segmento atual.

		// Itera para cada conjunto de segmentos, enquanto pelo menos um arquivo fonte
		// ainda tiver registros.
		while (source.stream().anyMatch(db -> !db.isFinished())) {
			// Popula o heap com o primeiro elemento de cada segmento.
			for (int i = 0; i < fanout; ++i) {
				Iterator<Track> caminhoAtual = sourceIterators.get(i);
				if (caminhoAtual.hasNext())
					heap.add(new TrackArquivo(caminhoAtual.next(), i));
			}

			if (verbose)
				System.err.println("\rIntercalando segmento " + currentDestination + ", grupo: "
						+ (grupo ? 'A' : 'B') + ", arquivo: " + currentDestination % fanout);

			// Itera até esgotarem-se os registros em cada segmento.
			while (heap.size() > 0) {
				TrackArquivo tmp = heap.remove();
				int origin = tmp.origin; // Armazena o arquivo onde o registro estava.

				// Se ainda há um registro no segmento deste arquivo, adiciona-o ao heap.
				if (sourceIterators.get(origin).hasNext()) {
					if (!source.get(origin).isSegmentFinished())
						heap.add(new TrackArquivo(sourceIterators.get(origin).next(), origin));
					else
						// Se o segmento já acabou, é importante mover o ponteiro do arquivo para o
						// começo do segmento seguinte, pois o método `.hasNext()` descarta
						// registros silenciosamente, se não tomarmos cuidado.
						source.get(origin).returnToSegmentStart();
				}

				// Ao escrever o elemento, salvamos em qual BD ele foi escrito.
				result = destination.get(currentDestination % fanout);
				result.append(tmp.track);
			}

			currentDestination += 1; // Incrementa o segmento.
		}

		// Ao fim, trunca todos os arquivos, deletando todos os elementos e deixando
		// apenas o
		// cabeçalho.
		for (TrackDB d : source)
			d.truncate();

		// Inverte o grupo, mudando a próxima iteração de A→B para A←B, ou vice-versa.
		grupo = !grupo;

		// Retorna o BD resultado, se tives sido ordenado com sucesso.
		return (currentDestination == 1) ? result : null;
	}

	// Classe auxiliar que agrupa uma Track com o índice do arquivo em que está,
	// para uso com PriorityQueue.
	private class TrackArquivo implements Comparable<TrackArquivo> {
		public Track track;
		public int origin;

		public TrackArquivo(Track track, int origin) {
			this.track = track;
			this.origin = origin;
		}

		@Override
		public int compareTo(TrackArquivo other) {
			return track.compareTo(other.track);
		}
	}

	// Getters & Setters.
	public TrackDB getDb() {
		return db;
	}

	public void setDb(TrackDB db) {
		this.db = db;
	}

	public TrackDB[] getFiles() {
		return files;
	}

	public void setFiles(TrackDB[] files) {
		this.files = files;
	}

	public int getFanout() {
		return fanout;
	}

	public void setFanout(int fanout) {
		this.fanout = fanout;
	}

	public int getMaxHeapNodes() {
		return maxHeapNodes;
	}

	public void setMaxHeapNodes(int maxHeapNodes) {
		this.maxHeapNodes = maxHeapNodes;
	}

	public boolean isGrupo() {
		return grupo;
	}

	public void setGrupo(boolean grupo) {
		this.grupo = grupo;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
}
