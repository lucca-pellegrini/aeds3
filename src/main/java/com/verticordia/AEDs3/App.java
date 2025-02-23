package com.verticordia.AEDs3;

import com.verticordia.AEDs3.DataBase.CSVManager;
import com.verticordia.AEDs3.DataBase.Track;
import com.verticordia.AEDs3.DataBase.TrackDB;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class App {
	public static void main(String[] args) throws IOException {
		CSVManager csv = new CSVManager("dataset-clean.csv");
		TrackDB db = new TrackDB("tracks.db");

		for (Track track : csv)
			db.create(track);

		System.out.println(db.read(ThreadLocalRandom.current().nextInt(1, 99891)));
	}
}
