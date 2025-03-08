package AEDs3.DataBase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

public class BalancedMergeSort {
	TrackDB db;
	TrackDB[] files;
	int fanout;
	int maxHeapNodes;
	boolean grupo;

	public BalancedMergeSort(TrackDB db) {
		this(db, 8, 64);
	}

	public BalancedMergeSort(TrackDB db, int fanout, int maxHeapNodes) {
		if (fanout > maxHeapNodes)
			throw new IllegalArgumentException("maxHeapNodes deve ser maior que fanout.");

		this.db = db;
		this.fanout = fanout;
		this.maxHeapNodes = maxHeapNodes;
		this.grupo = true;
		files = new TrackDB[fanout * 2];
	}

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

		// Deleta os arquivos temporários.
		for (int i = 0; i < 2 * fanout; ++i) {
			files[i].close();
			Files.delete(Paths.get(files[i].getFilePath()));
		}
	}

	private void distribuir() throws IOException {
		for (int i = 0; i < files.length; ++i)
			files[i] = new TrackDB(db.getFilePath() + "." + UUID.randomUUID() + "." + i);

		Iterator<Track> iterator = db.iterator();
		PriorityQueue<TrackPonderada> heap = new PriorityQueue<>(maxHeapNodes);
		int weight = 0;

		// Popula o heap inicialmente.
		while (heap.size() < maxHeapNodes && iterator.hasNext()) {
			System.err.println(heap.size() + " itens no heap");
			heap.add(new TrackPonderada(iterator.next(), weight));
		}

		// Distribui os elementos do arquivo inicial.
		while (heap.size() > 0) {
			System.err.println(heap.size() + " itens no heap");

			TrackPonderada tmp = heap.remove();
			int lastId = tmp.track.getId();
			weight = tmp.weight;

			files[weight % fanout].append(tmp.track);
			System.err.println("ID " + lastId + " peso: " + weight
					+ " arquivo : " + files[weight % fanout].getFilePath());

			if (iterator.hasNext()) {
				tmp = new TrackPonderada(iterator.next(), weight);
				if (tmp.track.getId() < lastId)
					tmp.weight += 1;
				heap.add(tmp);
			}
		}

		if (heap.size() != 0)
			throw new AssertionError("Erro interno na distribuição dos registros.");
	}

	private class TrackPonderada implements Comparable<TrackPonderada> {
		public Track track;
		public int weight;

		public TrackPonderada(Track track, int weight) {
			this.track = track;
			this.weight = weight;
		}

		@Override
		public int compareTo(TrackPonderada other) {
			int cmp = Integer.compare(weight, other.weight);
			cmp = (cmp == 0) ? Integer.compare(track.getId(), other.track.getId()) : cmp;
			return cmp;
		}
	}

	private TrackDB intercalar() throws IOException {
		List<TrackDB> source = new ArrayList<>(fanout);
		List<TrackDB> destination = new ArrayList<>(fanout);
		List<Iterator<Track>> sourceIterators = new ArrayList<Iterator<Track>>(fanout);

		int firstSourceId = (grupo) ? 0 : fanout;
		int firstDestinationId = fanout - firstSourceId;

		TrackDB result = null;

		for (int i = firstSourceId; i < 2 * fanout - firstDestinationId; ++i) {
			source.add(files[i]);
			sourceIterators.add(files[i].iterator());
		}

		for (int i = firstDestinationId; i < 2 * fanout - firstSourceId; ++i)
			destination.add(files[i]);

		PriorityQueue<TrackArquivo> heap = new PriorityQueue<>(fanout);
		int currentDestination = 0;

		// Itera para cada conjunto de segmentos.
		while (source.stream().anyMatch(db -> !db.isFinished())) {
			// Popula o heap com o primeiro elemento de cada segmento.
			for (int i = 0; i < fanout; ++i) {
				Iterator<Track> caminhoAtual = sourceIterators.get(i);
				if (caminhoAtual.hasNext())
					heap.add(new TrackArquivo(caminhoAtual.next(), i));
			}

			// Itera até esgotarem-se os registros em cada segmento.
			while (heap.size() > 0) {
				TrackArquivo tmp = heap.remove();
				int origin = tmp.origin;

				if (sourceIterators.get(origin).hasNext()) {
					if (!source.get(origin).isSegmentFinished())
						heap.add(new TrackArquivo(sourceIterators.get(origin).next(), origin));
					else
						source.get(origin).returnToSegmentStart();
				}

				result = destination.get(currentDestination % fanout);
				result.append(tmp.track);
			}

			currentDestination += 1;
		}

		for (TrackDB d : source)
			d.truncate();

		grupo = !grupo;

		return (currentDestination == 1) ? result : null;
	}

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
}
