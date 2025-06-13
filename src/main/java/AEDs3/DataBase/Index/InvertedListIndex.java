package AEDs3.DataBase.Index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Classe que implementa um índice de lista invertida.
 * Utiliza arquivos para armazenar o dicionário e os blocos de dados.
 *
 * Esta classe fornece métodos para criar, ler e deletar registros associados a
 * palavras-chave, utilizando uma estrutura de lista invertida. Os registros são
 * armazenados em blocos, que são gerenciados por meio de arquivos de acesso
 * aleatório.
 */
public class InvertedListIndex {
	/**
	 * Caminho para o arquivo de dicionário.
	 */
	String dictionaryFilePath;

	/**
	 * Caminho para o arquivo de blocos.
	 */
	String blockFilePath;

	/**
	 * Arquivo de acesso aleatório para o dicionário.
	 */
	RandomAccessFile dict;

	/**
	 * Arquivo de acesso aleatório para os blocos.
	 */
	RandomAccessFile blocks;

	/**
	 * Capacidade máxima de registros por bloco.
	 */
	private static final int BLOCK_CAPACITY = 4;

	/**
	 * Classe que representa um registro na lista invertida.
	 * Implementa a interface Comparable para permitir ordenação.
	 */
	static class InvertedListRegister implements Comparable<InvertedListRegister> {
		/**
		 * ID do registro na lista invertida.
		 */
		private int id;

		/**
		 * Construtor que inicializa o registro com um ID.
		 *
		 * @param i ID do registro.
		 */
		public InvertedListRegister(int i) {
			this.id = i;
		}

		/**
		 * Obtém o ID do registro.
		 *
		 * @return ID do registro.
		 */
		public int getId() {
			return id;
		}

		/**
		 * Define o ID do registro.
		 *
		 * @param id Novo ID do registro.
		 */
		public void setId(int id) {
			this.id = id;
		}

		/**
		 * Construtor de cópia.
		 *
		 * @param other Outro registro a ser copiado.
		 */
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

	/**
	 * Classe que representa um bloco na lista invertida.
	 * Gerencia registros e permite operações de adição, remoção e verificação.
	 */
	static class Block {
		/**
		 * Quantidade de dados presentes na lista.
		 */
		short num;

		/**
		 * Quantidade máxima de dados que a lista pode conter.
		 */
		short max;

		/**
		 * Array de registros na lista invertida.
		 */
		InvertedListRegister[] items;

		/**
		 * Endereço do próximo bloco.
		 */
		long next;

		/**
		 * Tamanho do bloco em bytes.
		 */
		short bytesPerBlock;

		/**
		 * Construtor que inicializa um bloco com capacidade máxima.
		 *
		 * @param qtdmax Capacidade máxima do bloco.
		 */
		public Block(int qtdmax) {
			num = 0;
			max = (short) qtdmax;
			items = new InvertedListRegister[max];
			next = -1;
			bytesPerBlock = (short) (2 + 4 * max + 8);
		}

		/**
		 * Converte o bloco em um array de bytes.
		 *
		 * @return Array de bytes representando o bloco.
		 * @throws IOException Se ocorrer um erro de I/O.
		 */
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

		/**
		 * Inicializa o bloco a partir de um array de bytes.
		 *
		 * @param ba Array de bytes a ser lido.
		 * @throws IOException Se ocorrer um erro de I/O.
		 */
		public void fromByteArray(byte[] ba) throws IOException {
			ByteArrayInputStream bais = new ByteArrayInputStream(ba);
			DataInputStream dis = new DataInputStream(bais);
			num = dis.readShort();
			for (int i = 0; i < max; ++i)
				items[i] = new InvertedListRegister(dis.readInt());
			next = dis.readLong();
		}

		/**
		 * Adiciona um registro ao bloco.
		 *
		 * @param e Registro a ser adicionado.
		 * @return true se o registro foi adicionado com sucesso, false se o bloco
		 *         estiver cheio.
		 */
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

		/**
		 * Verifica se um ID está presente no bloco.
		 *
		 * @param id ID a ser verificado.
		 * @return true se o ID estiver presente, false caso contrário.
		 */
		public boolean test(int id) {
			if (empty())
				return false;
			int i;
			for (i = 0; i < num && id > items[i].getId(); ++i)
				;
			return i < num && id == items[i].getId();
		}

		/**
		 * Remove um registro do bloco pelo ID.
		 *
		 * @param id ID do registro a ser removido.
		 * @return true se o registro foi removido, false se não foi encontrado.
		 */
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

		/**
		 * Obtém o último registro do bloco.
		 *
		 * @return Último registro do bloco.
		 */
		public InvertedListRegister last() {
			return items[num - 1];
		}

		/**
		 * Lista todos os registros presentes no bloco.
		 *
		 * @return Array de registros presentes no bloco.
		 */
		public InvertedListRegister[] list() {
			InvertedListRegister[] res = new InvertedListRegister[num];
			for (int i = 0; i < num; i++)
				res[i] = new InvertedListRegister(items[i]);
			return res;
		}

		/**
		 * Verifica se o bloco está vazio.
		 *
		 * @return true se o bloco estiver vazio, false caso contrário.
		 */
		public boolean empty() {
			return num == 0;
		}

		/**
		 * Verifica se o bloco está cheio.
		 *
		 * @return true se o bloco estiver cheio, false caso contrário.
		 */
		public boolean full() {
			return num == max;
		}

		/**
		 * Obtém o endereço do próximo bloco.
		 *
		 * @return Endereço do próximo bloco.
		 */
		public long next() {
			return next;
		}

		/**
		 * Define o endereço do próximo bloco.
		 *
		 * @param p Endereço do próximo bloco.
		 */
		public void setNext(long p) {
			next = p;
		}

		/**
		 * Obtém o tamanho do bloco em bytes.
		 *
		 * @return Tamanho do bloco em bytes.
		 */
		public int size() {
			return bytesPerBlock;
		}
	}

	/**
	 * Construtor que inicializa o índice de lista invertida com os caminhos dos
	 * arquivos.
	 *
	 * @param dictionaryFilePath Caminho para o arquivo de dicionário.
	 * @param blockFilePath      Caminho para o arquivo de blocos.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	public InvertedListIndex(String dictionaryFilePath, String blockFilePath) throws IOException {
		// XOR garante que se um arquivo existe, o outro também existe.
		if (Files.exists(Paths.get(dictionaryFilePath)) ^ Files.exists(Paths.get(blockFilePath)))
			throw new FileNotFoundException("Um dos arquivos de Lista Invertida não existe.");

		this.dictionaryFilePath = dictionaryFilePath;
		this.blockFilePath = blockFilePath;

		dict = new RandomAccessFile(dictionaryFilePath, "rw");
		if (dict.length() < 4) { // cabeçalho do arquivo com número de entidades
			dict.seek(0);
			dict.writeInt(0);
		}
		blocks = new RandomAccessFile(blockFilePath, "rw");
	}

	/**
	 * Cria um novo registro na lista invertida para uma palavra e ID.
	 *
	 * @param word Palavra chave.
	 * @param id   ID do registro.
	 * @return true se o registro foi criado com sucesso, false se já existir.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	public boolean create(String word, int id) throws IOException {
		return this.create(word, new InvertedListRegister(id));
	}

	// Insere um dado na lista da chave de forma NÃO ORDENADA
	/**
	 * Insere um registro na lista da chave de forma NÃO ORDENADA.
	 *
	 * @param word Palavra chave.
	 * @param reg  Registro a ser inserido.
	 * @return true se o registro foi inserido com sucesso, false se já existir.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
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

	/**
	 * Lê todos os IDs associados a uma palavra chave.
	 *
	 * @param word Palavra chave.
	 * @return Array de IDs associados à palavra.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
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
	/**
	 * Retorna a lista de registros de uma determinada chave.
	 *
	 * @param c Palavra chave.
	 * @return Array de registros associados à palavra.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
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
	/**
	 * Remove o registro de uma chave pelo ID (não apaga a chave nem blocos).
	 *
	 * @param c  Palavra chave.
	 * @param id ID do registro a ser removido.
	 * @return true se o registro foi removido, false se não foi encontrado.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
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

	/**
	 * Fecha os arquivos e deleta os arquivos de dicionário e blocos.
	 *
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	public void destruct() throws IOException {
		blocks.close();
		dict.close();
		Files.delete(Paths.get(blockFilePath));
		Files.delete(Paths.get(dictionaryFilePath));
	}

	/**
	 * Obtém o caminho do arquivo de dicionário.
	 *
	 * @return Caminho do arquivo de dicionário.
	 */
	public String getDirFilePath() {
		return this.dictionaryFilePath;
	}

	/**
	 * Obtém o caminho do arquivo de blocos.
	 *
	 * @return Caminho do arquivo de blocos.
	 */
	public String getBlockFilePath() {
		return this.blockFilePath;
	}

	/**
	 * Lista os caminhos dos arquivos de dicionário e blocos.
	 *
	 * @return Array com os caminhos dos arquivos.
	 */
	public String[] listFilePaths() {
		return new String[] { dictionaryFilePath, blockFilePath };
	}

}
