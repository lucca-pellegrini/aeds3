package AEDs3.DataBase;

import static org.junit.jupiter.api.Assertions.*;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class TrackDBTest implements AutoCloseable {
	private Path filePath;
	private UUID dbId;
	private TrackDB db;
	private Iterator<Track> trackIterator;

	@BeforeAll
	void setUp(@TempDir Path tempDir) {
		this.filePath = tempDir.resolve("TrackDBTest.db");
	}

	@Nested
	@Order(1)
	class InitializationTests {
		@BeforeEach
		void setUp() throws IOException {
			db = new TrackDB(filePath.toString());
			dbId = db.getUUID();
			db = new TrackDB(filePath.toString());
			assertEquals(dbId, db.getUUID());
		}

		@Test
		void testInitialValues() {
			assertEquals(db.getLastId(), 0);
			assertEquals(db.getNumTracks(), 0);
			assertEquals(db.getNumSpaces(), 0);
			assertTrue(db.isOrdered());
		}

		@Test
		void testInitialConditions() throws IOException, ClassNotFoundException {
			assertEquals(db.file.getFilePointer(), db.file.length());
			assertEquals(db.file.getFilePointer(), TrackDB.HEADER_SIZE);
			assertThrows(EOFException.class, () -> db.nextTrack());
			assertFalse(db.iterator().hasNext());
			assertNull(db.read(1));
			assertThrows(NoSuchElementException.class, () -> db.delete(1));
			assertThrows(NoSuchElementException.class, () -> db.update(1, null));
		}
	}

	@Nested
	@Order(2)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class CRUDTests {
		@BeforeEach
		void setUp() throws IOException {
			db = new TrackDB(filePath.toString());
			dbId = db.getUUID();
			db = new TrackDB(filePath.toString());
			assertEquals(dbId, db.getUUID());
		}

		@Test
		@Order(1)
		void testCreate() throws IOException {
			try (CSVManager csv = new CSVManager(
					getClass().getClassLoader().getResource("TrackDBTest.csv").getPath())) {
				int i = 0;
				for (Track t : csv)
					assertEquals(++i, db.create(t));
			}
			assertEquals(db.getLastId(), 32);
		}

		@Test
		@Order(2)
		void testRead() throws IOException {
			assertTrue(db.read(8).getGenres().contains("english baroque"));
			assertArrayEquals(db.read(2).getTrackId(), "7lt9DQRgp0zcTFQofibPPk".toCharArray());

			db.file.seek(TrackDB.HEADER_SIZE);
			db.setFilter(Track.Field.ALBUM_NAME, Pattern.compile(".*2019 /.*2019 /.*2019"));
			for (Track t : db)
				assertEquals(t.getTrackArtists().get(0), "Johann Strauss II");
			db.clearFilter();
		}

		@Test
		@Order(3)
		void testUpdate() throws IOException {
			Track tmp;

			tmp = db.read(25);
			assertEquals(tmp.getName(), "i miss you");
			tmp.setName("shorter");
			db.update(25, tmp);
			assertTrue(db.isOrdered());
			tmp.setName("much much much much much much longer");
			db.update(25, tmp);
			assertFalse(db.isOrdered());
			assertEquals(db.getNumTracks() + 1, db.getNumSpaces());
			assertEquals(db.getLastId(), 32);
		}

		@Test
		@Order(4)
		void testDelete() throws IOException {
			int numTracks = db.getNumTracks();

			for (int i = 20; i > 15; --i) {
				db.delete(i);
				assertEquals(--numTracks, db.getNumTracks());
				assertNull(db.read(i));
			}
		}
	}

	@AfterAll
	@Override
	public void close() throws IOException {
		db.close();
	}
}
