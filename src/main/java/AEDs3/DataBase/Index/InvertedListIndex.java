package AEDs3.DataBase.Index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class InvertedListIndex {
	String nomeArquivoDicionario;
	String nomeArquivoBlocos;
	RandomAccessFile arqDicionario;
	RandomAccessFile arqBlocos;
	private static final int BLOCK_CAPACITY = 4;

	class Bloco {
		short quantidade; // quantidade de dados presentes na lista
		short quantidadeMaxima; // quantidade máxima de dados que a lista pode conter
		ElementoLista[] elementos; // sequência de dados armazenados no bloco
		long proximo; // ponteiro para o bloco sequinte da mesma chave
		short bytesPorBloco; // size fixo do cesto em bytes

		public Bloco(int qtdmax) {
			quantidade = 0;
			quantidadeMaxima = (short) qtdmax;
			elementos = new ElementoLista[quantidadeMaxima];
			proximo = -1;
			bytesPorBloco = (short) (2 + 4 * quantidadeMaxima + 8);
		}

		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeShort(quantidade);
			int i = 0;
			while (i < quantidade) {
				dos.writeInt(elementos[i].getId());
				i++;
			}
			while (i < quantidadeMaxima) {
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
				elementos[i] = new ElementoLista(dis.readInt());
				i++;
			}
			proximo = dis.readLong();
		}

		// Insere um valor no bloco
		public boolean create(ElementoLista e) {
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
			while (i < quantidade && id > elementos[i].getId()) i++;
			return i < quantidade && id == elementos[i].getId();
		}

		// Remove um valor do bloco
		public boolean delete(int id) {
			if (empty())
				return false;
			int i = 0;
			while (i < quantidade && id > elementos[i].getId()) i++;
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

		public ElementoLista last() {
			return elementos[quantidade - 1];
		}

		public ElementoLista[] list() {
			ElementoLista[] lista = new ElementoLista[quantidade];
			for (int i = 0; i < quantidade; i++) lista[i] = elementos[i].clone();
			return lista;
		}

		public boolean empty() {
			return quantidade == 0;
		}

		public boolean full() {
			return quantidade == quantidadeMaxima;
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

	public InvertedListIndex(String nd, String nc) throws IOException {
		nomeArquivoDicionario = nd;
		nomeArquivoBlocos = nc;

		arqDicionario = new RandomAccessFile(nomeArquivoDicionario, "rw");
		if (arqDicionario.length() < 4) { // cabeçalho do arquivo com número de entidades
			arqDicionario.seek(0);
			arqDicionario.writeInt(0);
		}
		arqBlocos = new RandomAccessFile(nomeArquivoBlocos, "rw");
	}

	// Incrementa o número de entidades
	public void incrementaEntidades() throws IOException {
		arqDicionario.seek(0);
		int n = arqDicionario.readInt();
		arqDicionario.seek(0);
		arqDicionario.writeInt(n + 1);
	}

	// Decrementa o número de entidades
	public void decrementaEntidades() throws IOException {
		arqDicionario.seek(0);
		int n = arqDicionario.readInt();
		arqDicionario.seek(0);
		arqDicionario.writeInt(n - 1);
	}

	// Retorna o número de entidades
	public int numeroEntidades() throws IOException {
		arqDicionario.seek(0);
		return arqDicionario.readInt();
	}

	public boolean create(String c, int id) throws IOException {
		return this.create(c, new ElementoLista(id));
	}

	// Insere um dado na lista da chave de forma NÃO ORDENADA
	private boolean create(String c, ElementoLista e) throws IOException {
		// Percorre toda a lista testando se já não existe
		// o dado associado a essa chave
		ElementoLista[] lista = readElement(c);
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
			Bloco b = new Bloco(BLOCK_CAPACITY);
			endereco = arqBlocos.length();
			arqBlocos.seek(endereco);
			arqBlocos.write(b.toByteArray());

			// Insere a nova chave no dicionário
			arqDicionario.seek(arqDicionario.length());
			arqDicionario.writeUTF(c);
			arqDicionario.writeLong(endereco);
		}

		// Cria um laço para percorrer todos os blocos encadeados nesse endereço
		Bloco b = new Bloco(BLOCK_CAPACITY);
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
			} else {
				// Avança para o próximo bloco
				proximo = b.next();
				if (proximo == -1) {
					// Se não existir um novo bloco, cria esse novo bloco
					Bloco b1 = new Bloco(BLOCK_CAPACITY);
					proximo = arqBlocos.length();
					arqBlocos.seek(proximo);
					arqBlocos.write(b1.toByteArray());

					// Atualiza o ponteiro do bloco anterior
					b.setNext(proximo);
				}
			}

			// Atualiza o bloco atual
			arqBlocos.seek(endereco);
			arqBlocos.write(b.toByteArray());
			endereco = proximo;
		}
		return true;
	}

	public int[] read(String c) throws IOException {
		if (c == null)
			return null;
		ElementoLista[] elementos = readElement(c);
		int[] ids = new int[elementos.length];
		for (int i = 0; i < ids.length; ++i)
			ids[i] = elementos[i].getId();
		return ids;
	}

	// Retorna a lista de dados de uma determinada chave
	private ElementoLista[] readElement(String c) throws IOException {
		ArrayList<ElementoLista> lista = new ArrayList<>();

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
			return new ElementoLista[0];

		// Cria um laço para percorrer todos os blocos encadeados nesse endereço
		Bloco b = new Bloco(BLOCK_CAPACITY);
		byte[] bd;
		while (endereco != -1) {
			// Carrega o bloco
			arqBlocos.seek(endereco);
			bd = new byte[b.size()];
			arqBlocos.read(bd);
			b.fromByteArray(bd);

			// Acrescenta cada valor à lista
			ElementoLista[] lb = b.list();
			Collections.addAll(lista, lb);

			// Avança para o próximo bloco
			endereco = b.next();
		}

		// Constrói o vetor de respostas
		lista.sort(null);
		ElementoLista[] resposta = new ElementoLista[lista.size()];
		for (int j = 0; j < lista.size(); j++) resposta[j] = (ElementoLista) lista.get(j);
		return resposta;
	}

	// Remove o dado de uma chave (mas não apaga a chave nem apaga blocos)
	public boolean delete(String c, int id) throws IOException {
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

		// Cria um laço para percorrer todos os blocos encadeados nesse endereço
		Bloco b = new Bloco(BLOCK_CAPACITY);
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

	public void destruct() throws IOException {
		arqBlocos.close();
		arqDicionario.close();
		Files.delete(Paths.get(nomeArquivoBlocos));
		Files.delete(Paths.get(nomeArquivoDicionario));
	}

	public String getDirFilePath() {
		return this.nomeArquivoDicionario;
	}

	public String getBlockFilePath() {
		return this.nomeArquivoBlocos;
	}
}

class ElementoLista implements Comparable<ElementoLista>, Cloneable {
	private int id;

	public ElementoLista(int i) {
		this.id = i;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public ElementoLista clone() {
		try {
			return (ElementoLista) super.clone();
		} catch (CloneNotSupportedException e) {
			// Tratamento de exceção se a clonagem falhar
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int compareTo(ElementoLista outro) {
		return Integer.compare(this.id, outro.id);
	}
}
