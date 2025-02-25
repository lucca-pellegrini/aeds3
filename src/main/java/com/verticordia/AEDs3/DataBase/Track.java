package com.verticordia.AEDs3.DataBase;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.verticordia.AEDs3.Util.Range;

// Criação da classe track.
public class Track implements Externalizable {
	// Atributos
	protected LocalDate albumReleaseDate;
	protected List<String> genres; // Sujeito a futura revisão.
	protected List<String> trackArtists;
	protected String albumName;
	protected String albumType; // Sujeito a futura revisão.
	protected String name;
	protected boolean explicit;
	protected char[] trackId;
	protected float danceability;
	protected float energy;
	protected float loudness;
	protected float tempo;
	protected float valence;
	protected int id;
	protected int key;
	protected int popularity;

	private static final int trackIdNumChars = 22;

	// Construtor
	public Track(LocalDate albumReleaseDate, List<String> genres, List<String> trackArtists,
			String albumName, String albumType, String name, boolean explicit, char[] trackId,
			float loudness, float danceability, float energy, float valence, float tempo, int key,
			int popularity, int id) {
		this.albumReleaseDate = albumReleaseDate;
		this.genres = genres;
		this.trackArtists = trackArtists;
		this.albumName = albumName;
		this.albumType = albumType;
		this.name = name;
		this.explicit = explicit;
		this.trackId = trackId;
		this.loudness = loudness;
		this.danceability = danceability;
		this.energy = energy;
		this.valence = valence;
		this.key = key;
		this.popularity = popularity;
		this.tempo = tempo;
		this.id = id;
	}

	public Track() {
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(this.getId());
		out.writeUTF(this.getName());
		out.writeByte(this.getTrackArtists().size());
		for (String s : this.getTrackArtists())
			out.writeUTF(s);
		out.writeUTF(this.getAlbumName());
		out.writeLong(
				this.getAlbumReleaseDate().atStartOfDay().toEpochSecond(ZoneOffset.UTC));
		out.writeUTF(this.getAlbumType());
		out.writeByte(this.getGenres().size());
		for (String s : this.getGenres())
			out.writeUTF(s);
		out.writeBoolean(this.isExplicit());
		byte[] trackIdBytes = new String(this.getTrackId()).getBytes(StandardCharsets.US_ASCII);
		out.write(trackIdBytes);
		out.writeByte(this.getPopularity());
		out.writeByte(this.getKey());
		out.writeFloat(this.getDanceability());
		out.writeFloat(this.getEnergy());
		out.writeFloat(this.getLoudness());
		out.writeFloat(this.getTempo());
		out.writeFloat(this.getValence());
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		id = in.readInt();
		name = in.readUTF();
		int numArtists = in.readByte();
		trackArtists = new ArrayList<String>(numArtists);
		for (int i : new Range(numArtists))
			trackArtists.add(in.readUTF());
		albumName = in.readUTF();
		albumReleaseDate = LocalDate.ofEpochDay(in.readLong() / 86400);
		albumType = in.readUTF();
		int numGenres = in.readByte();
		genres = new ArrayList<String>(numGenres);
		for (int i : new Range(numGenres))
			genres.add(in.readUTF());
		explicit = in.readBoolean();
		byte[] trackIdBytes = new byte[Track.getTrackIdNumChars()];
		in.readFully(trackIdBytes);
		trackId = new String(trackIdBytes, StandardCharsets.US_ASCII).toCharArray();
		popularity = in.readByte();
		key = in.readByte();
		danceability = in.readFloat();
		energy = in.readFloat();
		loudness = in.readFloat();
		tempo = in.readFloat();
		valence = in.readFloat();
	}

	@Override
	public String toString() {
		return "Track [trackArtists=" + trackArtists + ", albumName=" + albumName + ", id=" + id
				+ ", getName()=" + getName() + "]";
	}

	// Getters e setters.
	public LocalDate getAlbumReleaseDate() {
		return albumReleaseDate;
	}

	public void setAlbumReleaseDate(LocalDate albumReleaseDate) {
		this.albumReleaseDate = albumReleaseDate;
	}

	public List<String> getGenres() {
		return genres;
	}

	public void setGenres(List<String> genres) {
		this.genres = genres;
	}

	public List<String> getTrackArtists() {
		return trackArtists;
	}

	public void setTrackArtists(List<String> trackArtists) {
		this.trackArtists = trackArtists;
	}

	public String getAlbumName() {
		return albumName;
	}

	public void setAlbumName(String albumName) {
		this.albumName = albumName;
	}

	public String getAlbumType() {
		return albumType;
	}

	public void setAlbumType(String albumType) {
		this.albumType = albumType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isExplicit() {
		return explicit;
	}

	public void setExplicit(boolean explicit) {
		this.explicit = explicit;
	}

	public char[] getTrackId() {
		return trackId;
	}

	public void setTrackId(char[] trackId) {
		this.trackId = trackId;
	}

	public float getLoudness() {
		return loudness;
	}

	public void setLoudness(float loudness) {
		this.loudness = loudness;
	}

	public float getDanceability() {
		return danceability;
	}

	public void setDanceability(float danceability) {
		this.danceability = danceability;
	}

	public float getEnergy() {
		return energy;
	}

	public void setEnergy(float energy) {
		this.energy = energy;
	}

	public float getValence() {
		return valence;
	}

	public void setValence(float valence) {
		this.valence = valence;
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}

	public int getPopularity() {
		return popularity;
	}

	public void setPopularity(int popularity) {
		this.popularity = popularity;
	}

	public float getTempo() {
		return tempo;
	}

	public void setTempo(float tempo) {
		this.tempo = tempo;
	}

	public static int getTrackIdNumChars() {
		return trackIdNumChars;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
