package AEDs3.Compression.Compressors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface para compressores de fluxo de dados.
 */
public interface StreamCompressor {
    /**
     * Comprime os dados do InputStream e escreve no OutputStream.
     *
     * @param in  InputStream de onde os dados serão lidos
     * @param out OutputStream onde os dados comprimidos serão escritos
     * @throws IOException se ocorrer um erro de I/O durante a compressão
     */
    public void compress(InputStream in, OutputStream out) throws IOException;

    /**
     * Descomprime os dados do InputStream e escreve no OutputStream.
     *
     * @param in  InputStream de onde os dados comprimidos serão lidos
     * @param out OutputStream onde os dados descomprimidos serão escritos
     * @throws IOException se ocorrer um erro de I/O durante a descompressão
     */
    public void decompress(InputStream in, OutputStream out) throws IOException;
}
