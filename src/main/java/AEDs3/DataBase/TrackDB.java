package AEDs3.DataBase;

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
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TrackDB implements Iterable<Track>, AutoCloseable {
	protected RandomAccessFile file;
	protected String filePath;
	protected int lastId;
	private long lastBinaryTrackPos;
	private TrackFilter searchFilter;

	// Tamanho do cabeçalho com os metadados do BD.
	private static final short HEADER_SIZE = Integer.SIZE / 8;

	// Abrindo o arquivo.
	public TrackDB(String fileName) throws FileNotFoundException, IOException {
		this.filePath = fileName;
		this.open();
	}

	public void open() throws IOException {
		file = new RandomAccessFile(filePath, "rw");
		file.seek(0);

		// Vê se o arquivo já tem algum ID inserido.
		try {
			lastId = file.readInt();
		} catch (EOFException e) {
			lastId = 0;
			file.writeInt(lastId);
		}
	}

	public void close() throws IOException {
		file.close();
	}


	// Função para adicionar uma linha no arquivo.
	public void create(Track track) throws IOException {
		lastId += 1;
		track.id = lastId;

		BinaryTrackWriter btw = new BinaryTrackWriter(track);
		file.seek(file.length());
		file.writeBoolean(btw.isTombstone());
		file.writeInt(btw.getSize());
		file.write(btw.getStream().toByteArray());

		updateLastId();
	}

	public Track read(int id) throws IOException {
		return readFirst(Track.Field.ID, id);
	}

	public Track readFirst(Track.Field field, Object value) throws IOException {
		file.seek(HEADER_SIZE); // Posiciona cursor no primeiro registro.

		for (Track t : this)
			if (t.matchesField(field, value))
				return t;

		return null;
	}

	public void update(int id, Track updated) throws IOException {
		if (read(id) == null)
			throw new NoSuchElementException("Não há elemento com ID " + id);

		updated.setId(id);
		file.seek(lastBinaryTrackPos);

		file.skipBytes(1); // Pula a lápide, pois .read() já validou o registro.
		int oldSize = file.readInt();
		BinaryTrackWriter writer = new BinaryTrackWriter(updated);

		// Verifica se o registro atualizado é menor ou igual ao anterior
		if (writer.getSize() <= oldSize) {
			// Volta para o começo do registro para sobrescrevê-lo
			file.seek(lastBinaryTrackPos);
			file.writeBoolean(writer.isTombstone());
			file.writeInt(oldSize);
		} else {
			// Seta a lápide do registro
			file.seek(lastBinaryTrackPos);
			file.writeBoolean(true);
			// Pula para o final do arquivo, para inserir registro no final
			file.seek(file.length());
			file.writeBoolean(writer.isTombstone());
			file.writeInt(writer.getSize());
		}

		// Escrevendo o registro.
		file.write(writer.getStream().toByteArray());
	}

	public void delete(int id) throws IOException {
		if (read(id) == null)
			throw new NoSuchElementException("Não há elemento com ID " + id);

		file.seek(lastBinaryTrackPos); // Volta para o começo do registro
		file.writeBoolean(true); // Seta a lápide
	}

	public void delete(Track.Field field, Object value) throws IOException {
		file.seek(HEADER_SIZE); // Posiciona cursor no primeiro registro.

		for (Track t : this) {
			if (t.matchesField(field, value)) {
				long pos = file.getFilePointer(); // Salva posição atual.
				file.seek(lastBinaryTrackPos); // Volta para o começo do registro.
				file.writeBoolean(true); // Seta a lápide.
				file.seek(pos); // Retorna para a posição salva.
			}
		}
	}

	public void print(int id) throws IOException {
		print(Track.Field.ID, id);
	}

	public void print(Track.Field field, Object value) throws IOException {
		file.seek(HEADER_SIZE); // Posiciona cursor no primeiro registro.

		for (Track t : this)
			if (t.matchesField(field, value))
				System.out.println(t);
	}

	public void printAll() throws IOException {
		file.seek(HEADER_SIZE); // Posiciona cursor no primeiro registro.

		for (Track t : this)
			System.out.println(t);
	}

	private Track nextTrack() throws EOFException, IOException, ClassNotFoundException {
		if (searchFilter == null)
			return nextValidBinaryTrackReader().getTrack();

		Track result = null;

		do
			result = nextValidBinaryTrackReader().getTrack();
		while (!(result.matchesField(searchFilter.searchField, searchFilter.searchValue)));

		return result;
	}

	private BinaryTrackReader nextValidBinaryTrackReader() throws EOFException, IOException {
		BinaryTrackReader result = null;

		do
			result = nextBinaryTrackReader();
		while (result == null);

		return result;
	}

	private BinaryTrackReader nextBinaryTrackReader() throws EOFException, IOException {
		lastBinaryTrackPos = file.getFilePointer();
		boolean tombstone = file.readBoolean();
		int size = file.readInt();

		if (tombstone) {
			file.skipBytes(size);
			return null;
		} else {
			byte[] buf = new byte[size];
			file.read(buf);
			return new BinaryTrackReader(tombstone, size, new ByteArrayInputStream(buf));
		}
	}

	private void updateLastId() throws IOException {
		long pos = file.getFilePointer();
		file.seek(0);
		file.writeInt(lastId);
		file.seek(pos);
	}

	public void setFilter(Track.Field field, Object value) {
		searchFilter = new TrackFilter(field, value);
	}

	public void clearFilter() {
		searchFilter = null;
	}

	private class TrackFilter {
		public Track.Field searchField;
		public Object searchValue;

		public TrackFilter(Field searchField, Object searchValue) {
			if (searchField == null || searchValue == null)
				throw new InvalidParameterException("Os valores devem ser não-nulos");

			this.searchField = searchField;
			this.searchValue = searchValue;
		}
	}

	@Override
	public Iterator<Track> iterator() throws RuntimeException {
		try {
			file.seek(HEADER_SIZE);
		} catch (IOException e) {
			throw new RuntimeException("Erro ao posicionar cursor no primeiro registro");
		}

		return new Iterator<Track>() {
			private Track currentTrack = null;

			@Override
			public boolean hasNext() {
				try {
					// Tenta ler a próxima Track. Se for válida, retorna true.
					currentTrack = nextTrack();
					return currentTrack != null;
				} catch (EOFException e) {
					return false;
				} catch (IOException | ClassNotFoundException e) {
					throw new RuntimeException("Falha ao obter próxima Track");
				}
			}

			@Override
			public Track next() throws RuntimeException {
				if (currentTrack == null)
					throw new NoSuchElementException("TrackDB chegou ao fim");

				Track track = currentTrack;
				currentTrack = null; // Reset para null até o próximo hasNext()
				return track;
			}
		};
	}

	public int getLastId() {
		return lastId;
	}

	public String getFilePath() {
		return filePath;
	}
}

abstract class BinaryTrack {
	protected boolean tombstone;
	protected int size;

	public boolean isTombstone() {
		return tombstone;
	}

	public void setTombstone(boolean tombstone) {
		this.tombstone = tombstone;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}

class BinaryTrackReader extends BinaryTrack {
	protected ByteArrayInputStream stream;
	private Track track;

	public BinaryTrackReader(boolean tombstone, int size, ByteArrayInputStream stream) {
		track = null;
		this.tombstone = tombstone;
		this.size = size;
		this.stream = stream;
	}

	private void readTrack() throws IOException, ClassNotFoundException {
		track = new Track();
		track.readExternal(new ObjectInputStream(stream));
	}

	public ByteArrayInputStream getStream() {
		return stream;
	}

	public void setStream(ByteArrayInputStream stream) {
		this.stream = stream;
	}

	public Track getTrack() throws IOException, ClassNotFoundException {
		if (track == null)
			readTrack();

		stream = null;
		return track;
	}

	public void setTrack(Track track) {
		this.track = track;
	}
}

// Classe de escrita para o arquivo.
class BinaryTrackWriter extends BinaryTrack {
	protected ByteArrayOutputStream stream;

	// Função para escrever no arquivo as tracks.
	public BinaryTrackWriter(Track track) throws IOException {
		stream = new ByteArrayOutputStream();

		try (ObjectOutputStream objOutStream = new ObjectOutputStream(stream)) {
			track.writeExternal(objOutStream);
		}

		tombstone = false;
		size = stream.size();
	}

	public ByteArrayOutputStream getStream() {
		return stream;
	}

	public void setStream(ByteArrayOutputStream stream) {
		this.stream = stream;
	}
}
