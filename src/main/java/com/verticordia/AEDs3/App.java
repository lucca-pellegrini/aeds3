package com.verticordia.AEDs3;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        CSVManager csv = new CSVManager("dataset.csv");
        TrackDB db = new TrackDB("tracks.db");

		for (Track track : csv)
			db.add(track);
    }
}