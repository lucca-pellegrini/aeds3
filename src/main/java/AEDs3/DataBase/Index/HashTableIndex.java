package AEDs3.DataBase.Index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Implementação de um índice de tabela hash extensível.
 * Gerencia a inserção, busca e remoção de registros em uma estrutura de hash.
 */
public class HashTableIndex implements ForwardIndex {
	/**
	 * Caminho para o arquivo de diretório.
	 */
	String dirFilePath;

	/**
	 * Caminho para o arquivo de cestos.
	 */
	String bucketFilePath;

	/**
	 * Caminho para o arquivo de metadados.
	 */
	String metaFilePath;

	/**
	 * Arquivo de acesso aleatório para o diretório.
	 */
	RandomAccessFile dirFile;

	/**
	 * Arquivo de acesso aleatório para os cestos.
	 */
	RandomAccessFile bucketFile;

	/**
	 * Número máximo de elementos por cesto.
	 */
	int bucketNumElements;

	/**
	 * Diretório da tabela hash.
	 */
	Directory directory;

	/**
	 * Classe interna que representa um cesto (bucket) na tabela hash.
	 * Armazena registros de índice e gerencia a inserção, busca e remoção dentro do
	 * cesto.
	 */
	private class Bucket {
		/**
		 * Número máximo de elementos que o cesto pode conter.
		 */
		short maxElements;

		/**
		 * Tamanho de cada elemento no cesto.
		 */
		short elementSize;

		/**
		 * Tamanho total do cesto.
		 */
		short bucketSize;

		/**
		 * Profundidade local do cesto.
		 */
		byte localDepth;

		/**
		 * Número atual de elementos no cesto.
		 */
		short numElements;

		/**
		 * Lista de registros de índice armazenados no cesto.
		 */
		ArrayList<ForwardIndexRegister> elements;

		/**
		 * Construtor do cesto com profundidade local padrão.
		 *
		 * @param maxElements Número máximo de elementos no cesto.
		 */
		public Bucket(int maxElements) {
			this(maxElements, 0);
		}

		/**
		 * Construtor do cesto com profundidade local especificada.
		 *
		 * @param maxElements Número máximo de elementos no cesto.
		 * @param localDepth  Profundidade local do cesto.
		 */
		public Bucket(int maxElements, int localDepth) {
			if (maxElements > Short.MAX_VALUE)
				throw new IllegalArgumentException(
						"Quantidade máxima de " + Short.MAX_VALUE + " elementos");
			if (localDepth > 0xFF / 2)
				throw new IllegalArgumentException(
						"Profundidade local máxima de " + 0xFF / 2 + " bits");
			this.localDepth = (byte) localDepth;
			this.numElements = 0;
			this.maxElements = (short) maxElements;
			this.elements = new ArrayList<>(maxElements);
			this.elementSize = ForwardIndexRegister.SIZE;
			this.bucketSize = (short) (elementSize * maxElements + 3);
		}

		/**
		 * Converte o cesto para um array de bytes.
		 *
		 * @return Array de bytes representando o cesto.
		 * @throws IOException Se ocorrer um erro de I/O.
		 */
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeByte(localDepth);
			dos.writeShort(numElements);
			for (int i = 0; i < numElements; i++)
				dos.write(elements.get(i).toByteArray());
			byte[] buf = new byte[elementSize];
			for (int i = numElements; i < maxElements; i++)
				dos.write(buf);
			return baos.toByteArray();
		}

		/**
		 * Inicializa o cesto a partir de um array de bytes.
		 *
		 * @param buf Array de bytes representando o cesto.
		 * @throws IOException Se ocorrer um erro de I/O.
		 */
		public void fromByteArray(byte[] buf) throws IOException {
			ByteArrayInputStream bais = new ByteArrayInputStream(buf);
			DataInputStream dis = new DataInputStream(bais);
			localDepth = dis.readByte();
			numElements = dis.readShort();
			elements = new ArrayList<>(maxElements);
			byte[] newBuf = new byte[elementSize];
			ForwardIndexRegister element;
			for (int i = 0; i < maxElements; ++i) {
				dis.read(newBuf);
				element = new ForwardIndexRegister();
				element.fromByteArray(newBuf);
				elements.add(element);
			}
		}

		/**
		 * Insere um registro no cesto.
		 *
		 * @param register Registro a ser inserido.
		 */
		public void insert(ForwardIndexRegister register) {
			if (isFull())
				throw new IllegalStateException("Bucket já está cheio.");
			int i;
			for (i = numElements - 1; i >= 0 && register.hashCode() < elements.get(i).hashCode(); --i)
				;
			elements.add(i + 1, register);
			numElements += 1;
		}

		/**
		 * Busca um registro no cesto.
		 *
		 * @param id Identificador do registro.
		 * @return Registro encontrado ou null se não encontrado.
		 */
		public ForwardIndexRegister search(int id) {
			if (isEmpty())
				return null;
			int i;
			for (i = 0; i < numElements && id > elements.get(i).hashCode(); ++i)
				;
			if (i < numElements && id == elements.get(i).hashCode())
				return elements.get(i);
			else
				return null;
		}

		/**
		 * Remove um registro do cesto.
		 *
		 * @param id Identificador do registro.
		 * @return true se o registro foi removido, false caso contrário.
		 */
		public boolean delete(int id) {
			if (isEmpty())
				return false;
			int i;
			for (i = 0; i < numElements && id > elements.get(i).hashCode(); ++i)
				;
			if (id == elements.get(i).hashCode()) {
				elements.remove(i);
				numElements -= 1;
				return true;
			}
			return false;
		}

		/**
		 * Verifica se o cesto está vazio.
		 *
		 * @return true se o cesto está vazio, false caso contrário.
		 */
		public boolean isEmpty() {
			return numElements == 0;
		}

		/**
		 * Verifica se o cesto está cheio.
		 *
		 * @return true se o cesto está cheio, false caso contrário.
		 */
		public boolean isFull() {
			return numElements == maxElements;
		}

		/**
		 * Retorna o tamanho do cesto.
		 *
		 * @return Tamanho do cesto.
		 */
		public int getSize() {
			return bucketSize;
		}
	}

	/**
	 * Classe interna que representa o diretório da tabela hash.
	 * Gerencia a profundidade global e os endereços dos cestos.
	 */
	private class Directory {
		/**
		 * Profundidade global do diretório.
		 */
		byte globalDepth;

		/**
		 * Endereços dos cestos no diretório.
		 */
		long[] addresses;

		/**
		 * Construtor do diretório, inicializa com profundidade global zero.
		 */
		public Directory() {
			globalDepth = 0;
			addresses = new long[1];
			addresses[0] = 0;
		}

		/**
		 * Atualiza o endereço de um cesto no diretório.
		 *
		 * @param pos Posição do cesto no diretório.
		 * @param newAddress Novo endereço do cesto.
		 */
		public void updateAddresses(int pos, long newAddress) {
			if (pos > 1 << globalDepth)
				return;
			addresses[pos] = newAddress;
		}

		/**
		 * Converte o diretório para um array de bytes.
		 *
		 * @return Array de bytes representando o diretório.
		 * @throws IOException Se ocorrer um erro de I/O.
		 */
		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeByte(globalDepth);
			int n = 1 << globalDepth;
			for (int i = 0; i < n; ++i)
				dos.writeLong(addresses[i]);
			return baos.toByteArray();
		}

		/**
		 * Inicializa o diretório a partir de um array de bytes.
		 *
		 * @param buf Array de bytes representando o diretório.
		 * @throws IOException Se ocorrer um erro de I/O.
		 */
		public void fromByteArray(byte[] buf) throws IOException {
			ByteArrayInputStream bais = new ByteArrayInputStream(buf);
			DataInputStream dis = new DataInputStream(bais);
			globalDepth = dis.readByte();
			int n = 1 << globalDepth;
			addresses = new long[n];
			for (int i = 0; i < n; ++i)
				addresses[i] = dis.readLong();
		}

		/**
		 * Retorna o endereço de um cesto no diretório.
		 *
		 * @param pos Posição do cesto no diretório.
		 * @return Endereço do cesto ou -1 se a posição for inválida.
		 */
		protected long address(int pos) {
			if (pos > 1 << globalDepth)
				return -1;
			return addresses[pos];
		}

		/**
		 * Duplica o diretório, aumentando a profundidade global.
		 */
		protected void duplicate() {
			if (globalDepth == 0xFF / 2)
				return;
			globalDepth += 1;
			int q1 = 1 << (globalDepth - 1);
			int q2 = 1 << globalDepth;
			long[] newAddresses = new long[q2];

			// Copia o vetor anterior para a primeira metade do novo vetor.
			System.arraycopy(addresses, 0, newAddresses, 0, q1);

			// Copia o vetor anterior para a segunda metade do novo vetor.
			for (int i = q1; i < q2; i++)
				newAddresses[i] = addresses[i - q1];

			addresses = newAddresses;
		}

		/**
		 * Calcula o hash para determinar o cesto de um elemento.
		 *
		 * @param id Identificador do elemento.
		 * @return Hash calculado.
		 */
		protected int hash(int id) {
			return Math.abs(id) % (1 << globalDepth);
		}

		/**
		 * Calcula o hash local para atualização de endereço ao duplicar o diretório.
		 *
		 * @param id Identificador do elemento.
		 * @param localDepth Profundidade local do cesto.
		 * @return Hash local calculado.
		 */
		protected int localHash(int id, int localDepth) {
			return Math.abs(id) % (1 << localDepth);
		}
	}

	/**
	 * Construtor que inicializa o índice de tabela hash a partir de arquivos
	 * existentes.
	 *
	 * @param nc Caminho para o arquivo de cestos.
	 * @param nd Caminho para o arquivo de diretório.
	 * @param nm Caminho para o arquivo de metadados.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	public HashTableIndex(String nc, String nd, String nm) throws IOException {
		if (!Files.exists(Paths.get(nc)) || !Files.exists(Paths.get(nd)) || !Files.exists(Paths.get(nm)))
			throw new FileNotFoundException("Um ou mais arquivos de Hash Extensível inexistente(s).");

		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(nm))) {
			bucketNumElements = in.readInt();
		}

		dirFilePath = nd;
		bucketFilePath = nc;
		metaFilePath = nm;

		dirFile = new RandomAccessFile(dirFilePath, "rw");
		bucketFile = new RandomAccessFile(bucketFilePath, "rw");

		// Se o diretório ou os cestos estiverem vazios, cria um novo diretório e lista
		// de cestos
		if (dirFile.length() == 0 || bucketFile.length() == 0) {
			// Cria um novo diretório, com profundidade de 0 bits (1 único elemento)
			directory = new Directory();
			byte[] dirBuf = directory.toByteArray();
			dirFile.write(dirBuf);

			// Cria um cesto vazio, já apontado pelo único elemento do diretório
			Bucket bucket = new Bucket(bucketNumElements);
			dirBuf = bucket.toByteArray();
			bucketFile.seek(0);
			bucketFile.write(dirBuf);
		}
	}

	/**
	 * Construtor que inicializa um novo índice de tabela hash.
	 *
	 * @param bucketCapacity Número máximo de elementos por cesto.
	 * @param nc             Caminho para o arquivo de cestos.
	 * @param nd             Caminho para o arquivo de diretório.
	 * @param nm             Caminho para o arquivo de metadados.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	public HashTableIndex(int bucketCapacity, String nc, String nd, String nm) throws IOException {
		if (bucketCapacity >= 8192)
			throw new InvalidHashTableCapacityException(
					bucketCapacity, InvalidHashTableCapacityException.Reason.TOO_LARGE);

		bucketNumElements = bucketCapacity;
		dirFilePath = nd;
		bucketFilePath = nc;
		metaFilePath = nm;

		dirFile = new RandomAccessFile(dirFilePath, "rw");
		bucketFile = new RandomAccessFile(bucketFilePath, "rw");

		// Se o diretório ou os cestos estiverem vazios, cria um novo diretório e lista
		// de cestos
		if (dirFile.length() == 0 || bucketFile.length() == 0) {
			try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(nm))) {
				out.writeInt(bucketNumElements);
			}

			// Cria um novo diretório, com profundidade de 0 bits (1 único elemento)
			directory = new Directory();
			byte[] dirBuf = directory.toByteArray();
			dirFile.write(dirBuf);

			// Cria um cesto vazio, já apontado pelo único elemento do diretório
			Bucket bucket = new Bucket(bucketNumElements);
			dirBuf = bucket.toByteArray();
			bucketFile.seek(0);
			bucketFile.write(dirBuf);
		}
	}

	/**
	 * Destrói o índice de tabela hash, fechando arquivos e removendo-os do sistema.
	 *
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	public void destruct() throws IOException {
		dirFile.close();
		bucketFile.close();

		Files.delete(Paths.get(bucketFilePath));
		Files.delete(Paths.get(dirFilePath));
		Files.delete(Paths.get(metaFilePath));
	}

	/**
	 * Insere um novo registro no índice de tabela hash.
	 *
	 * @param id  Identificador do registro.
	 * @param pos Posição do registro no arquivo de dados.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	public void insert(int id, long pos) throws IOException {
		this.insert(new ForwardIndexRegister(id, pos));
	}

	/**
	 * Método auxiliar que insere um registro no índice de tabela hash.
	 *
	 * @param elem Registro a ser inserido.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	private void insert(ForwardIndexRegister elem) throws IOException {
		byte[] dirBuf = new byte[(int) dirFile.length()];
		dirFile.seek(0);
		dirFile.read(dirBuf);
		directory = new Directory();
		directory.fromByteArray(dirBuf);

		// Identifica a hash do diretório,
		int i = directory.hash(elem.hashCode());

		// Recupera o cesto
		long bucketAddress = directory.address(i);
		Bucket bucket = new Bucket(bucketNumElements);
		byte[] buf = new byte[bucket.getSize()];
		bucketFile.seek(bucketAddress);
		bucketFile.read(buf);
		bucket.fromByteArray(buf);

		// Testa se a chave já não existe no cesto
		if (bucket.search(elem.hashCode()) != null)
			throw new IllegalStateException("Elemento já existe");

		// Testa se o cesto já não está cheio
		// Se não estiver, insert o par de chave e dado
		if (!bucket.isFull()) {
			// Insere a chave no cesto e o atualiza
			bucket.insert(elem);
			bucketFile.seek(bucketAddress);
			bucketFile.write(bucket.toByteArray());
			return;
		}

		// Duplica o diretório
		byte localDepth = bucket.localDepth;
		if (localDepth >= directory.globalDepth)
			directory.duplicate();
		byte globalDepth = directory.globalDepth;

		// Cria os novos cestos, com os seus dados no arquivo de cestos
		Bucket bucket1 = new Bucket(bucketNumElements, localDepth + 1);
		bucketFile.seek(bucketAddress);
		bucketFile.write(bucket1.toByteArray());

		Bucket bucket2 = new Bucket(bucketNumElements, localDepth + 1);
		long newAddress = bucketFile.length();
		bucketFile.seek(newAddress);
		bucketFile.write(bucket2.toByteArray());

		// Atualiza os dados no diretório
		int begin = directory.localHash(elem.hashCode(), bucket.localDepth);
		int displacement = 1 << localDepth;
		int max = 1 << globalDepth;
		boolean troca = false;
		for (int j = begin; j < max; j += displacement) {
			if (troca)
				directory.updateAddresses(j, newAddress);
			troca = !troca;
		}

		// Atualiza o arquivo do diretório
		dirBuf = directory.toByteArray();
		dirFile.seek(0);
		dirFile.write(dirBuf);

		// Reinsere as chaves do cesto antigo
		for (int j = 0; j < bucket.numElements; j++) {
			insert(bucket.elements.get(j));
		}
		insert(elem); // insere o nome elemento
	}

	/**
	 * Busca um registro no índice de tabela hash.
	 *
	 * @param id Identificador do registro a ser buscado.
	 * @return A posição do registro no arquivo de dados, ou -1 se não encontrado.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	public long search(int id) throws IOException {
		// Carrega o diretório
		byte[] dirBuf = new byte[(int) dirFile.length()];
		dirFile.seek(0);
		dirFile.read(dirBuf);
		directory = new Directory();
		directory.fromByteArray(dirBuf);

		// Identifica a hash do diretório,
		int i = directory.hash(id);

		// Recupera o cesto
		long enderecoCesto = directory.address(i);
		Bucket bucket = new Bucket(bucketNumElements);
		byte[] ba = new byte[bucket.getSize()];
		bucketFile.seek(enderecoCesto);
		bucketFile.read(ba);
		bucket.fromByteArray(ba);

		ForwardIndexRegister res = bucket.search(id);
		return res != null ? res.getPos() : -1;
	}

	/**
	 * Remove um registro do índice de tabela hash.
	 *
	 * @param id Identificador do registro a ser removido.
	 * @throws IOException Se ocorrer um erro de I/O.
	 */
	public void delete(int id) throws IOException {
		// Carrega o diretório
		byte[] dirBuf = new byte[(int) dirFile.length()];
		dirFile.seek(0);
		dirFile.read(dirBuf);
		directory = new Directory();
		directory.fromByteArray(dirBuf);

		// Identifica a hash do diretório,
		int i = directory.hash(id);

		// Recupera o cesto
		long enderecoCesto = directory.address(i);
		Bucket bucket = new Bucket(bucketNumElements);
		byte[] ba = new byte[bucket.getSize()];
		bucketFile.seek(enderecoCesto);
		bucketFile.read(ba);
		bucket.fromByteArray(ba);

		// delete a chave
		if (bucket.delete(id)) {
			// Atualiza o cesto
			bucketFile.seek(enderecoCesto);
			bucketFile.write(bucket.toByteArray());
		}
	}

	/**
	 * Retorna a capacidade máxima de elementos por cesto.
	 *
	 * @return Capacidade máxima de elementos por cesto.
	 */
	public int getBucketCapacity() {
		return this.bucketNumElements;
	}

	/**
	 * Retorna os caminhos dos arquivos associados ao índice de tabela hash.
	 *
	 * @return Array de strings contendo os caminhos dos arquivos de diretório,
	 *         cestos e metadados.
	 */
	public String[] listFilePaths() {
		return new String[] { dirFilePath, bucketFilePath, metaFilePath };
	}
}
