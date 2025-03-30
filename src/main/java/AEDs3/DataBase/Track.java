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

/**
 * Representa uma faixa de música no banco de dados.
 * <p>
 * A classe {@link Track} implementa a interface {@link Externalizable},
 * permitindo a leitura e escrita dos dados da faixa de forma binária. Ela
 * também implementa {@link Comparable} para comparação das faixas com base no
 * ID.
 * <p>
 * Esta classe contém metadados relacionados a uma faixa de música, como nome da
 * faixa, artistas, data de lançamento do álbum, entre outros.
 */
public class Track implements Externalizable, Comparable<Track> {
	/**
	 * Data de lançamento do álbum.
	 */
	protected LocalDate albumReleaseDate;

	/**
	 * Gêneros musicais da faixa.
	 */
	protected List<String> genres;

	/**
	 * Artistas da faixa.
	 */
	protected List<String> trackArtists;

	/**
	 * Nome do álbum.
	 */
	protected String albumName;

	/**
	 * Tipo do álbum (e.g., "single", "album").
	 */
	protected String albumType;

	/**
	 * Nome da faixa.
	 */
	protected String name;

	/**
	 * Indica se a faixa contém conteúdo explícito.
	 */
	protected boolean explicit;

	/**
	 * Identificador único da faixa.
	 */
	protected char[] trackId;

	/**
	 * Atributos de áudio da faixa.
	 */
	protected float danceability;
	protected float energy;
	protected float loudness;
	protected float tempo;
	protected float valence;

	/**
	 * ID da faixa.
	 */
	protected int id;

	/**
	 * Chave musical da faixa.
	 */
	protected int key;

	/**
	 * Popularidade da faixa.
	 */
	protected int popularity;

	private static final int trackIdNumChars = 22;

	/**
	 * Constrói uma instância de {@link Track} com todos os metadados fornecidos.
	 *
	 * @param albumReleaseDate Data de lançamento do álbum.
	 * @param genres           Lista de gêneros musicais.
	 * @param trackArtists     Lista de artistas da faixa.
	 * @param albumName        Nome do álbum.
	 * @param albumType        Tipo do álbum.
	 * @param name             Nome da faixa.
	 * @param explicit         Indica se a faixa é explícita.
	 * @param trackId          Identificador da faixa.
	 * @param loudness         Volume da faixa.
	 * @param danceability     Dançabilidade da faixa.
	 * @param energy           Energia da faixa.
	 * @param valence          Valência (humor) da faixa.
	 * @param tempo            Tempo da faixa.
	 * @param key              Chave musical da faixa.
	 * @param popularity       Popularidade da faixa.
	 * @param id               ID único da faixa.
	 */
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

	/**
	 * Construtor padrão.
	 */
	public Track() {
	}

	/**
	 * Método responsável pela escrita dos dados da faixa em formato binário.
	 *
	 * @param out Fluxo de saída de dados.
	 * @throws IOException Caso ocorra erro durante a escrita dos dados.
	 */
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

	/**
	 * Método responsável pela leitura dos dados da faixa em formato binário.
	 *
	 * @param in Fluxo de entrada de dados.
	 * @throws IOException            Caso ocorra erro durante a leitura dos dados.
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException {
		id = in.readInt();
		name = in.readUTF();
		int numArtists = in.readByte();
		trackArtists = new ArrayList<>(numArtists);
		for (int i = 0; i < numArtists; ++i)
			trackArtists.add(in.readUTF());
		albumName = in.readUTF();
		albumReleaseDate = LocalDate.ofEpochDay(in.readLong() / 86400);
		albumType = in.readUTF();
		int numGenres = in.readByte();
		genres = new ArrayList<>(numGenres);
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

	/**
	 * Compara esta faixa com outra faixa com base no ID.
	 *
	 * @param other Faixa a ser comparada.
	 * @return Resultado da comparação entre os IDs das faixas.
	 */
	@Override
	public int compareTo(Track other) {
		return Integer.compare(getId(), other.getId());
	}

	/**
	 * Representação em formato de string da faixa.
	 *
	 * @return String representando a faixa.
	 */
	@Override
	public String toString() {
		return "Track [trackArtists=" + trackArtists + ", albumName=" + albumName + ", id=" + id
				+ ", getName()=" + getName() + "]";
	}

	/**
	 * Verifica se um campo específico da faixa corresponde ao valor informado.
	 *
	 * @param field Campo a ser verificado.
	 * @param value Valor a ser comparado.
	 * @return Verdadeiro se o campo corresponder ao valor, falso caso contrário.
	 * @throws InvalidParameterException Se o tipo do valor não for compatível com o
	 *                                   campo.
	 */
	@SuppressWarnings("unchecked")
	public boolean matchesField(Track.Field field, Object value) {
		return switch (field) {
			case ID -> getId() == (int) value;
			case ALBUM_RELEASE_DATE -> getAlbumReleaseDate().equals(value);
			case ALBUM_TYPE -> getAlbumType().equals(value);
			case EXPLICIT -> isExplicit() == (boolean) value;
			case TRACK_ID -> new String(getTrackId()).equals(value);
			case POPULARITY -> getPopularity() == (int) value;
			case KEY -> getKey() == (int) value;
			case DANCEABILITY -> getDanceability() == (float) value;
			case ENERGY -> getEnergy() == (float) value;
			case LOUDNESS -> getLoudness() == (float) value;
			case TEMPO -> getTempo() == (float) value;
			case VALENCE -> getValence() == (float) value;
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
			case TRACK_ARTISTS -> {
				if (!(value instanceof Collection<?>))
					yield false;
				if (((Collection<?>) value).stream().allMatch(element -> element instanceof String))
					yield getTrackArtists().containsAll((Collection<String>) value);
				else
					throw new InvalidParameterException(
							"Tipo inválido! Esperava Collection<String>.");
			}
			case GENRES -> {
				if (!(value instanceof Collection<?>))
					yield false;
				if (((Collection<?>) value).stream().allMatch(element -> element instanceof String))
					yield getGenres().containsAll((Collection<String>) value);
				else
					throw new InvalidParameterException(
							"Tipo inválido! Esperava Collection<String>.");
			}
		};
	}

	/**
	 * Enum que define os campos que podem ser utilizados para busca na faixa.
	 */
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

	// Getters e setters para todos os atributos.
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
