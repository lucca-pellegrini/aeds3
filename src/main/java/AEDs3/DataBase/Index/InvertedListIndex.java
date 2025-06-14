package AEDs3.DataBase.Index;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * InvertedListIndex
 *
 * Manages an inverted index on disk, keeping a limited (LRU)
 * cache in memory of at most 1000 entries. The rest is backed
 * by the disk files:
 * - blocksFilePath: stores the actual postings (word -> list of IDs).
 * - directoryFilePath: stores the dictionary mapping word -> offset in blocks.
 * - frequencyFilePath: stores the dictionary mapping word -> frequency.
 *
 * Usage constraints:
 * 1) If none of the three files exist, create them.
 * 2) If all three exist, "load" them (minimal initialization).
 * 3) If some exist and others don't, throw error.
 * 4) If a word's frequency surpasses 10000, further insertions for that word
 * or searches for that word throw an exception.
 * 5) The in-memory cache never exceeds 1000 entries.
 */
public class InvertedListIndex implements AutoCloseable {

	private static final int DEFAULT_CACHE_SIZE = 1 << 10;
	private static final int MAX_FREQUENCY = 1 << 14;

	private final String blocksFilePath;
	private final String directoryFilePath;
	private final String frequencyFilePath;

	private final RandomAccessFile blkRaf;
	private final RandomAccessFile dirRaf;
	private final RandomAccessFile freqRaf;

	private long cacheSize;

	// This cache holds the postings for up to 1000 words
	// LRU-based eviction (LinkedHashMap with access-order = true).
	private final Map<String, CachedPosting> cache = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, CachedPosting> eldest) {
			if (size() > getCacheSize()) {
				// Write the postings for the evicted word out to disk
				flushPostingToDisk(eldest.getKey(), eldest.getValue());
				return true;
			}
			return false;
		}
	};

	// Simple structure to hold a posting list (list of IDs) and its frequency
	private static class CachedPosting {
		List<Integer> ids = new ArrayList<>();
		int frequency;
	}

	/**
	 * Constructor
	 *
	 * @param blocksFilePath    Path to the blocks file (postings).
	 * @param directoryFilePath Path to the directory file (word -> offset).
	 * @param frequencyFilePath Path to the frequency file (word -> frequency).
	 *
	 *                          Throws IllegalStateException if some files exist
	 *                          while others do not.
	 *                          Creates new empty files if none exist; otherwise,
	 *                          re-loads existing files
	 *                          minimally (does not read them entirely into memory).
	 */
	public InvertedListIndex(
			String blocksFilePath,
			String directoryFilePath,
			String frequencyFilePath) throws IOException {
		this.blocksFilePath = blocksFilePath;
		this.directoryFilePath = directoryFilePath;
		this.frequencyFilePath = frequencyFilePath;
		this.blkRaf = new RandomAccessFile(blocksFilePath, "rw");
		this.dirRaf = new RandomAccessFile(directoryFilePath, "rw");
		this.freqRaf = new RandomAccessFile(frequencyFilePath, "rw");
		this.cacheSize = DEFAULT_CACHE_SIZE;

		initFiles();
	}

	/**
	 * Create (insert) a new entry.
	 *
	 * @param word The word to insert.
	 * @param id   The document/entity ID to associate with that word.
	 * @return true if the entry was created successfully.
	 * @throws IllegalStateException if word's frequency is > 10000 or any other
	 *                               logic error.
	 */
	public boolean create(String word, int id) {
		if (word == null)
			return false;

		// 1) Get the posting from cache or disk
		CachedPosting posting = getPosting(word);

		// 2) Check if frequency is already too high
		if (posting.frequency >= MAX_FREQUENCY) {
			throw new IllegalStateException("Word '" + word + "' exceeds max frequency of " + MAX_FREQUENCY);
		}

		// 3) Insert the ID if not already present
		if (!posting.ids.contains(id)) {
			posting.ids.add(id);
			posting.frequency++;
		}

		// Possibly flush to disk if the cache LRU decides to evict soon...
		cache.put(word, posting);

		// 4) Return success
		return true;
	}

	/**
	 * Read (search) for an entry.
	 *
	 * @param word The word to look up.
	 * @return All matching IDs, or empty array if none.
	 * @throws IllegalStateException if word's frequency is > 10000.
	 */
	public int[] read(String word) {
		if (word == null)
			return new int[0];

		// 1) Check if we have it
		CachedPosting posting = getPosting(word);
		// System.out.println("Got posting.\nfreq = " + posting.frequency + "\nids:");
		// for (int i : posting.ids)
		// System.out.print(" " + i);
		// System.out.println();

		// 2) If freq > 10000, throw
		if (posting.frequency > MAX_FREQUENCY) {
			throw new IllegalStateException("Word '" + word + "' exceeds max frequency of " + MAX_FREQUENCY);
		}

		if (posting.ids.isEmpty())
			return new int[0];

		// 3) Return the IDs as an int array
		return posting.ids.stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Delete an association (word -> id).
	 *
	 * @param word the word to update
	 * @param id   the ID to remove from its postings
	 * @return true if it was actually removed; false if not found
	 */
	public boolean delete(String word, int id) {
		if (word == null)
			return false;

		// 1) Retrieve or create from disk
		CachedPosting posting = getPosting(word);

		// 2) Attempt to remove the ID
		boolean removed = posting.ids.remove((Integer) id);
		if (removed) {
			posting.frequency--;
			if (posting.frequency < 0) {
				posting.frequency = 0; // Just as a safety net
			}
		}

		// 3) Save back to cache
		cache.put(word, posting);
		return removed;
	}

	/**
	 * Destruct
	 *
	 * Delete all files that this index manages from disk.
	 */
	public void destruct() {
		// Close any open resources, flush, etc.
		flushAllPostingsToDisk();
		// Delete the files from disk
		new File(blocksFilePath).delete();
		new File(directoryFilePath).delete();
		new File(frequencyFilePath).delete();
	}

	/**
	 * List all file paths
	 *
	 * @return an array of file paths that this object manages
	 */
	public String[] listFilePaths() {
		return new String[] { blocksFilePath, directoryFilePath, frequencyFilePath };
	}

	// ----------------------------------------------------------------------------------
	// Private Implementation Details
	// ----------------------------------------------------------------------------------

	/**
	 * Initializes the index files:
	 * - Checks if they exist.
	 * - If none exist, creates them.
	 * - If all exist, open them for read/write minimal usage.
	 * - Otherwise, throw error if partially exist.
	 */
	private void initFiles() {
		File blocksFile = new File(blocksFilePath);
		File directoryFile = new File(directoryFilePath);
		File freqFile = new File(frequencyFilePath);

		boolean blocksExists = blocksFile.exists();
		boolean directoryExists = directoryFile.exists();
		boolean freqExists = freqFile.exists();

		// 1) If none exist, create them
		if (!blocksExists && !directoryExists && !freqExists) {
			try {
				blocksFile.createNewFile();
				directoryFile.createNewFile();
				freqFile.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException("Could not create index files", e);
			}
		}
		// 2) If all exist, we consider that a "load" scenario
		else if (blocksExists && directoryExists && freqExists) {
			// No heavy loading - just open the files for future read/writes
			// If needed, do some minimal scanning or caching of metadata
		}
		// 3) Otherwise, partial existence -> error
		else {
			throw new IllegalStateException("Some index files exist while others do not. Cannot initialize.");
		}
	}

	/**
	 * Retrieves a posting from the cache, or loads it from disk if it isn't cached.
	 * If there's no entry on disk (word not found in directory), returns a new
	 * posting.
	 */
	private CachedPosting getPosting(String word) {
		// Check if already in cache
		if (cache.containsKey(word)) {
			// System.out.println(word + " <- aleary cached");
			return cache.get(word);
		}

		// Otherwise, load from disk if it exists
		// System.out.println(word + " <- not cached");
		CachedPosting posting = loadPostingFromDisk(word);
		// Put into cache
		cache.put(word, posting);
		// System.out.println(word + " <- dded to cache!");
		return posting;
	}

	/** Small helper to remember where a block lives on disk */
	private static class DirectoryEntry {
		final long offset;
		final int blockSize;

		DirectoryEntry(long offset, int blockSize) {
			this.offset = offset;
			this.blockSize = blockSize;
		}
	}

	/** Read a length-prefixed UTF-8 string */
	private static String readString(RandomAccessFile raf) throws IOException {
		int len = raf.readInt();
		byte[] buf = new byte[len];
		raf.readFully(buf);
		return new String(buf, StandardCharsets.UTF_8);
	}

	/** Write a length-prefixed UTF-8 string */
	private static void writeString(RandomAccessFile raf, String s) throws IOException {
		byte[] buf = s.getBytes(StandardCharsets.UTF_8);
		raf.writeInt(buf.length);
		raf.write(buf);
	}

	/** Loads (or creates) the posting list + frequency for `word` from disk. */
	private CachedPosting loadPostingFromDisk(String word) {
		CachedPosting posting = new CachedPosting();
		try {
			dirRaf.seek(0);
			freqRaf.seek(0);

			// 1) Find the *last* directory record for `word`
			// System.out.println("Find last dir");
			DirectoryEntry last = null;
			while (dirRaf.getFilePointer() < dirRaf.length()) {
				String w = readString(dirRaf);
				long off = dirRaf.readLong();
				int sz = dirRaf.readInt();
				if (w.equals(word)) {
					last = new DirectoryEntry(off, sz);
				}
			}
			// 2) If found, read the block from blocksFile
			if (last != null) {
				// System.out.println("Found! reading block");
				blkRaf.seek(last.offset);
				int count = blkRaf.readInt();
				for (int i = 0; i < count; i++) {
					posting.ids.add(blkRaf.readInt());
				}
			}

			// 3) Now scan the frequency file for the *last* freq record
			// System.out.println("Scanning frequency");
			while (freqRaf.getFilePointer() < freqRaf.length()) {
				String w = readString(freqRaf);
				int f = freqRaf.readInt();
				if (w.equals(word)) {
					posting.frequency = f;
					// System.out.println("Frequency is: " + f);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("I/O error loading '" + word + "'", e);
		}
		return posting;
	}

	/** Flushes the in-memory posting back out to disk (appending new records). */
	private void flushPostingToDisk(String word, CachedPosting posting) {
		try {
			// 1) Append to blocksFile
			long offset;
			int blockSize;
			offset = blkRaf.length();
			blkRaf.seek(offset);
			int count = posting.ids.size();
			blkRaf.writeInt(count);
			for (int id : posting.ids) {
				blkRaf.writeInt(id);
			}
			blockSize = 4 + 4 * count;

			// 2) Append a new directory record
			dirRaf.seek(dirRaf.length());
			writeString(dirRaf, word);
			dirRaf.writeLong(offset);
			dirRaf.writeInt(blockSize);

			// 3) Append a new frequency record
			freqRaf.seek(freqRaf.length());
			writeString(freqRaf, word);
			freqRaf.writeInt(posting.frequency);

		} catch (IOException e) {
			throw new RuntimeException("I/O error flushing '" + word + "'", e);
		}
	}

	/**
	 * Flush all postings that are currently in the cache to disk.
	 */
	public void flushAllPostingsToDisk() {
		for (Map.Entry<String, CachedPosting> e : cache.entrySet()) {
			flushPostingToDisk(e.getKey(), e.getValue());
		}
		cache.clear();
	}

	public void close() {
		flushAllPostingsToDisk();
	}

	public static int getDefaultCacheSize() {
		return DEFAULT_CACHE_SIZE;
	}
	public long getCacheSize() {
		return cacheSize;
	}

	public void setCacheSize(long cacheSize) {
		this.cacheSize = cacheSize;
	}
}
