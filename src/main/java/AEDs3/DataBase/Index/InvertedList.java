package AEDs3.DataBase.Index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

class InvertedListRegister implements Comparable <InvertedListRegister>, Cloneable {
    
    private long posicao;
    private int id;

    public InvertedListRegister(Long l, int i) {
        this.posicao = l;
        this.id = i;
    }

    public long getPos() {
        return posicao;
    }

    public void setPos(long posicao) {
        this.posicao = posicao;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    @Override
    public String toString() {
        return "("+this.posicao+";"+this.id+")";
    }

    @Override
    public InvertedListRegister clone() {
        try {
            return (InvertedListRegister) super.clone();
        } catch (CloneNotSupportedException e) {
            // Tratamento de exceção se a clonagem falhar
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int compareTo(InvertedListRegister outro) {
        return Integer.compare(this.id, outro.id);
    }
}

public class InvertedList {

  String nomeArquivoDicionario;
  String nomeArquivoBlocos;
  RandomAccessFile arqDicionario;
  RandomAccessFile arqBlocos;
  int quantidadeDadosPorBloco;

  class Bloco {

    short quantidade; // quantidade de dados presentes na lista
    short quantidadeMaxima; // quantidade máxima de dados que a lista pode conter
    InvertedListRegister[] elementos; // sequência de dados armazenados no bloco
    long proximo; // ponteiro para o bloco sequinte da mesma chave
    short bytesPorBloco; // size fixo do cesto em bytes

    public Bloco(int qtdmax) throws Exception {
      quantidade = 0;
      quantidadeMaxima = (short) qtdmax;
      elementos = new InvertedListRegister[quantidadeMaxima];
      proximo = -1;
      bytesPorBloco = (short) (2 + (8+4) * quantidadeMaxima + 8);  // 4 do INT e 4 do FLOAT
    }

    public byte[] toByteArray() throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream(baos);
      dos.writeShort(quantidade);
      int i = 0;
      while (i < quantidade) {
        dos.writeLong(elementos[i].getPos());
        dos.writeInt(elementos[i].getId());
        i++;
      }
      while (i < quantidadeMaxima) {
        dos.writeLong(-1);
        dos.writeInt(-1);
        i++;
      }
      dos.writeLong(proximo);
      return baos.toByteArray();
    }

    public void fromByteArray(byte[] ba) throws IOException {
      ByteArrayInputStream bais = new ByteArrayInputStream(ba);
      DataInputStream dis = new DataInputStream(bais);
      quantidade = dis.readShort();
      int i = 0;
      while (i < quantidadeMaxima) {
        elementos[i] = new InvertedListRegister(dis.readLong(), dis.readInt());
        i++;
      }
      proximo = dis.readLong();
    }

    // Insere um valor no bloco
    public boolean create(InvertedListRegister e) {
      if (full())
        return false;
      int i = quantidade - 1;
      while (i >= 0 && e.getId() < elementos[i].getId()) {
        elementos[i + 1] = elementos[i];
        i--;
      }
      i++;
      elementos[i] = e.clone();
      quantidade++;
      return true;
    }

    // Lê um valor no bloco
    public boolean test(int id) {
      if (empty())
        return false;
      int i = 0;
      while (i < quantidade && id > elementos[i].getId())
        i++;
      if (i < quantidade && id == elementos[i].getId())
        return true;
      else
        return false;
    }

    // Remove um valor do bloco
    public boolean delete(int id) {
      if (empty())
        return false;
      int i = 0;
      while (i < quantidade && id > elementos[i].getId())
        i++;
      if (id == elementos[i].getId()) {
        while (i < quantidade - 1) {
          elementos[i] = elementos[i + 1];
          i++;
        }
        quantidade--;
        return true;
      } else
        return false;
    }

    public InvertedListRegister last() {
      return elementos[quantidade - 1];
    }

    public InvertedListRegister[] list() {
      InvertedListRegister[] lista = new InvertedListRegister[quantidade];
      for (int i = 0; i < quantidade; i++)
        lista[i] = elementos[i].clone();
      return lista;
    }

    public boolean empty() {
      return quantidade == 0;
    }

    public boolean full() {
      return quantidade == quantidadeMaxima;
    }

    public String toString() {
      String s = "\nQuantidade: " + quantidade + "\n| ";
      int i = 0;
      while (i < quantidade) {
        s += elementos[i] + " | ";
        i++;
      }
      while (i < quantidadeMaxima) {
        s += "- | ";
        i++;
      }
      return s;
    }

    public long next() {
      return proximo;
    }

    public void setNext(long p) {
      proximo = p;
    }

    public int size() {
      return bytesPorBloco;
    }

  }

  public void ListaInvertida(int n, String nd, String nc) throws Exception {
    quantidadeDadosPorBloco = n;
    nomeArquivoDicionario = nd;
    nomeArquivoBlocos = nc;

    arqDicionario = new RandomAccessFile(nomeArquivoDicionario, "rw");
    if(arqDicionario.length()<4) {    // cabeçalho do arquivo com número de entidades
      arqDicionario.seek(0);
      arqDicionario.writeInt(0);
    }
    arqBlocos = new RandomAccessFile(nomeArquivoBlocos, "rw");
  }

  // Incrementa o número de entidades
  public void incrementaEntidades() throws Exception {
    arqDicionario.seek(0);
    int n = arqDicionario.readInt();
    arqDicionario.seek(0);
    arqDicionario.writeInt(n+1);    
  }

  // Decrementa o número de entidades
  public void decrementaEntidades() throws Exception {
    arqDicionario.seek(0);
    int n = arqDicionario.readInt();
    arqDicionario.seek(0);
    arqDicionario.writeInt(n-1);    
  }

  // Retorna o número de entidades
  public int numeroEntidades() throws Exception {
    arqDicionario.seek(0);
    return arqDicionario.readInt();
  }

  // Insere um dado na lista da chave de forma NÃO ORDENADA
  public boolean create(String c, InvertedListRegister e) throws Exception {

    // Percorre toda a lista testando se já não existe
    // o dado associado a essa chave
    InvertedListRegister[] lista = read(c);
    for (int i = 0; i < lista.length; i++)
      if (lista[i].getId() == e.getId())
        return false;

    String chave = "";
    long endereco = -1;
    boolean jaExiste = false;

    // localiza a chave no dicionário
    arqDicionario.seek(4);
    while (arqDicionario.getFilePointer() != arqDicionario.length()) {
      chave = arqDicionario.readUTF();
      endereco = arqDicionario.readLong();
      if (chave.compareTo(c) == 0) {
        jaExiste = true;
        break;
      }
    }

    // Se não encontrou, cria um novo bloco para essa chave
    if (!jaExiste) {
      // Cria um novo bloco
      Bloco b = new Bloco(quantidadeDadosPorBloco);
      endereco = arqBlocos.length();
      arqBlocos.seek(endereco);
      arqBlocos.write(b.toByteArray());

      // Insere a nova chave no dicionário
      arqDicionario.seek(arqDicionario.length());
      arqDicionario.writeUTF(c);
      arqDicionario.writeLong(endereco);
    }

    // Cria um laço para percorrer todos os blocos encadeados nesse endereço
    Bloco b = new Bloco(quantidadeDadosPorBloco);
    byte[] bd;
    while (endereco != -1) {
        long proximo = -1;
        
        // Carrega o bloco
        arqBlocos.seek(endereco);
        bd = new byte[b.size()];
        arqBlocos.read(bd);
        b.fromByteArray(bd);
        
        // Testa se o dado cabe nesse bloco
        if (!b.full()) {
            b.create(e);
            // Atualiza o bloco e ENCERRA o loop
            arqBlocos.seek(endereco);
            arqBlocos.write(b.toByteArray());
            return true;  // *** Sai do método após inserir o elemento ***
        } else {
            // Avança para o próximo bloco
            proximo = b.next();
            if (proximo == -1) {
                // Se não existir um novo bloco, cria esse novo bloco
                Bloco b1 = new Bloco(quantidadeDadosPorBloco);
                b1.create(e);  // *** Insere o elemento no novo bloco ***
                proximo = arqBlocos.length();
                arqBlocos.seek(proximo);
                arqBlocos.write(b1.toByteArray());
                
                // Atualiza o ponteiro do bloco anterior
                b.setNext(proximo);
                
                // Atualiza o bloco atual
                arqBlocos.seek(endereco);
                arqBlocos.write(b.toByteArray());
                return true;  // *** Sai do método após criar e inserir no novo bloco ***
            }
        }
        
        // Atualiza o bloco atual
        arqBlocos.seek(endereco);
        arqBlocos.write(b.toByteArray());
        endereco = proximo;
    }
    return true;
  }

  // Retorna a lista de dados de uma determinada chave
  public InvertedListRegister[] read(String c) throws Exception {

    ArrayList<InvertedListRegister> lista = new ArrayList<>();

    String chave = "";
    long endereco = -1;
    boolean jaExiste = false;

    // localiza a chave no dicionário
    arqDicionario.seek(4);
    while (arqDicionario.getFilePointer() != arqDicionario.length()) {
      chave = arqDicionario.readUTF();
      endereco = arqDicionario.readLong();
      if (chave.compareTo(c) == 0) {
        jaExiste = true;
        break;
      }
    }
    if (!jaExiste)
      return new InvertedListRegister[0];

    // Cria um laço para percorrer todos os blocos encadeados nesse endereço
    Bloco b = new Bloco(quantidadeDadosPorBloco);
    byte[] bd;
    while (endereco != -1) {

      if (endereco < 0) {
        System.err.println("Erro: endereço negativo inválido (" + endereco + ") para a chave: " + c);
        break; // Interrompe o loop em caso de endereço inválido
      }

      // Carrega o bloco
      arqBlocos.seek(endereco);
      bd = new byte[b.size()];
      arqBlocos.read(bd);
      b.fromByteArray(bd);

      // Acrescenta cada valor à lista
      InvertedListRegister[] lb = b.list();
      for (int i = 0; i < lb.length; i++)
        lista.add(lb[i]);

      // Avança para o próximo bloco
      endereco = b.next();

    }

    // Constrói o vetor de respostas
    lista.sort(null);
    InvertedListRegister[] resposta = new InvertedListRegister[lista.size()];
    for (int j = 0; j < lista.size(); j++)
      resposta[j] = (InvertedListRegister) lista.get(j);
    return resposta;
  }

  // Procura na lista se o ID ainda existe
  public boolean encontrarID(int id) throws Exception {
    // Percorre todas as chaves do dicionário
    arqDicionario.seek(4);
    while (arqDicionario.getFilePointer() < arqDicionario.length()) {
      String chave = arqDicionario.readUTF();
      long endereco = arqDicionario.readLong();
        
      // Para cada chave, busca o ID
      Bloco b = new Bloco(quantidadeDadosPorBloco);
      byte[] bd;
      while (endereco != -1) {

        if (endereco < 0) {
          System.err.println("Erro: endereço negativo inválido (" + endereco + ") para o ID: " + id);
          break; // Interrompe o loop em caso de endereço inválido
        }

        // Carrega o bloco
        arqBlocos.seek(endereco);
        bd = new byte[b.size()];
        arqBlocos.read(bd);
        b.fromByteArray(bd);
            
        // Verifica se o ID está neste bloco
        if (b.test(id)) {
          return true;  // ID encontrado
        }
            
        // Avança para o próximo bloco
        endereco = b.next();
      }
    }
    return false;  // ID não encontrado em nenhuma chave
  }

  // Procura na lista o endereço do ID
  public long encontrarEndereco(int id) throws Exception {

    long resposta = -1;
    String chave = "";
    long endereco = -1;

    // Percorre todas as chaves do dicionário
    arqDicionario.seek(4);
    while (arqDicionario.getFilePointer() < arqDicionario.length()) {
      chave = arqDicionario.readUTF();
      endereco = arqDicionario.readLong();
        
      // Para cada chave, busca o ID
      Bloco b = new Bloco(quantidadeDadosPorBloco);
      byte[] bd;
      while (endereco != -1) {

        if (endereco < 0) {
          System.err.println("Erro: endereço negativo inválido (" + endereco + ") para o ID: " + id);
          break; // Interrompe o loop em caso de endereço inválido
        }

        // Carrega o bloco
        arqBlocos.seek(endereco);
        bd = new byte[b.size()];
        arqBlocos.read(bd);
        b.fromByteArray(bd);
            
        // Acrescenta cada valor à lista
        InvertedListRegister[] lb = b.list();
        for (int i = 0; i < lb.length; i++){
          
          if (lb[i].getId() == id) {
            resposta = lb[i].getPos();
            return resposta;  // ID encontrado, retorna a localização
          }

        }
            
        // Avança para o próximo bloco
        endereco = b.next();
      }
    }
    return resposta;  // ID não encontrado em nenhuma chave
  }

  // Remove o dado de uma chave (mas não apaga a chave nem apaga blocos)
  public boolean delete(String c, int id) throws Exception {

    // CASO ESPECIAL: Exclusão global por ID (em todas as chaves)
    if (c == null && id >= 0) {
      boolean algoFoiRemovido = false;
      
      // Primeiro, vamos salvar a posição atual do arquivo para restaurar depois
      long posicaoOriginal = arqDicionario.getFilePointer();
      
      // Posiciona no início do arquivo (após o cabeçalho)
      arqDicionario.seek(4);
      
      // Percorre todas as chaves do dicionário
      while (arqDicionario.getFilePointer() < arqDicionario.length()) {
          String chaveAtual = arqDicionario.readUTF();
          long enderecoAtual = arqDicionario.readLong();
          
          // Para cada chave, busca e remove o ID
          Bloco b = new Bloco(quantidadeDadosPorBloco);
          byte[] bd;
          long endereco = enderecoAtual;
          
          while (endereco != -1) {
              // Carrega o bloco
              arqBlocos.seek(endereco);
              bd = new byte[b.size()];
              arqBlocos.read(bd);
              b.fromByteArray(bd);
              
              // Verifica se o ID está neste bloco
              if (b.test(id)) {
                  b.delete(id);
                  arqBlocos.seek(endereco);
                  arqBlocos.write(b.toByteArray());
                  algoFoiRemovido = true;
              }
              
              // Avança para o próximo bloco
              endereco = b.next();
          }
      }
      
      // Restaura a posição original
      arqDicionario.seek(posicaoOriginal);
      return algoFoiRemovido;
    }

    String chave = "";
    long endereco = -1;
    boolean jaExiste = false;

    // localiza a chave no dicionário
    arqDicionario.seek(4);
    while (arqDicionario.getFilePointer() != arqDicionario.length()) {
      chave = arqDicionario.readUTF();
      endereco = arqDicionario.readLong();
      if (chave.compareTo(c) == 0) {
        jaExiste = true;
        break;
      }
    }

    if (!jaExiste)
      return false;

    // Caso especial: id = -1 significa "apagar todos os elementos"
    if (id == -1) {
      boolean algoFoiRemovido = false;
      Bloco b = new Bloco(quantidadeDadosPorBloco);
      byte[] bd;
      long enderecoInicial = endereco;
        
      // Percorre todos os blocos da chave e os esvazia
      while (endereco != -1) {
        // Carrega o bloco
        arqBlocos.seek(endereco);
        bd = new byte[b.size()];
        arqBlocos.read(bd);
        b.fromByteArray(bd);
            
        // Se o bloco tinha algum elemento, marcar que algo foi removido
        if (!b.empty()) {
          algoFoiRemovido = true;
        }
            
        // Salva o próximo endereço antes de esvaziar o bloco
        long proximoBloco = b.next();
            
        // Criar um novo bloco vazio mantendo o mesmo ponteiro de próximo
        b = new Bloco(quantidadeDadosPorBloco);
        b.setNext(proximoBloco);
            
        // Escrever o bloco vazio no arquivo
        arqBlocos.seek(endereco);
        arqBlocos.write(b.toByteArray());
            
        // Avança para o próximo bloco
        endereco = proximoBloco;
      }
        
      return algoFoiRemovido;
    }

    

    // Cria um laço para percorrer todos os blocos encadeados nesse endereço
    Bloco b = new Bloco(quantidadeDadosPorBloco);
    byte[] bd;
    while (endereco != -1) {

      // Carrega o bloco
      arqBlocos.seek(endereco);
      bd = new byte[b.size()];
      arqBlocos.read(bd);
      b.fromByteArray(bd);

      // Testa se o valor está neste bloco e sai do laço
      if (b.test(id)) {
        b.delete(id);
        arqBlocos.seek(endereco);
        arqBlocos.write(b.toByteArray());
        return true;
      }

      // Avança para o próximo bloco
      endereco = b.next();
    }

    // chave não encontrada
    return false;

  }

  public void print() throws Exception {

    System.out.println("\nLISTAS INVERTIDAS:");

    // Percorre todas as chaves
    arqDicionario.seek(4);
    while (arqDicionario.getFilePointer() != arqDicionario.length()) {

      String chave = arqDicionario.readUTF();
      long endereco = arqDicionario.readLong();

      // Percorre a lista desta chave
      ArrayList<InvertedListRegister> lista = new ArrayList<>();
      Bloco b = new Bloco(quantidadeDadosPorBloco);
      byte[] bd;
      while (endereco != -1) {

        // Carrega o bloco
        arqBlocos.seek(endereco);
        bd = new byte[b.size()];
        arqBlocos.read(bd);
        b.fromByteArray(bd);

        // Acrescenta cada valor à lista
        InvertedListRegister[] lb = b.list();
        for (int i = 0; i < lb.length; i++)
          lista.add(lb[i]);

        // Avança para o próximo bloco
        endereco = b.next();
      }

      // Imprime a chave e sua lista
      System.out.print(chave + ": ");
      lista.sort(null);
      for (int j = 0; j < lista.size(); j++)
        System.out.print(lista.get(j) + " ");
      System.out.println();
    }
  }

  // Método para fechar os arquivos abertos
  public void close() throws IOException {
  if (arqDicionario != null) {
      arqDicionario.close();
  }
  if (arqBlocos != null) {
      arqBlocos.close();
  }
  }
}
