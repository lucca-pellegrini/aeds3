package AEDs3.DataBase;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Iterator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Classe responsável por gerenciar a leitura de arquivos CSV que contêm dados
 * de faixas de música. A classe implementa a interface {@link Iterable} para
 * fornecer uma maneira conveniente de iterar sobre os registros de faixas
 * presentes no arquivo CSV.
 * <p>
 * A classe também implementa {@link AutoCloseable}, garantindo que o recurso
 * CSVParser seja fechado automaticamente quando não for mais necessário.
 * <p>
 * Utiliza a biblioteca Apache Commons CSV para ler e parsear o arquivo CSV.
 */
public class CSVManager implements Iterable<Track>, AutoCloseable {
	/** Objeto responsável pela análise (parse) do arquivo CSV. */
	protected CSVParser parser;

	/**
	 * Construtor da classe {@link CSVManager}.
	 * <p>
	 * Inicializa o parser do arquivo CSV e configura o padrão de leitura para
	 * ignorar o cabeçalho e utilizar a codificação UTF-8.
	 *
	 * @param fileName O nome do arquivo CSV a ser lido.
	 * @throws FileNotFoundException Se o arquivo não for encontrado.
	 * @throws IOException           Se ocorrer algum erro de entrada/saída durante
	 *                               a leitura do arquivo.
	 */
	public CSVManager(String fileName) throws IOException {
		parser = CSVFormat.RFC4180.builder()
				.setHeader() // Configura o cabeçalho
				.setSkipHeaderRecord(true) // Ignora o cabeçalho do arquivo CSV
				.get() // Obtém o formatador CSV configurado
				.parse(new InputStreamReader(
						new FileInputStream(fileName), StandardCharsets.UTF_8));
	}

	/**
	 * Método para fechar o parser do CSV e liberar os recursos.
	 *
	 * @throws IOException Se ocorrer um erro durante o fechamento do parser.
	 */
	public void close() throws IOException {
		parser.close();
	}

	/**
	 * Implementação do método {@link Iterable#iterator()} que fornece um iterador
	 * para iterar sobre as faixas de música presentes no arquivo CSV.
	 * <p>
	 * O iterador converte cada registro do CSV em um objeto {@link Track}, que
	 * representa uma faixa de música com seus metadados.
	 *
	 * @return Um iterador para os objetos {@link Track}.
	 */
	@Override
	public Iterator<Track> iterator() {
		return new Iterator<>() {
			private final Iterator<CSVRecord> csvIterator = parser.iterator(); // Iterador do CSV

			/**
			 * Verifica se existem mais registros a serem lidos no arquivo CSV.
			 *
			 * @return {@code true} se houver mais registros, {@code false} caso contrário.
			 */
			@Override
			public boolean hasNext() {
				return csvIterator.hasNext();
			}

			/**
			 * Retorna o próximo registro como um objeto {@link Track}.
			 * <p>
			 * O método faz o parsing dos dados do CSV e os converte em um objeto
			 * {@link Track}, incluindo a conversão de datas e divisão de listas de gêneros
			 * e artistas.
			 *
			 * @return O próximo objeto {@link Track} a ser processado.
			 */
			@Override
			public Track next() {
				CSVRecord nextRecord = csvIterator.next(); // Obtém o próximo registro CSV
				LocalDate releaseDate;
				String releaseDateRecord = nextRecord.get("album_release_date");

				// Método para quando tiver só o ano, forçar a data completa.
				try {
					releaseDate = LocalDate.parse(
						releaseDateRecord, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
				} catch (DateTimeParseException e) {
					int year;
					int month;
					if (releaseDateRecord.contains("-")) {
						String[] list = releaseDateRecord.split("-");
						year = Integer.parseInt(list[0]);
						month = Integer.parseInt(list[1]);
					} else {
						year = Integer.parseInt(releaseDateRecord);
						month = 1;
					}

					releaseDate = LocalDate.of(year, month, 1);
				}

				// Criando e retornando o objeto Track a partir dos dados do CSV
				return new Track(releaseDate,
					Arrays.stream(nextRecord.get("genres").split(","))
						.map(s -> s.replaceAll("[\\[\\]']", "").trim())
						.toList(),
					Arrays.stream(nextRecord.get("track_artists").split(","))
						.map(s -> s.replaceAll("[\\[\\]']", "").trim())
						.toList(),
					nextRecord.get("album_name"), nextRecord.get("album_type"), nextRecord.get("name"),
					Boolean.parseBoolean(nextRecord.get("explicit")),
					nextRecord.get("track_id").toCharArray(), Float.parseFloat(nextRecord.get("loudness")),
					Float.parseFloat(nextRecord.get("danceability")),
					Float.parseFloat(nextRecord.get("energy")), Float.parseFloat(nextRecord.get("valence")),
					Float.parseFloat(nextRecord.get("tempo")), Integer.parseInt(nextRecord.get("key")),
					Integer.parseInt(nextRecord.get("popularity")),
					Integer.MIN_VALUE // Índice nulo
				);
			}
		};
	}
}
