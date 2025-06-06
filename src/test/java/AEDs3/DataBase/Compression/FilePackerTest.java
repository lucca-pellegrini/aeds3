package AEDs3.DataBase.Compression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FilePackerTest {

    private final Map<String, Long> fileCRCMap = new HashMap<>();

    @AfterEach
    void tearDown() throws IOException {
        // Delete the unpacked files after each test
        String[] unpackedFiles = {
                "packerRandomData.0.bin",
                "packerRandomData.1.bin",
                "packerRandomData.2.bin",
                "packerRandomData.3.bin",
                "packerRandomData.4.bin",
                "packerRandomData.5.bin",
                "packerRandomData.6.bin",
                "packerRandomData.7.bin",
                "packerRandomData.8.bin",
                "packerRandomData.9.bin",
                "packerRandomData.10.bin",
                "packerRandomData.11.bin",
                "packerRandomData.12.bin",
                "packerRandomData.13.bin",
                "packerRandomData.14.bin",
                "packerRandomData.15.bin",
                "packerTextData.0.txt",
                "packerTextData.1.txt",
                "packerTextData.2.txt",
                "packerTextData.3.txt"
        };

        for (String fileName : unpackedFiles) {
            Files.deleteIfExists(Paths.get(fileName));
        }

        // Delete all generated files and the packed file
        for (String fileName : fileCRCMap.keySet()) {
            Files.deleteIfExists(Paths.get(fileName));
        }
        Files.deleteIfExists(Paths.get("generatedPack.pack"));
    }

    @Test
    void testUnpackAndVerifyCRC32() throws IOException {
        // Step 1: Unpack the packedData.pack file
        String packedFilePath = getResourcePath("packedData.pack");
        String[] unpackedFiles = FilePacker.unpack(packedFilePath);

        // Step 2: Verify each unpacked file's CRC32 digest
        for (String fileName : unpackedFiles) {
            // Calculate the CRC32 of the unpacked file
            long actualCRC32 = calculateCRC32(fileName);

            // Read the expected CRC32 from the corresponding .crc32 file
            String crc32FilePath = getResourcePath(fileName + ".crc32");
            long expectedCRC32 = Long.parseLong(Files.readString(Paths.get(crc32FilePath)).trim(), 16);

            // Compare the calculated CRC32 with the expected CRC32
            assertEquals(expectedCRC32, actualCRC32, "CRC32 mismatch for file: " + fileName);
        }
    }

    @Test
    void testPackAndUnpackGeneratedFiles() throws IOException {
        // Step 1: Generate files with pseudorandom content
        generateRandomFiles(10, 30);

        // Step 2: Pack the generated files
        String[] fileNames = fileCRCMap.keySet().toArray(new String[0]);
        FilePacker.pack(fileNames, "generatedPack.pack");

        // Delete original files after packing
        for (String fileName : fileNames) {
            Files.deleteIfExists(Paths.get(fileName));
        }

        // Step 3: Unpack the files
        String[] unpackedFiles = FilePacker.unpack("generatedPack.pack");

        // Step 4: Verify the integrity of unpacked files
        for (String fileName : unpackedFiles) {
            long actualCRC32 = calculateCRC32(fileName);
            long expectedCRC32 = fileCRCMap.get(fileName);
            assertEquals(expectedCRC32, actualCRC32, "CRC32 mismatch for file: " + fileName);
        }
    }

    private void generateRandomFiles(int minFiles, int maxFiles) throws IOException {
        Random random = new Random();
        int numFiles = random.nextInt(maxFiles - minFiles + 1) + minFiles;

        for (int i = 0; i < numFiles; i++) {
            String fileName = String.format("packerGeneratedData.%d.bin", i);
            int fileSize = random.nextInt(901) + 100; // 100 to 1000 bytes

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                byte[] data = new byte[fileSize];
                if (random.nextBoolean()) {
                    // Fill with random data
                    random.nextBytes(data);
                } else {
                    // Fill with a repeating pattern
                    byte pattern = (byte) random.nextInt(256);
                    for (int j = 0; j < data.length; j++) {
                        data[j] = pattern;
                    }
                }
                fos.write(data);
            }

            // Calculate and store the CRC32 checksum
            long crc32 = calculateCRC32(fileName);
            fileCRCMap.put(fileName, crc32);
        }
    }

    private long calculateCRC32(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            CRC32 crc32 = new CRC32();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                crc32.update(buffer, 0, bytesRead);
            }
            return crc32.getValue();
        }
    }

    private String getResourcePath(String resourceName) {
        try {
            return Objects.requireNonNull(getClass().getClassLoader().getResource(resourceName)).getPath();
        } catch (Exception e) {
            System.err.println(e.getMessage() + "Failed to get " + resourceName);
            throw e;
        }
    }
}
