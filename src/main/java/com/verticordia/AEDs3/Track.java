package com.verticordia.AEDs3;

import java.util.Date;
import java.util.List;

//Criação da classe track.
public class Track {
	//Atributos
	protected Date albumReleaseDate;
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

	//Construtor
	public Track(Date albumReleaseDate, List<String> genres, List<String> trackArtists, String albumName,
			String albumType, String name, boolean explicit, char[] trackId, float loudness,
			float danceability, float energy, float valence, float tempo, int key, int popularity,
			int id) {
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

	// Getters e setters.
	public Date getAlbumReleaseDate() {
		return albumReleaseDate;
	}

	public void setAlbumReleaseDate(Date albumReleaseDate) {
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

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}