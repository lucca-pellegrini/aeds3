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
	String dictionaryFilePath;
	String blockFilePath;
	RandomAccessFile dict;
	RandomAccessFile blocks;
	private static final int BLOCK_CAPACITY = 4;

	static class InvertedListRegister implements Comparable<InvertedListRegister> {
		private int id;

		public InvertedListRegister(int i) {
			this.id = i;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public InvertedListRegister(InvertedListRegister other) {
			this.id = other.id;
		}

		@Override
		public int compareTo(InvertedListRegister other) {
			return Integer.compare(this.id, other.id);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			InvertedListRegister other = (InvertedListRegister) obj;
			return this.id == other.id;
		}

		@Override
		public int hashCode() {
			return Integer.hashCode(this.id);
		}
	}

	static class Block {
		short num; // quantidade de dados presentes na lista
		short max; // quantidade máxima de dados que a lista pode conter
		InvertedListRegister[] items;
		long next;
		short bytesPerBlock;

		public Block(int qtdmax) {
			num = 0;
			max = (short) qtdmax;
			items = new InvertedListRegister[max];
			next = -1;
			bytesPerBlock = (short) (2 + 4 * max + 8);
		}

		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeShort(num);
			int i = 0;
			for (i = 0; i < num; ++i)
				dos.writeInt(items[i].getId());
			for (; i < max; ++i)
				dos.writeInt(-1);
			dos.writeLong(next);
			return baos.toByteArray();
		}

		public void fromByteArray(byte[] ba) throws IOException {
			ByteArrayInputStream bais = new ByteArrayInputStream(ba);
			DataInputStream dis = new DataInputStream(bais);
			num = dis.readShort();
			for (int i = 0; i < max; ++i)
				items[i] = new InvertedListRegister(dis.readInt());
			next = dis.readLong();
		}

		public boolean create(InvertedListRegister e) {
			if (full())
				return false;
			int i;
			for (i = num - 1; i >= 0 && e.getId() < items[i].getId(); --i)
				items[i + 1] = items[i];
			items[++i] = new InvertedListRegister(e);
			num += 1;
			return true;
		}

		public boolean test(int id) {
			if (empty())
				return false;
			int i;
			for (i = 0; i < num && id > items[i].getId(); ++i)
				;
			return i < num && id == items[i].getId();
		}

		public boolean delete(int id) {
			if (empty())
				return false;
			int i;
			for (i = 0; i < num && id > items[i].getId(); ++i)
				;
			if (id == items[i].getId()) {
				for (; i < num - 1; ++i)
					items[i] = items[i + 1];
				num -= 1;
				return true;
			} else {
				return false;
			}
		}

		public InvertedListRegister last() {
			return items[num - 1];
		}

		public InvertedListRegister[] list() {
			InvertedListRegister[] res = new InvertedListRegister[num];
			for (int i = 0; i < num; i++)
				res[i] = new InvertedListRegister(items[i]);
			return res;
		}

		public boolean empty() {
			return num == 0;
		}

		public boolean full() {
			return num == max;
		}

		public long next() {
			return next;
		}

		public void setNext(long p) {
			next = p;
		}

		public int size() {
			return bytesPerBlock;
		}
	}

	public InvertedListIndex(String dictionaryFilePath, String blockFilePath) throws IOException {
		this.dictionaryFilePath = dictionaryFilePath;
		this.blockFilePath = blockFilePath;

		dict = new RandomAccessFile(dictionaryFilePath, "rw");
		if (dict.length() < 4) { // cabeçalho do arquivo com número de entidades
			dict.seek(0);
			dict.writeInt(0);
		}
		blocks = new RandomAccessFile(blockFilePath, "rw");
	}

	public boolean create(String word, int id) throws IOException {
		return this.create(word, new InvertedListRegister(id));
	}

	// Insere um dado na lista da chave de forma NÃO ORDENADA
	private boolean create(String word, InvertedListRegister reg) throws IOException {
		// Percorre toda a lista testando se já não existe
		// o dado associado a essa chave
		InvertedListRegister[] elements = readElement(word);
		for (int i = 0; i < elements.length; i++)
			if (elements[i].getId() == reg.getId())
				return false;

		String key = "";
		long address = -1;
		boolean exists = false;

		// localiza a chave no dicionário
		dict.seek(4);
		while (dict.getFilePointer() != dict.length()) {
			key = dict.readUTF();
			address = dict.readLong();
			if (key.compareTo(word) == 0) {
				exists = true;
				break;
			}
		}

		// Se não encontrou, cria um novo bloco para essa chave
		if (!exists) {
			// Cria um novo bloco
			Block b = new Block(BLOCK_CAPACITY);
			address = blocks.length();
			blocks.seek(address);
			blocks.write(b.toByteArray());

			// Insere a nova chave no dicionário
			dict.seek(dict.length());
			dict.writeUTF(word);
			dict.writeLong(address);
		}

		// Cria um laço para percorrer todos os blocos encadeados nesse endereço
		Block b = new Block(BLOCK_CAPACITY);
		byte[] bd;
		while (address != -1) {
			long next = -1;

			// Carrega o bloco
			blocks.seek(address);
			bd = new byte[b.size()];
			blocks.read(bd);
			b.fromByteArray(bd);

			// Testa se o dado cabe nesse bloco
			if (!b.full()) {
				b.create(reg);
			} else {
				// Avança para o próximo bloco
				next = b.next();
				if (next == -1) {
					// Se não existir um novo bloco, cria esse novo bloco
					Block b1 = new Block(BLOCK_CAPACITY);
					next = blocks.length();
					blocks.seek(next);
					blocks.write(b1.toByteArray());

					// Atualiza o ponteiro do bloco anterior
					b.setNext(next);
				}
			}

			// Atualiza o bloco atual
			blocks.seek(address);
			blocks.write(b.toByteArray());
			address = next;
		}
		return true;
	}

	public int[] read(String word) throws IOException {
		if (word == null)
			return null;
		InvertedListRegister[] elementos = readElement(word);
		int[] ids = new int[elementos.length];
		for (int i = 0; i < ids.length; ++i)
			ids[i] = elementos[i].getId();
		return ids;
	}

	// Retorna a lista de dados de uma determinada chave
	private InvertedListRegister[] readElement(String c) throws IOException {
		ArrayList<InvertedListRegister> lista = new ArrayList<>();

		String chave = "";
		long endereco = -1;
		boolean jaExiste = false;

		// localiza a chave no dicionário
		dict.seek(4);
		while (dict.getFilePointer() != dict.length()) {
			chave = dict.readUTF();
			endereco = dict.readLong();
			if (chave.compareTo(c) == 0) {
				jaExiste = true;
				break;
			}
		}
		if (!jaExiste)
			return new InvertedListRegister[0];

		// Cria um laço para percorrer todos os blocos encadeados nesse endereço
		Block b = new Block(BLOCK_CAPACITY);
		byte[] bd;
		while (endereco != -1) {
			// Carrega o bloco
			blocks.seek(endereco);
			bd = new byte[b.size()];
			blocks.read(bd);
			b.fromByteArray(bd);

			// Acrescenta cada valor à lista
			InvertedListRegister[] lb = b.list();
			Collections.addAll(lista, lb);

			// Avança para o próximo bloco
			endereco = b.next();
		}

		// Constrói o vetor de respostas
		lista.sort(null);
		InvertedListRegister[] resposta = new InvertedListRegister[lista.size()];
		for (int j = 0; j < lista.size(); j++)
			resposta[j] = lista.get(j);
		return resposta;
	}

	// Remove o dado de uma chave (mas não apaga a chave nem apaga blocos)
	public boolean delete(String c, int id) throws IOException {
		String chave = "";
		long endereco = -1;
		boolean jaExiste = false;

		// localiza a chave no dicionário
		dict.seek(4);
		while (dict.getFilePointer() != dict.length()) {
			chave = dict.readUTF();
			endereco = dict.readLong();
			if (chave.compareTo(c) == 0) {
				jaExiste = true;
				break;
			}
		}
		if (!jaExiste)
			return false;

		// Cria um laço para percorrer todos os blocos encadeados nesse endereço
		Block b = new Block(BLOCK_CAPACITY);
		byte[] bd;
		while (endereco != -1) {
			// Carrega o bloco
			blocks.seek(endereco);
			bd = new byte[b.size()];
			blocks.read(bd);
			b.fromByteArray(bd);

			// Testa se o valor está neste bloco e sai do laço
			if (b.test(id)) {
				b.delete(id);
				blocks.seek(endereco);
				blocks.write(b.toByteArray());
				return true;
			}

			// Avança para o próximo bloco
			endereco = b.next();
		}

		// chave não encontrada
		return false;
	}

	public void destruct() throws IOException {
		blocks.close();
		dict.close();
		Files.delete(Paths.get(blockFilePath));
		Files.delete(Paths.get(dictionaryFilePath));
	}

	public String getDirFilePath() {
		return this.dictionaryFilePath;
	}

	public String getBlockFilePath() {
		return this.blockFilePath;
	}
}
