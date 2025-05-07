package AEDs3.DataBase.Compression;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

class HuffmanNode implements Comparable<HuffmanNode> {
    byte b;
    int frequency;
    HuffmanNode left, right;

    public HuffmanNode(byte b, int f) {
        this.b = b;
        this.frequency = f;
        left = right = null;
    }

    @Override
    public int compareTo(HuffmanNode o) {
        return this.frequency - o.frequency;
    }
}

public class Huffman {
    
    // Método para gerar a tabela de códigos de Huffman
    public static HashMap<Byte, String> codeToBit(byte[] sequence) {
        Map<Byte, Integer> frequencyMap = new HashMap<>();
        for (byte c : sequence) {
            frequencyMap.put(c, frequencyMap.getOrDefault(c, 0) + 1);
        }

        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();
        for (Byte b : frequencyMap.keySet()) {
            pq.add(new HuffmanNode(b, frequencyMap.get(b)));
        }

        while (pq.size() > 1) {
            HuffmanNode left = pq.poll();
            HuffmanNode right = pq.poll();

            HuffmanNode father = new HuffmanNode((byte) 0, left.frequency + right.frequency);
            father.left = left;
            father.right = right;

            pq.add(father);
        }

        HuffmanNode root = pq.poll();
        HashMap<Byte, String> codes = new HashMap<>();
        generateCodes(root, "", codes);

        return codes;
    }

    // Método recursivo para gerar os códigos binários a partir da árvore de Huffman
    private static void generateCodes(HuffmanNode node, String code, HashMap<Byte, String> codes) {
        if (node == null) {
            return;
        }

        if (node.b != 0) {  // Se for um nó folha, armazenar o código
            codes.put(node.b, code);
        }

        generateCodes(node.left, code + "0", codes);
        generateCodes(node.right, code + "1", codes);
    }

    // Método para decodificar a sequência de bits usando a tabela de códigos
    public static byte[] decode(String codedSequence, Map<Byte, String> codes) {
        ByteArrayOutputStream decodedSequence = new ByteArrayOutputStream();
        StringBuilder currentCode = new StringBuilder();
        Map<String, Byte> reverseCodes = new HashMap<>();

        // Criar um mapa invertido de códigos para facilitar a busca rápida
        for (Map.Entry<Byte, String> entry : codes.entrySet()) {
            reverseCodes.put(entry.getValue(), entry.getKey());
        }

        for (int i = 0; i < codedSequence.length(); i++) {
            currentCode.append(codedSequence.charAt(i));
            if (reverseCodes.containsKey(currentCode.toString())) {
                decodedSequence.write(reverseCodes.get(currentCode.toString()));
                currentCode.setLength(0);  // Limpar o StringBuilder para o próximo código
            }
        }

        return decodedSequence.toByteArray();
    }

    // Método principal para codificar e decodificar o arquivo
    public static void main(String[] args) {
        String caminhoArquivo = "C:\\Users\\pedro\\OneDrive\\Área de Trabalho\\AEDs-III\\AEDs-III-8\\src\\main\\java\\AEDs3\\DataBase\\Compression\\arquivo_grande.bin";

        try {
            // Leitura do arquivo binário
            byte[] dados = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(caminhoArquivo));
            System.out.println("Arquivo original lido: " + dados.length + " bytes");

            // Codificação Huffman
            HashMap<Byte, String> codigos = codeToBit(dados);

            // Codificação em BitArray
            BitArray sequenciaCodificada = new BitArray();
            int i = 0;
            for (byte b : dados) {
                String codigo = codigos.get(b);
                if (codigo == null) {
                    System.out.println("Byte não encontrado na tabela de códigos: " + b);
                    continue; // Pula para o próximo byte
                }
                for (char c : codigo.toCharArray()) {
                    if (c == '0')
                        sequenciaCodificada.clear(i++);
                    else
                        sequenciaCodificada.set(i++);
                }
            }

            byte[] vb = sequenciaCodificada.toByteArray();
            System.out.println("Tamanho compactado: " + vb.length + " bytes");

            // Decodificação
            BitArray sequenciaCodificada2 = new BitArray(vb);
            String bitString = sequenciaCodificada2.bitString();
            byte[] dadosDecodificados = decode(bitString, codigos);

            // Escrita em um novo arquivo
            java.nio.file.Files.write(java.nio.file.Paths.get("arquivo_grande_decodificado.bin"), dadosDecodificados);
            System.out.println("Arquivo decodificado salvo como 'arquivo_grande_decodificado.bin'");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
