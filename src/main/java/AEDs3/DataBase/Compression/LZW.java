package AEDs3.DataBase.Compression;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class LZW {
    public static final int BITS_PER_INDEX = 12;

    public static void main(String[] args) {

        try {
            File file = new File("C:\\Users\\pedro\\OneDrive\\Área de Trabalho\\AEDs-III\\AEDs-III-8\\src\\main\\java\\AEDs3\\DataBase\\Compression\\arquivo_grande.bin");
            FileInputStream fis = new FileInputStream(file);

            byte[] originalBytes = fis.readAllBytes();
            fis.close();

            byte[] encodedBytes = encode(originalBytes);

            FileOutputStream fos = new FileOutputStream("compressed_file.lzw");
            fos.write(encodedBytes);
            fos.close();

            fis = new FileInputStream("compressed_file.lzw");
            byte[] encodedCopy = fis.readAllBytes();
            fis.close();

            byte[] decodedBytes = decode(encodedCopy);

            fos = new FileOutputStream("decompressed_file.bin");
            fos.write(decodedBytes);
            fos.close();

            System.out.println("\nOriginal Size: " + originalBytes.length + " bytes");
            System.out.println("Compressed Size: " + encodedBytes.length + " bytes");
            System.out.println("Compression Efficiency: " + (100 * (1 - (float) encodedBytes.length / (float) originalBytes.length)) + "%");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] encode(byte[] originalBytes) throws Exception {

        ArrayList<ArrayList<Byte>> dictionary = new ArrayList<>();
        ArrayList<Byte> byteSequence;
        int i, j;
        byte b;

        for (j = -128; j < 128; j++) {
            b = (byte) j;
            byteSequence = new ArrayList<>();
            byteSequence.add(b);
            dictionary.add(byteSequence);
        }

        ArrayList<Integer> output = new ArrayList<>();

        i = 0;
        int index;
        int lastIndex;

        while (i < originalBytes.length) {

            byteSequence = new ArrayList<>();
            b = originalBytes[i];
            byteSequence.add(b);
            index = dictionary.indexOf(byteSequence);
            lastIndex = index;

            while (index != -1 && i < originalBytes.length - 1) {
                i++;
                b = originalBytes[i];
                byteSequence.add(b);
                index = dictionary.indexOf(byteSequence);

                if (index != -1)
                    lastIndex = index;
            }

            output.add(lastIndex);

            if (dictionary.size() < (Math.pow(2, BITS_PER_INDEX) - 1))
                dictionary.add(byteSequence);

            if (index != -1 && i == originalBytes.length - 1)
                break;
        }

        BitArray bits = new BitArray(output.size() * BITS_PER_INDEX);
        int l = output.size() * BITS_PER_INDEX - 1;
        for (i = output.size() - 1; i >= 0; i--) {
            int n = output.get(i);
            for (int m = 0; m < BITS_PER_INDEX; m++) {
                if (n % 2 == 0)
                    bits.clear(l);
                else
                    bits.set(l);
                l--;
                n /= 2;
            }
        }

        System.out.println("Indices: ");
        System.out.println(output);
        System.out.println("Bit Array: ");
        System.out.println(bits);

        return bits.toByteArray();
    }

    public static byte[] decode(byte[] encodedBytes) throws Exception {

        BitArray bits = new BitArray(encodedBytes);

        int i, j, k;
        ArrayList<Integer> indices = new ArrayList<>();
        k = 0;
        for (i = 0; i < bits.length() / BITS_PER_INDEX; i++) {
            int n = 0;
            for (j = 0; j < BITS_PER_INDEX; j++) {
                n = n * 2 + (bits.get(k++) ? 1 : 0);
            }
            indices.add(n);
        }

        ArrayList<Byte> byteSequence;
        ArrayList<Byte> originalBytes = new ArrayList<>();

        ArrayList<ArrayList<Byte>> dictionary = new ArrayList<>();
        byte b;
        for (j = -128, i = 0; j < 128; j++, i++) {
            b = (byte) j;
            byteSequence = new ArrayList<>();
            byteSequence.add(b);
            dictionary.add(byteSequence);
        }

        ArrayList<Byte> nextByteSequence;

        i = 0;
        while (i < indices.size()) {
            byteSequence = (ArrayList<Byte>) (dictionary.get(indices.get(i))).clone();
            for (j = 0; j < byteSequence.size(); j++)
                originalBytes.add(byteSequence.get(j));

            if (dictionary.size() < (Math.pow(2, BITS_PER_INDEX) - 1))
                dictionary.add(byteSequence);

            i++;
            if (i < indices.size()) {
                nextByteSequence = (ArrayList<Byte>) dictionary.get(indices.get(i));
                byteSequence.add(nextByteSequence.get(0));
            }
        }

        byte[] outputBytes = new byte[originalBytes.size()];
        for (i = 0; i < originalBytes.size(); i++)
            outputBytes[i] = originalBytes.get(i);

        return outputBytes;
    }
}