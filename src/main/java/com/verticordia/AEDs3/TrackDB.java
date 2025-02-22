package com.verticordia.AEDs3;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class TrackDB {
	protected RandomAccessFile file;
	protected int lastId;

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

	private void updateLastId() throws IOException {
		long pos = file.getFilePointer();
		file.seek(0);
		file.writeInt(lastId);
		file.seek(pos);
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

	private void readTrack() throws IOException {
		DataInputStream dataStream = new DataInputStream(stream);

		int id = dataStream.readInt();
		String name = dataStream.readUTF();
		int numArtists = dataStream.readByte();
		List<String> trackArtists = new ArrayList<String>(numArtists);
		for (int i : new Range(numArtists))
			trackArtists.add(dataStream.readUTF());
		String albumName = dataStream.readUTF();
		LocalDate albumReleaseDate = LocalDate.ofEpochDay(dataStream.readLong() / 86400);
		String albumType = dataStream.readUTF();
		int numGenres = dataStream.readByte();
		List<String> genres = new ArrayList<String>(numGenres);
		for (int i : new Range(numGenres))
			genres.add(dataStream.readUTF());
		boolean explicit = dataStream.readBoolean();
		byte[] trackIdBytes = dataStream.readNBytes(Track.getTrackIdNumChars());
		char[] trackId = new String(trackIdBytes, StandardCharsets.US_ASCII).toCharArray();
		byte popularity = dataStream.readByte();
		byte key = dataStream.readByte();
		float danceability = dataStream.readFloat();
		float energy = dataStream.readFloat();
		float loudness = dataStream.readFloat();
		float tempo = dataStream.readFloat();
		float valence = dataStream.readFloat();

		this.track = new Track(albumReleaseDate, genres, trackArtists, albumName, albumType, name,
				explicit, trackId, loudness, danceability, energy, valence, tempo, key, popularity, id);
	}

	public ByteArrayInputStream getStream() {
		return stream;
	}

	public void setStream(ByteArrayInputStream stream) {
		this.stream = stream;
	}

	public Track getTrack() throws IOException {
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
		DataOutputStream dataStream = new DataOutputStream(stream);

		dataStream.writeInt(track.getId());
		dataStream.writeUTF(track.getName());
		dataStream.writeByte(track.getTrackArtists().size());
		for (String s : track.getTrackArtists())
			dataStream.writeUTF(s);
		dataStream.writeUTF(track.getAlbumName());
		dataStream.writeLong(track.getAlbumReleaseDate().toEpochDay());
		dataStream.writeUTF(track.getAlbumType());
		dataStream.writeByte(track.getGenres().size());
		for (String s : track.getGenres())
			dataStream.writeUTF(s);
		dataStream.writeBoolean(track.isExplicit());
		dataStream.writeChars(track.getTrackId().toString());
		dataStream.writeByte(track.getPopularity());
		dataStream.writeByte(track.getKey());
		dataStream.writeFloat(track.getDanceability());
		dataStream.writeFloat(track.getEnergy());
		dataStream.writeFloat(track.getLoudness());
		dataStream.writeFloat(track.getTempo());
		dataStream.writeFloat(track.getValence());

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
