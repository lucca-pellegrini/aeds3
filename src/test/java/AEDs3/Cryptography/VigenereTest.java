package AEDs3.Cryptography;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class VigenereTest {

	// Keep track of generated files to delete them in tearDown
	private final List<Path> generatedFiles = new ArrayList<>();

	@AfterEach
	void tearDown() throws IOException {
		for (Path path : generatedFiles) {
			Files.deleteIfExists(path);
		}
		generatedFiles.clear();
	}

	/**
	 * Tests encrypting and then decrypting a set of resource files,
	 * verifying that the CRC32 checksum of the decrypted file matches
	 * the original.
	 */
	@Test
	void testEncryptDecryptResourceFiles() throws IOException {
		VigenereKey key = new VigenereKey(generateRandomAsciiString(64));
		String[] resources = {
				"vigenereTextData.txt",
				"vigenereRandomData.bin"
		};

		for (String resource : resources) {
			String originalPath = getResourcePath(resource);
			long originalCrc = calculateCRC32(originalPath);

			Path encrypted = Paths.get(resource + ".enc");
			Path decrypted = Paths.get(resource + ".dec");
			generatedFiles.add(encrypted);
			generatedFiles.add(decrypted);

			new Vigenere().encrypt(originalPath, encrypted.toString(), key);
			new Vigenere().decrypt(encrypted.toString(), decrypted.toString(), key);

			long decryptedCrc = calculateCRC32(decrypted.toString());
			assertEquals(
					originalCrc,
					decryptedCrc,
					"CRC32 mismatch after encrypt/decrypt for resource: " + resource);
		}
	}

	/**
	 * Tests encrypting and decrypting randomly generated binary files,
	 * verifying CRC32 integrity.
	 */
	@Test
	void testEncryptDecryptRandomFiles() throws IOException {
		VigenereKey key = new VigenereKey(generateRandomAsciiString(64));
		Random rnd = new Random();

		// Generate and test 5 random files of size between 100 and 1000 bytes
		for (int i = 0; i < 5; i++) {
			String baseName = "randomFile" + i + ".bin";
			Path original = Paths.get(baseName);
			generateRandomFile(original, 100 + rnd.nextInt(901));
			generatedFiles.add(original);

			long originalCrc = calculateCRC32(original.toString());

			Path encrypted = Paths.get(baseName + ".enc");
			Path decrypted = Paths.get(baseName + ".dec");
			generatedFiles.add(encrypted);
			generatedFiles.add(decrypted);

			new Vigenere().encrypt(original.toString(), encrypted.toString(), key);
			new Vigenere().decrypt(encrypted.toString(), decrypted.toString(), key);

			long decryptedCrc = calculateCRC32(decrypted.toString());
			assertEquals(
					originalCrc,
					decryptedCrc,
					"CRC32 mismatch after encrypt/decrypt for random file: " + baseName);
		}
	}

	/**
	 * Tests that providing a key with non-ASCII characters throws
	 * IllegalArgumentException.
	 */
	@Test
	void testInvalidKeyThrowsException() {
		String invalidKey = "abcdéfg";

		assertThrows(
				IllegalArgumentException.class,
				() -> new VigenereKey(invalidKey),
				"Expected new VigenereKey() to throw for non-ASCII key");
	}

	// Helper to write a file with random bytes
	private void generateRandomFile(Path path, int size) throws IOException {
		byte[] data = new byte[size];
		new Random().nextBytes(data);
		Files.write(path, data);
	}

	// Helper to calculate CRC32 of a file
	private long calculateCRC32(String filePath) throws IOException {
		try (InputStream in = new BufferedInputStream(new FileInputStream(filePath))) {
			CRC32 crc = new CRC32();
			byte[] buffer = new byte[1024];
			int n;
			while ((n = in.read(buffer)) != -1) {
				crc.update(buffer, 0, n);
			}
			return crc.getValue();
		}
	}

	// Helper to resolve resource paths from src/test/resources
	private String getResourcePath(String resourceName) {
		return Paths.get(
				Objects.requireNonNull(
						getClass().getClassLoader().getResource(resourceName),
						"Resource not found: " + resourceName).getPath())
				.toString();
	}

	private static String generateRandomAsciiString(int maxLen) {
		Random random = new Random();
		int length = random.nextInt(maxLen) + 1;
		StringBuilder sb = new StringBuilder(length);

		// Generate random ASCII characters
		for (int i = 0; i < length; i++) {
			// ASCII range from 32 (space) to 126 (~)
			char randomChar = (char) (random.nextInt(95) + 32);
			sb.append(randomChar);
		}

		return sb.toString();
	}
}
