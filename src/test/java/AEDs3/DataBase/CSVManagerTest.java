package AEDs3.DataBase;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CSVManagerTest implements AutoCloseable {
	private Iterator<Track> trackIterator;
	private CSVManager manager;

	@BeforeEach
	void setUp() throws IOException {
		manager = new CSVManager(
				getClass().getClassLoader().getResource("CSVManagerTestDataset.csv").getPath());
		trackIterator = manager.iterator();
	}

	@Test
	void testIterator() {
		assertNotNull(trackIterator);
	}

	@Test
	void testTrackParsing() {
		// Check the first record
		assertTrue(trackIterator.hasNext());
		Track firstTrack = trackIterator.next();
		assertNotNull(firstTrack);
		assertEquals("WILL", firstTrack.getAlbumName());
		assertEquals("WILL", firstTrack.getName());
		assertTrue(firstTrack.getTrackArtists().contains("TRUE"));
		assertEquals(2020, firstTrack.getAlbumReleaseDate().getYear());
		assertEquals(9, firstTrack.getAlbumReleaseDate().getMonthValue());

		// Check another record
		assertTrue(trackIterator.hasNext());
		Track secondTrack = trackIterator.next();
		assertEquals("Feel It", secondTrack.getName());
		assertEquals("Feel It", secondTrack.getAlbumName());
		assertEquals(2024, secondTrack.getAlbumReleaseDate().getYear());
		assertEquals(2, secondTrack.getAlbumReleaseDate().getMonthValue());
	}

	@Test
	void testDateParsing() {
		// Check how the CSVManager parses date strings.
		Track track;

		do {
			assertTrue(trackIterator.hasNext());
			track = trackIterator.next();
		} while (!new String(track.getTrackId()).equals("0x14eCwSx7kgoboLUmG2ik"));

		// Check normal date format is handled
		assertEquals(track.getAlbumReleaseDate(), LocalDate.of(2022, 12, 17));

		do {
			assertTrue(trackIterator.hasNext());
			track = trackIterator.next();
		} while (!new String(track.getTrackId()).equals("3N3t4yMGy1PCF7ZbizvZbK"));

		// Check year-only date format is handled
		assertEquals(track.getAlbumReleaseDate(), LocalDate.of(1993, 1, 1));

		do {
			assertTrue(trackIterator.hasNext());
			track = trackIterator.next();
		} while (!new String(track.getTrackId()).equals("3u7jQn9a8xrplb4wqFQIZL"));

		// Check year and month only date format is handled
		assertEquals(track.getAlbumReleaseDate(), LocalDate.of(1958, 9, 1));
	}

	@Test
	void testGenreList() {
		Track track;

		do {
			assertTrue(trackIterator.hasNext());
			track = trackIterator.next();
		} while (!new String(track.getTrackId()).equals("3WJRXxlZWFgt2G2O97maED"));

		// Check if genre list matches
		List<String> expectedGenres = Arrays.asList("classical", "french opera", "french romanticism",
				"late romantic era");
		List<String> actualGenres = track.getGenres();

		assertTrue(
				actualGenres.containsAll(expectedGenres) && expectedGenres.containsAll(actualGenres));
	}

	@Test
	void testEmptyGenreList() {
		Track track;

		do {
			assertTrue(trackIterator.hasNext());
			track = trackIterator.next();
		} while (!new String(track.getTrackId()).equals("5QjRzIrVQXZlqptOQEKE76"));

		// Check if genre list is empty (or contains empty string)
		assertTrue(track.getGenres().isEmpty() || track.getGenres().contains(""));
	}

	@Test
	void testTrackDataParsing() {
		assertTrue(trackIterator.hasNext());

		// Check specific attributes of the first track
		Track track = trackIterator.next();
		assertEquals(166.03, track.getTempo(), 0.0001);
		assertEquals(0.459, track.getEnergy(), 0.0001);
		assertEquals(-6.472, track.getLoudness(), 0.0001);
		assertEquals(0.24, track.getDanceability(), 0.0001);
	}

	@Override
	public void close() throws IOException {
		manager.close();
	}
}
