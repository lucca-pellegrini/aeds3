package AEDs3.DataBase;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

// Criação da classe track.
public class Track implements Externalizable, Comparable<Track> {
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

	// Escrita dos dados em binário.
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(getId());
		out.writeUTF(getName());
		out.writeByte(getTrackArtists().size());
		for (String s : getTrackArtists())
			out.writeUTF(s);
		out.writeUTF(getAlbumName());
		out.writeLong(getAlbumReleaseDate().atStartOfDay().toEpochSecond(ZoneOffset.UTC));
		out.writeUTF(getAlbumType());
		out.writeByte(getGenres().size());
		for (String s : getGenres())
			out.writeUTF(s);
		out.writeBoolean(isExplicit());
		byte[] trackIdBytes = new String(getTrackId()).getBytes(StandardCharsets.US_ASCII);
		out.write(trackIdBytes);
		out.writeByte(getPopularity());
		out.writeByte(getKey());
		out.writeFloat(getDanceability());
		out.writeFloat(getEnergy());
		out.writeFloat(getLoudness());
		out.writeFloat(getTempo());
		out.writeFloat(getValence());
	}

	// Leitura dos dados em binário.
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		id = in.readInt();
		name = in.readUTF();
		int numArtists = in.readByte();
		trackArtists = new ArrayList<String>(numArtists);
		for (int i = 0; i < numArtists; ++i)
			trackArtists.add(in.readUTF());
		albumName = in.readUTF();
		albumReleaseDate = LocalDate.ofEpochDay(in.readLong() / 86400);
		albumType = in.readUTF();
		int numGenres = in.readByte();
		genres = new ArrayList<String>(numGenres);
		for (int i = 0; i < numGenres; ++i)
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
	public int compareTo(Track other) {
		return Integer.compare(getId(), other.getId());
	}

	@Override
	public String toString() {
		return "Track [trackArtists=" + trackArtists + ", albumName=" + albumName + ", id=" + id
				+ ", getName()=" + getName() + "]";
	}

	// Verifica a igualdade entre um campo e um valor.
	@SuppressWarnings("unchecked")
	public boolean matchesField(Track.Field field, Object value) {
		return switch (field) {
			case ID -> getId() == (int) value;
			case ALBUM_RELEASE_DATE -> getAlbumReleaseDate().equals(value);
			case ALBUM_TYPE -> getAlbumType().equals(value);
			case EXPLICIT -> !(isExplicit() ^ (boolean) value);
			case TRACK_ID -> new String(getTrackId()).equals(value);
			case POPULARITY -> getPopularity() == (int) value;
			case KEY -> getKey() == (int) value;
			case DANCEABILITY -> getDanceability() == (float) value;
			case ENERGY -> getEnergy() == (float) value;
			case LOUDNESS -> getLoudness() == (float) value;
			case TEMPO -> getTempo() == (float) value;
			case VALENCE -> getValence() == (float) value;

			// Strings são comparadas por regex.
			case NAME -> {
				if (value instanceof String)
					yield getName().equals(value);
				else if (value instanceof Pattern)
					yield ((Pattern) value).matcher(getName()).find();
				else
					throw new InvalidParameterException(
							"Tipo inválido. Esperava String ou Pattern.");
			}

			case ALBUM_NAME -> {
				if (value instanceof String)
					yield getAlbumName().equals(value);
				else if (value instanceof Pattern)
					yield ((Pattern) value).matcher(getAlbumName()).find();
				else
					throw new InvalidParameterException(
							"Tipo inválido. Esperava String ou Pattern.");
			}

			// Listas exigem muito mais cuidado para tratar de forma segura.
			case TRACK_ARTISTS -> {
				// Verifica se o valor é uma Collection.
				if (!(value instanceof Collection<?>))
					yield false;

				// Verifica se os elementos da Collection são Strings.
				if (((Collection<?>) value).stream().allMatch(element -> element instanceof String))
					yield getTrackArtists().containsAll((Collection<String>) value);
				else
					throw new InvalidParameterException(
							"Tipo inválido! Esperava Collection<String>.");
			}

			case GENRES -> {
				// Verifica se o valor é uma Collection.
				if (!(value instanceof Collection<?>))
					yield false;

				// Verifica se os elementos da Collection são Strings.
				if (((Collection<?>) value).stream().allMatch(element -> element instanceof String))
					yield getGenres().containsAll((Collection<String>) value);
				else
					throw new InvalidParameterException(
							"Tipo inválido! Esperava Collection<String>.");
			}
		};
	}

	// Campos do registro disponíveis para busca.
	public enum Field {
		ID,
		NAME,
		TRACK_ARTISTS,
		ALBUM_NAME,
		ALBUM_RELEASE_DATE,
		ALBUM_TYPE,
		GENRES,
		EXPLICIT,
		TRACK_ID,
		POPULARITY,
		KEY,
		DANCEABILITY,
		ENERGY,
		LOUDNESS,
		TEMPO,
		VALENCE
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
		if (trackId.length != Track.getTrackIdNumChars())
			throw new InvalidParameterException(
					"trackId deve ter exatamente " + Track.getTrackIdNumChars() + " caracteres");

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
