package AEDs3.DataBase;

import java.io.IOException;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.UUID;

import javax.swing.border.SoftBevelBorder;

public class BalancedMergeSort {
	TrackDB db;
	TrackDB[] files;
	int fanout;
	int maxHeapNodes;

	public BalancedMergeSort(TrackDB db) {
		this(db, 8, 64);
	}

	public BalancedMergeSort(TrackDB db, int fanout, int maxHeapNodes) {
		if (fanout > maxHeapNodes)
			throw new IllegalArgumentException("maxHeapNodes deve ser maior que fanout.");

		this.db = db;
		this.fanout = fanout;
		this.maxHeapNodes = maxHeapNodes;
		files = new TrackDB[fanout * 2];
	}

	public void sort() throws IOException {
		for (int i = 0; i < files.length; ++i)
			files[i] = new TrackDB(db.getFilePath() + "." + UUID.randomUUID().getLeastSignificantBits() + "." + i);

		Iterator<Track> iterator = db.iterator();
		PriorityQueue<TrackPonderada> heap = new PriorityQueue<>(maxHeapNodes);
		int weight = 0;
		TrackPonderada lastTrack, nextTrack;

		// Popula o heap inicialmente.
		while (heap.size() < maxHeapNodes && iterator.hasNext())
			heap.add(new TrackPonderada(iterator.next(), weight));

		// Distribui os elementos do arquivo inicial.
		while (iterator.hasNext()) {
			lastTrack = heap.remove();
			int lastId = lastTrack.track.getId();
			int lastWeight = lastTrack.weight;

			if (lastWeight > weight)
				weight = lastWeight;

			files[weight % fanout].append(lastTrack.track);
			System.err.println("ID " + lastId + " peso: " + weight + " arquivo : " + files[weight % fanout].getFilePath());

			lastTrack = null;

			nextTrack = new TrackPonderada(iterator.next(), weight);
			if (nextTrack.track.getId() < lastId)
				nextTrack.weight += 1;
			heap.add(nextTrack);
		}
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
}
