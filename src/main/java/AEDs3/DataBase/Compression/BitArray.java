package AEDs3.DataBase.Compression;

import java.util.BitSet;

public class BitArray {

    // Representa o vetor de bits principal
    private BitSet bitSet;

    // Construtor padrão: inicializa o BitSet e define o primeiro bit
    public BitArray() {
        bitSet = new BitSet();
        bitSet.set(0);
    }

    // Construtor com tamanho especificado: inicializa o BitSet e define o último bit
    public BitArray(int n) {
        bitSet = new BitSet(n);
        bitSet.set(n);
    }

    // Construtor que inicializa o BitSet a partir de um vetor de bytes
    public BitArray(byte[] byteArray) {
        bitSet = BitSet.valueOf(byteArray);
    }

    // Retorna o BitSet como um array de bytes
    public byte[] toByteArray() {
        return bitSet.toByteArray();
    }

    // Define um bit na posição 'i' como 1 (true)
    public void set(int i) {
        if (i >= bitSet.length() - 1) {
            bitSet.clear(bitSet.length() - 1);
            bitSet.set(i + 1);
        }
        bitSet.set(i);
    }

    // Define um bit na posição 'i' como 0 (false)
    public void clear(int i) {
        if (i >= bitSet.length() - 1) {
            bitSet.clear(bitSet.length() - 1);
            bitSet.set(i + 1);
        }
        bitSet.clear(i);
    }

    // Retorna o valor do bit na posição 'i'
    public boolean get(int i) {
        return bitSet.get(i);
    }

    // Retorna o comprimento do BitSet (número de bits utilizados)
    public int length() {
        return bitSet.length() - 1;
    }

    // Retorna o tamanho do BitSet (capacidade alocada internamente)
    public int size() {
        return bitSet.size();
    }

    // Retorna uma representação em string dos bits (0 e 1)
    public String bitString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.length(); i++) {
            sb.append(this.get(i) ? '1' : '0');
        }
        return sb.toString();
    }
}
