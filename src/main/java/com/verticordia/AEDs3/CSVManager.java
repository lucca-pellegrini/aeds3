package com.verticordia.AEDs3;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;

public class CSVManager implements Iterable<Track> {
	protected CSVParser parser;

	public CSVManager(String fileName) throws FileNotFoundException, IOException {
		FileInputStream fis = new FileInputStream(new File(fileName));
		parser = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(
				new InputStreamReader(fis, StandardCharsets.UTF_8));
	}

	/* Usamos o GPT para fazer os métodos hasNext() e next() para abstrair o iterador da classe
	 * CSVRecord. */
	@Override
	public Iterator<Track> iterator() {
		return new Iterator<Track>() {
			private final Iterator<CSVRecord> csvIterator = parser.iterator();

			@Override
			public boolean hasNext() {
				return csvIterator.hasNext();
			}

			@Override
			public Track next() {
				try {
					CSVRecord record = csvIterator.next();
					return new Track(
							new SimpleDateFormat("yyyy-MM-dd")
									.parse(record.get("album_release_date")),
							Arrays.asList(record.get("genres").split(",")),
							Arrays.asList(record.get("track_artists").split(",")),
							record.get("album_name"), record.get("album_type"),
							record.get("name"),
							Boolean.parseBoolean(record.get("explicit")),
							record.get("track_id").toCharArray(),
							Float.parseFloat(record.get("loudness")),
							Float.parseFloat(record.get("danceability")),
							Float.parseFloat(record.get("energy")),
							Float.parseFloat(record.get("valence")),
							Float.parseFloat(record.get("tempo")),
							Integer.parseInt(record.get("key")),
							Integer.parseInt(record.get("popularity")),
							Integer.MIN_VALUE // Índice nulo
					);
				} catch (ParseException e) {
					throw new RuntimeException("Error parsing date", e);
				}
			}
		};
	}
}