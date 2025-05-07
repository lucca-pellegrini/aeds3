package AEDs3.DataBase.Compression;

import java.util.BitSet;

public class BitArray {

    private BitSet vetor;
    
    public BitArray() {
        vetor = new BitSet();
        vetor.set(0);
    }

    public BitArray(int n) {
        vetor = new BitSet(n);
        vetor.set(n);
    }

    public BitArray(byte[] v) {
        vetor = BitSet.valueOf(v);
    }

    public byte[] toByteArray() {
        return vetor.toByteArray();
    }

    public void set(int i) {
        if(i>=vetor.length()-1) {
            vetor.clear(vetor.length()-1);
            vetor.set(i+1);
        }
        vetor.set(i);
    }

    public void clear(int i) {
        if(i>=vetor.length()-1) {
            vetor.clear(vetor.length()-1);
            vetor.set(i+1);
        }
        vetor.clear(i);
    }

    public boolean get(int i) {
        return vetor.get(i);
    }

    public int length() {
        return vetor.length()-1;
    }

    public int size() {
        return vetor.size();
    }

    public String bitString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.length(); i++) {
            sb.append(this.get(i) ? '1' : '0');
        }
        return sb.toString();
    }
}
