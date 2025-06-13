package AEDs3.DataBase;

import AEDs3.PatternMatching.BoyerMoore;
import AEDs3.PatternMatching.KMP;

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
	 * Dançabilidade da faixa.
	 */
	protected float danceability;

	/**
	 * Energia da faixa.
	 */
	protected float energy;

	/**
	 * Volume da faixa.
	 */
	protected float loudness;

	/**
	 * Tempo da faixa.
	 */
	protected float tempo;

	/**
	 * Valência (humor) da faixa.
	 */
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

	/**
	 * Número de caracteres do identificador da faixa.
	 */
	private static final int TRACK_ID_NUM_CHARS = 22;

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
	 * @throws IOException Caso ocorra erro durante a leitura dos dados.
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
	 * Determina igualdade desta faixa com outra baseada no ID.
	 *
	 * @param obj Faixa a ser comparada.
	 * @return Resultado da comparação entre os IDs das faixas.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Track other = (Track) obj;
		return this.id == other.id;
	}

	/**
	 * Determina a hash de uma Track.
	 *
	 * @return Resultado da hash baseada no ID.
	 */
	@Override
	public int hashCode() {
		return Integer.hashCode(this.id);
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
	 * @throws IOException               Se ocorrer um erro de entrada/saída durante
	 *                                   a verificação.
	 */
	@SuppressWarnings("unchecked")
	public boolean matchesField(Track.Field field, Object value) throws IOException {
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
			case KMP -> {
				if (value instanceof String pattern)
					yield KMP.match(pattern, getName()) || KMP.match(pattern, getAlbumName());
				else
					throw new InvalidParameterException("Tipo inválido. Esperava String.");
			}
			case BOYER_MOORE -> {
				if (value instanceof String pattern)
					yield BoyerMoore.match(pattern, getName()) || BoyerMoore.match(pattern, getAlbumName());
				else
					throw new InvalidParameterException("Tipo inválido. Esperava String.");
			}
			case NAME -> {
				if (value instanceof String)
					yield getName().equals(value);
				else if (value instanceof Pattern pattern)
					yield pattern.matcher(getName()).find();
				else
					throw new InvalidParameterException(
							"Tipo inválido. Esperava String ou Pattern.");
			}
			case ALBUM_NAME -> {
				if (value instanceof String)
					yield getAlbumName().equals(value);
				else if (value instanceof Pattern pattern)
					yield pattern.matcher(getAlbumName()).find();
				else
					throw new InvalidParameterException(
							"Tipo inválido. Esperava String ou Pattern.");
			}
			case TRACK_ARTISTS -> {
				if (!(value instanceof Collection<?>))
					yield false;
				if (((Collection<?>) value).stream().allMatch(String.class::isInstance))
					yield getTrackArtists().containsAll((Collection<String>) value);
				else
					throw new InvalidParameterException(
							"Tipo inválido! Esperava Collection<String>.");
			}
			case GENRES -> {
				if (!(value instanceof Collection<?>))
					yield false;
				if (((Collection<?>) value).stream().allMatch(String.class::isInstance))
					yield getGenres().containsAll((Collection<String>) value);
				else
					throw new InvalidParameterException(
							"Tipo inválido! Esperava Collection<String>.");
			}
		};
	}

	/**
	 * Enum que define os campos que podem ser utilizados para busca na faixa.
	 * Inclui também membros que não são campos, mas mecanismos de busca, como os
	 * algoritmos de casamento de padrões.
	 */
	public enum Field {
		/**
		 * Identificador único da faixa.
		 */
		ID,
		/**
		 * Nome da faixa.
		 */
		NAME,
		/**
		 * Artistas da faixa.
		 */
		TRACK_ARTISTS,
		/**
		 * Nome do álbum.
		 */
		ALBUM_NAME,
		/**
		 * Data de lançamento do álbum.
		 */
		ALBUM_RELEASE_DATE,
		/**
		 * Tipo do álbum (e.g., "single", "album").
		 */
		ALBUM_TYPE,
		/**
		 * Gêneros musicais da faixa.
		 */
		GENRES,
		/**
		 * Indica se a faixa contém conteúdo explícito.
		 */
		EXPLICIT,
		/**
		 * Identificador único da faixa.
		 */
		TRACK_ID,
		/**
		 * Popularidade da faixa.
		 */
		POPULARITY,
		/**
		 * Chave musical da faixa.
		 */
		KEY,
		/**
		 * Dançabilidade da faixa.
		 */
		DANCEABILITY,
		/**
		 * Energia da faixa.
		 */
		ENERGY,
		/**
		 * Volume da faixa.
		 */
		LOUDNESS,
		/**
		 * Tempo da faixa.
		 */
		TEMPO,
		/**
		 * Valência (humor) da faixa.
		 */
		VALENCE,
		/**
		 * Algoritmo de casamento de padrões KMP.
		 */
		KMP,
		/**
		 * Algoritmo de casamento de padrões Boyer-Moore.
		 */
		BOYER_MOORE
	}

	// Getters e setters para todos os atributos.

	/**
	 * Obtém a data de lançamento do álbum.
	 *
	 * @return a data de lançamento do álbum.
	 */
	public LocalDate getAlbumReleaseDate() {
		return albumReleaseDate;
	}

	/**
	 * Define a data de lançamento do álbum.
	 *
	 * @param albumReleaseDate a nova data de lançamento do álbum.
	 */
	public void setAlbumReleaseDate(LocalDate albumReleaseDate) {
		this.albumReleaseDate = albumReleaseDate;
	}

	/**
	 * Obtém a lista de gêneros musicais.
	 *
	 * @return a lista de gêneros musicais.
	 */
	public List<String> getGenres() {
		return genres;
	}

	/**
	 * Define a lista de gêneros musicais.
	 *
	 * @param genres a nova lista de gêneros musicais.
	 */
	public void setGenres(List<String> genres) {
		this.genres = genres;
	}

	/**
	 * Obtém a lista de artistas da faixa.
	 *
	 * @return a lista de artistas da faixa.
	 */
	public List<String> getTrackArtists() {
		return trackArtists;
	}

	/**
	 * Define a lista de artistas da faixa.
	 *
	 * @param trackArtists a nova lista de artistas da faixa.
	 */
	public void setTrackArtists(List<String> trackArtists) {
		this.trackArtists = trackArtists;
	}

	/**
	 * Obtém o nome do álbum.
	 *
	 * @return o nome do álbum.
	 */
	public String getAlbumName() {
		return albumName;
	}

	/**
	 * Define o nome do álbum.
	 *
	 * @param albumName o novo nome do álbum.
	 */
	public void setAlbumName(String albumName) {
		this.albumName = albumName;
	}

	/**
	 * Obtém o tipo do álbum.
	 *
	 * @return o tipo do álbum.
	 */
	public String getAlbumType() {
		return albumType;
	}

	/**
	 * Define o tipo do álbum.
	 *
	 * @param albumType o novo tipo do álbum.
	 */
	public void setAlbumType(String albumType) {
		this.albumType = albumType;
	}

	/**
	 * Obtém o nome da faixa.
	 *
	 * @return o nome da faixa.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Define o nome da faixa.
	 *
	 * @param name o novo nome da faixa.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Verifica se a faixa contém conteúdo explícito.
	 *
	 * @return true se a faixa for explícita, caso contrário false.
	 */
	public boolean isExplicit() {
		return explicit;
	}

	/**
	 * Define se a faixa contém conteúdo explícito.
	 *
	 * @param explicit true se a faixa for explícita, caso contrário false.
	 */
	public void setExplicit(boolean explicit) {
		this.explicit = explicit;
	}

	/**
	 * Obtém o identificador único da faixa.
	 *
	 * @return o identificador único da faixa.
	 */
	public char[] getTrackId() {
		return trackId;
	}

	/**
	 * Define o identificador único da faixa.
	 *
	 * @param trackId o novo identificador único da faixa.
	 * @throws InvalidParameterException se o trackId não tiver o número correto de
	 *                                   caracteres.
	 */
	public void setTrackId(char[] trackId) {
		if (trackId.length != Track.getTrackIdNumChars())
			throw new InvalidParameterException(
					"trackId deve ter exatamente " + Track.getTrackIdNumChars() + " caracteres");
		this.trackId = trackId;
	}

	/**
	 * Obtém o volume da faixa.
	 *
	 * @return o volume da faixa.
	 */
	public float getLoudness() {
		return loudness;
	}

	/**
	 * Define o volume da faixa.
	 *
	 * @param loudness o novo volume da faixa.
	 */
	public void setLoudness(float loudness) {
		this.loudness = loudness;
	}

	/**
	 * Obtém a dançabilidade da faixa.
	 *
	 * @return a dançabilidade da faixa.
	 */
	public float getDanceability() {
		return danceability;
	}

	/**
	 * Define a dançabilidade da faixa.
	 *
	 * @param danceability a nova dançabilidade da faixa.
	 */
	public void setDanceability(float danceability) {
		this.danceability = danceability;
	}

	/**
	 * Obtém a energia da faixa.
	 *
	 * @return a energia da faixa.
	 */
	public float getEnergy() {
		return energy;
	}

	/**
	 * Define a energia da faixa.
	 *
	 * @param energy a nova energia da faixa.
	 */
	public void setEnergy(float energy) {
		this.energy = energy;
	}

	/**
	 * Obtém a valência (humor) da faixa.
	 *
	 * @return a valência da faixa.
	 */
	public float getValence() {
		return valence;
	}

	/**
	 * Define a valência (humor) da faixa.
	 *
	 * @param valence a nova valência da faixa.
	 */
	public void setValence(float valence) {
		this.valence = valence;
	}

	/**
	 * Obtém a chave musical da faixa.
	 *
	 * @return a chave musical da faixa.
	 */
	public int getKey() {
		return key;
	}

	/**
	 * Define a chave musical da faixa.
	 *
	 * @param key a nova chave musical da faixa.
	 */
	public void setKey(int key) {
		this.key = key;
	}

	/**
	 * Obtém a popularidade da faixa.
	 *
	 * @return a popularidade da faixa.
	 */
	public int getPopularity() {
		return popularity;
	}

	/**
	 * Define a popularidade da faixa.
	 *
	 * @param popularity a nova popularidade da faixa.
	 */
	public void setPopularity(int popularity) {
		this.popularity = popularity;
	}

	/**
	 * Obtém o tempo da faixa.
	 *
	 * @return o tempo da faixa.
	 */
	public float getTempo() {
		return tempo;
	}

	/**
	 * Define o tempo da faixa.
	 *
	 * @param tempo o novo tempo da faixa.
	 */
	public void setTempo(float tempo) {
		this.tempo = tempo;
	}

	/**
	 * Obtém o número de caracteres do identificador da faixa.
	 *
	 * @return o número de caracteres do identificador da faixa.
	 */
	public static int getTrackIdNumChars() {
		return TRACK_ID_NUM_CHARS;
	}

	/**
	 * Obtém o ID único da faixa.
	 *
	 * @return o ID único da faixa.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Define o ID único da faixa.
	 *
	 * @param id o novo ID único da faixa.
	 */
	public void setId(int id) {
		this.id = id;
	}
}
