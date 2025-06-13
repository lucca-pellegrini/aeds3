package AEDs3.Compression.Compressors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implementação de um compressor que copia dados sem compressão.
 */
public class CopyCompressor implements StreamCompressor {
    /**
     * Copia os dados do InputStream para o OutputStream sem compressão.
     *
     * @param in  o InputStream de onde os dados serão lidos
     * @param out o OutputStream para onde os dados serão escritos
     * @throws IOException se ocorrer um erro de I/O durante a operação
     */
    @Override
    public void compress(InputStream in, OutputStream out) throws IOException {
		in.transferTo(out);
    }

    /**
     * Copia os dados do InputStream para o OutputStream sem descompressão.
     *
     * @param in  o InputStream de onde os dados serão lidos
     * @param out o OutputStream para onde os dados serão escritos
     * @throws IOException se ocorrer um erro de I/O durante a operação
     */
    @Override
    public void decompress(InputStream in, OutputStream out) throws IOException {
		in.transferTo(out);
    }
}
