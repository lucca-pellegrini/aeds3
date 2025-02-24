package com.verticordia.AEDs3;

import com.verticordia.AEDs3.DataBase.CSVManager;
import com.verticordia.AEDs3.DataBase.Track;
import com.verticordia.AEDs3.DataBase.TrackDB;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

public class App {
	public static final int ID = ThreadLocalRandom.current().nextInt(1, 99891);

	public static void main(String[] args) throws IOException {
		CSVManager csv = new CSVManager("dataset-clean.csv");
		TrackDB db = new TrackDB("tracks.db");

		for (Track track : csv)
			db.create(track);

		System.out.println(db.read(ID));
		db.delete(ID);

		try {
			db.read(ID);
			throw new RuntimeException("Erro: ID " + ID + " deveria ter sido deletado!");
		} catch (NoSuchElementException e) {
			System.out.println("ID " + ID + " deletado com sucesso");
		}
	}
}
