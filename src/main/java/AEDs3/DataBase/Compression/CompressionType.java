package AEDs3.DataBase.Compression;

public enum CompressionType {
    HUFFMAN("Compressão Huffman"),
    LZW("Compressão LZW");

    private final String description;

    CompressionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
