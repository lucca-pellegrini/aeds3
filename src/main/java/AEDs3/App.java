package AEDs3;

import AEDs3.DataBase.CSVManager;
import AEDs3.DataBase.Track;
import AEDs3.DataBase.TrackDB;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class App {
	public static void main(String[] args) throws IOException {
		CSVManager csv = new CSVManager("dataset-clean.csv");
		TrackDB db = new TrackDB("tracks.db");

		if (db.getLastId() == 0) {
			for (Track track : csv)
				db.create(track);
		}

		final int ID = ThreadLocalRandom.current().nextInt(1, db.getLastId());

		System.out.println("Deletando:\n\t" + db.read(ID) + "\n");
		db.delete(ID);

		if (db.read(ID) == null)
			System.out.println("ID " + ID + " deletado com sucesso\n\n");
		else
			throw new RuntimeException("Erro: ID " + ID + " deveria ter sido deletado!");

		ArrayList<String> artist = new ArrayList<>();
		artist.add("Cole Porter");
		db.setFilter(Track.Field.TRACK_ARTISTS, artist);
		System.out.println("Exibindo músicas de " + artist.get(0) + ":\n");
		for (Track track : db)
			System.out.println(track);
		System.out.println("\n\n");

		ArrayList<String> genres = new ArrayList<>();
		System.out.println("Exibindo músicas “adult standards” e “easy listening”:\n");
		genres.add("adult standards");
		genres.add("easy listening");
		db.setFilter(Track.Field.GENRES, genres);
		db.printAll();
		System.out.println("\n\n");

		System.out.println("Testando busca de álbum por RegEx:\n");
		db.setFilter(Track.Field.ALBUM_NAME,
				Pattern.compile(
						"((Catholic Classics, Vol\\.\\s\\d+:?|The Art of Gregorian|Masters of Chant( "
								+ "[IVXLCDM]+)?: (The .*|Chapter [IVXLCDM]+)))",
						Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
		db.printAll();
		System.out.println("\n\n");

		try {
			Menu.main(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
