package com.verticordia.AEDs3;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.stream.Collectors;

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

	/*Usamos o GPT para fazer os métodos hasNext() e next() para abstrair o iterador da classe CSVRecord.*/
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
				CSVRecord record = csvIterator.next();
				Date releaseDate;
				String releaseDateRecord = record.get("album_release_date");

				//Metodo para quando tiver só o ano, forçar a data completa.
				try { 
					releaseDate = new SimpleDateFormat("yyyy-MM-dd").parse(releaseDateRecord);
				} catch (ParseException e) {
					int year, month;
					if (releaseDateRecord.contains("-")) {
						String[] list = releaseDateRecord.split("-");
						year = Integer.parseInt(list[0]);
						month = Integer.parseInt(list[1]);
					} else {
						year = Integer.parseInt(releaseDateRecord);
						month = 01;
					}

					releaseDate = new Date(year - 1900, month, 01);
				}

				//Pegando as informações da track.
				return new Track(releaseDate,
						Arrays.stream(record.get("genres").split(","))
								.map(s -> s.replaceAll("[\\[\\]']", "").trim())
								.collect(Collectors.toList()),
						Arrays.stream(record.get("track_artists").split(","))
								.map(s -> s.replaceAll("[\\[\\]']", "").trim())
								.collect(Collectors.toList()),
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
			}
		};
	}
}