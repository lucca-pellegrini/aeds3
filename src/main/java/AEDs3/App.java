package AEDs3;

import AEDs3.DataBase.CSVManager;
import AEDs3.DataBase.Track;
import AEDs3.DataBase.TrackDB;
import java.io.IOException;
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

		if (db.read(ID) == null)
			System.out.println("ID " + ID + " deletado com sucesso");
		else
			throw new RuntimeException("Erro: ID " + ID + " deveria ter sido deletado!");
	}
}
