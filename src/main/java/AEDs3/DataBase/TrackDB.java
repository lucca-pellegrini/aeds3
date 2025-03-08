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
import java.util.UUID;

public class TrackDB implements Iterable<Track>, AutoCloseable {
	protected RandomAccessFile file;
	protected String filePath;
	private long lastBinaryTrackPos;
	private TrackFilter searchFilter;

	// Itens do cabeçalho
	protected UUID uuid; // ID único aleatório do BD.
	protected int lastId; // Último ID inserido.
	protected int numTracks; // Número de Tracks válidas no BD.
	protected int numSpaces; // Número de espaços usados no BD, incluindo com lápides.
	protected long flags; // BitMask com diversas flags.

	// Tamanho do cabeçalho com os metadados do BD.
	private static final short HEADER_SIZE = 3 * Long.SIZE / 8 + 3 * Integer.SIZE / 8;

	// Parâmetros para a ordenação
	protected boolean segmentFinished = false; // Se estamos no fim de um segmento.
	protected int lastIteratorId = 0; // Último ID encontrado pelo iterador.
	protected long segmentStart = HEADER_SIZE; // Posição do início do segmento atual.

	// Abrindo o arquivo.
	public TrackDB(String fileName) throws FileNotFoundException, IOException {
		this.filePath = fileName;
		this.open();
	}

	public void open() throws IOException {
		file = new RandomAccessFile(filePath, "rw");
		file.seek(0);

		// Vê se o arquivo já tem o UUID inserido.
		try {
			long major = file.readLong();
			long minor = file.readLong();
			uuid = new UUID(major, minor);
			lastId = file.readInt();
			flags = file.readLong();
			numTracks = file.readInt();
			numSpaces = file.readInt();
		} catch (EOFException e) {
			uuid = UUID.randomUUID();
			lastId = 0;
			flags = 0;
			setOrdered(true); // Arquivo vazio está ordenado.
			numTracks = 0;
			numSpaces = 0;
			updateHeader();
		}
	}

	public void close() throws IOException {
		file.close();
	}

	// Função para adicionar uma linha no arquivo.
	public int create(Track track) throws IOException {
		lastId += 1;
		track.id = lastId;

		return append(track);
	}

	protected int append(Track track) throws IOException {
		numTracks += 1;
		numSpaces += 1;

		BinaryTrackWriter btw = new BinaryTrackWriter(track);
		file.seek(file.length());
		file.writeBoolean(btw.isTombstone());
		file.writeInt(btw.getSize());
		file.write(btw.getStream().toByteArray());

		updateHeader();

		return track.id;
	}

	public Track read(int id) throws IOException {
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

			// Indica que o arquivo está agora desordenado.
			setOrdered(false);

			// Atualiza o cabeçalho.
			numSpaces += 1;
			updateHeader();
		}

		// Escrevendo o registro.
		file.write(writer.getStream().toByteArray());
	}

	public void delete(int id) throws IOException {
		if (read(id) == null)
			throw new NoSuchElementException("Não há elemento com ID " + id);

		file.seek(lastBinaryTrackPos); // Volta para o começo do registro
		file.writeBoolean(true); // Seta a lápide

		numTracks -= 1; // Decrementa contador de tracks.
		updateHeader(); // Atualiza o cabeçalho.
	}

	public void delete(Track.Field field, Object value) throws IOException {
		file.seek(HEADER_SIZE); // Posiciona cursor no primeiro registro.

		for (Track t : this) {
			if (t.matchesField(field, value)) {
				long pos = file.getFilePointer(); // Salva posição atual.
				file.seek(lastBinaryTrackPos); // Volta para o começo do registro.
				file.writeBoolean(true); // Seta a lápide.
				file.seek(pos); // Retorna para a posição salva.
				numTracks -= 1; // Decrementa contador de tracks.
			}
		}

		// Atualiza o cabeçalho.
		updateHeader();
	}

	public void sort(int fanout, int maxHeapNodes) throws IOException {
		new BalancedMergeSort(this, fanout, maxHeapNodes).sort();
	}

	public void sort() throws IOException {
		new BalancedMergeSort(this).sort();
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

	// Retorna o ponteiro do arquivo ao primeiro elemento do segmento.
	protected void returnToSegmentStart() throws IOException {
		file.seek(segmentStart);
	}

	// Zera o arquivo, mantendo apenas o UUID.
	protected void truncate() throws IOException {
		boolean isOpened = (file != null);
		if (!isOpened)
			open();

		file.getChannel().truncate(0);
		lastId = numTracks = numSpaces = 0;
		updateHeader();

		if (!isOpened)
			close();
	}

	// Verifica se estamos no final do arquivo.
	public boolean isFinished() {
		try {
			return file.getFilePointer() == file.length();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Problema ao verificar se o arquivo acabou.");
		}
	}

	private void updateHeader() throws IOException {
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

	public void setFilter(TrackFilter searchFilter) {
		this.searchFilter = searchFilter;
	}

	public void setFilter(Track.Field field, Object value) {
		setFilter(new TrackFilter(field, value));
	}

	public TrackFilter getFilter() {
		return searchFilter;
	}

	public void clearFilter() {
		searchFilter = null;
	}

	static public class TrackFilter {
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
					long pos = file.getFilePointer();
					// Tenta ler a próxima Track. Se for válida, retorna true.
					currentTrack = nextTrack();
					if (currentTrack != null)
						segmentFinished = (currentTrack.getId() < lastIteratorId);
					else
						segmentFinished = false;

					if (segmentFinished)
						segmentStart = pos;

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
				lastIteratorId = track.getId();
				currentTrack = null; // Reset para null até o próximo hasNext()
				return track;
			}
		};
	}

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

	public boolean isOrdered() {
		return (flags & Flag.ORDERED.getBitmask()) != 0;
	}

	public void setOrdered(boolean value) throws IOException {
		flags = value ? (flags | Flag.ORDERED.getBitmask()) : (flags & ~Flag.ORDERED.getBitmask());
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

// Tipo de enumeração para a BitMask contendo as flags do arquivo.
enum Flag {
	ORDERED(1L << 0); // Indica se o arquivo está ordenado.

	private final long bitmask;

	Flag(long bitmask) {
		this.bitmask = bitmask;
	}

	public long getBitmask() {
		return bitmask;
	}
}
