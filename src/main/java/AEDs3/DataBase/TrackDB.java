package AEDs3.DataBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TrackDB implements Iterable<Track> {
	protected RandomAccessFile file;
	protected int lastId;
	private long lastBinaryTrackPos;

	// Tamanho do cabeçalho com os metadados do BD.
	private static final short HEADER_SIZE = Integer.SIZE / 8;

	// Abrindo o arquivo.
	public TrackDB(String fileName) throws FileNotFoundException, IOException {
		file = new RandomAccessFile(fileName, "rw");
		file.seek(0);

		try {
			lastId = file.readInt();
		} catch (EOFException e) {
			lastId = 0;
			file.writeInt(lastId);
		}
	}

	// Função para adicionar uma linha no arquivo.
	public void create(Track track) throws IOException {
		lastId += 1;
		track.id = lastId;

		BinaryTrackWriter btw = new BinaryTrackWriter(track);
		file.seek(file.length());
		file.writeBoolean(btw.isValid());
		file.writeInt(btw.getSize());
		file.write(btw.getStream().toByteArray());

		updateLastId();
	}

	public Track read(int id) throws IOException {
		file.seek(HEADER_SIZE); // Posiciona cursor no primeiro registro.

		for (Track t : this)
			if (t.getId() == id)
				return t;

		return null;
	}

	public void update(int id, Track updated) throws IOException {
		if (this.read(id) == null)
			throw new NoSuchElementException("Não há elemento com ID " + id);

		updated.setId(id);

		file.skipBytes(1); // Pula a lápide, pois .read() já validou o registro.
		int oldSize = file.readInt();
		BinaryTrackWriter writer = new BinaryTrackWriter(updated);

		// Verifica se o registro atualizado é menor ou igual ao anterior
		if (writer.getSize() <= oldSize) {
			// Volta para o começo do registro para sobrescrevê-lo
			file.seek(lastBinaryTrackPos);
		} else {
			// Seta a lápide do registro
			file.seek(lastBinaryTrackPos);
			file.writeBoolean(false);

			// Pula para o final do arquivo, para inserir registro no final
			file.seek(file.length());
		}

		file.writeBoolean(writer.isValid());
		file.writeInt(writer.getSize());
		file.write(writer.getStream().toByteArray());
	}

	public void delete(int id) throws IOException {
		if (this.read(id) == null)
			throw new NoSuchElementException("Não há elemento com ID " + id);

		file.seek(lastBinaryTrackPos); // Volta para o começo do registro
		file.writeBoolean(false); // Seta a lápide
	}

	public Track next() throws NoSuchElementException, IOException, ClassNotFoundException {
		try {
			return nextValidBinaryTrackReader().getTrack();
		} catch (EOFException e) {
			throw new NoSuchElementException("TrackDB chegou ao fim");
		}
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
		boolean valid = file.readBoolean();
		int size = file.readInt();

		if (valid) {
			byte[] buf = new byte[size];
			file.read(buf);
			return new BinaryTrackReader(valid, size, new ByteArrayInputStream(buf));
		} else {
			file.skipBytes(size);
			return null;
		}
	}

	private void updateLastId() throws IOException {
		long pos = file.getFilePointer();
		file.seek(0);
		file.writeInt(lastId);
		file.seek(pos);
	}

	@Override
	public Iterator<Track> iterator() throws RuntimeException {
		try {
			file.seek(HEADER_SIZE);
		} catch (IOException e) {
			throw new RuntimeException("Erro ao posicionar cursor no primeiro registro");
		}

		return new Iterator<Track>() {
			private BinaryTrackReader currentReader = null;

			@Override
			public boolean hasNext() {
				try {
					// Tenta ler a próxima Track. Se for válida, retorna true.
					currentReader = nextValidBinaryTrackReader();
					return currentReader != null;
				} catch (IOException e) {
					return false;
				}
			}

			@Override
			public Track next() throws RuntimeException {
				if (currentReader == null)
					throw new NoSuchElementException("TrackDB chegou ao fim");

				try {
					Track track = currentReader.getTrack();
					currentReader = null; // Reset para null até o próximo hasNext()
					return track;
				} catch (IOException | ClassNotFoundException e) {
					throw new RuntimeException("Falha ao obter próxima Track");
				}
			}
		};
	}
}

abstract class BinaryTrack {
	protected boolean valid;
	protected int size;

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
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

	public BinaryTrackReader(boolean valid, int size, ByteArrayInputStream stream) {
		track = null;
		this.valid = valid;
		this.size = size;
		this.stream = stream;
	}

	private void readTrack() throws IOException, ClassNotFoundException {
		this.track = new Track();
		this.track.readExternal(new ObjectInputStream(stream));
	}

	public ByteArrayInputStream getStream() {
		return stream;
	}

	public void setStream(ByteArrayInputStream stream) {
		this.stream = stream;
	}

	public Track getTrack() throws IOException, ClassNotFoundException {
		if (track == null)
			this.readTrack();

		this.stream = null;
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

		valid = true;
		size = stream.size();
	}

	public ByteArrayOutputStream getStream() {
		return stream;
	}

	public void setStream(ByteArrayOutputStream stream) {
		this.stream = stream;
	}
}
