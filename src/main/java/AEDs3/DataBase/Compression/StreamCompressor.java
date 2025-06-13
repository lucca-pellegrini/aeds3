package AEDs3.DataBase.Compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

interface StreamCompressor {
	public void compress(InputStream in, OutputStream out) throws IOException;
	public void decompress(InputStream in, OutputStream out) throws IOException;
}
