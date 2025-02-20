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
	public void add(Track track) throws IOException {
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

// Classe de escrita para o arquivo.
class BinaryTrackWriter {
	protected ByteArrayOutputStream stream;
	protected boolean valid;
	protected int size;

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
