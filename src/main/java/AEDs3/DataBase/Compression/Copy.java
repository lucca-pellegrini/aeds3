package AEDs3.DataBase.Compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Copy implements StreamCompressor {

    @Override
    public void compress(InputStream in, OutputStream out) throws IOException {
		in.transferTo(out);
    }

    @Override
    public void decompress(InputStream in, OutputStream out) throws IOException {
		in.transferTo(out);
    }
}
