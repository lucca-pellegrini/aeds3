package AEDs3.DataBase.Index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class HashTableIndex implements Index {
	String dirFilePath;
	String bucketFilePath;
	String metaFilePath;
	RandomAccessFile dirFile;
	RandomAccessFile bucketFile;
	int bucketNumElements;
	Directory directory;

	private class Bucket {
		short maxElements; // quantidade máxima de elementos que o cesto pode conter
		short elementSize; // tamanho fixo de cada elemento em bytes
		short bucketSize; // tamanho fixo do cesto em bytes

		byte localDepth; // profundidade local do cesto
		short numElements; // quantidade de elementos presentes no cesto
		ArrayList<IndexRegister> elements; // sequência de elementos armazenados

		public Bucket(int maxElements) {
			this(maxElements, 0);
		}

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
			this.elementSize = IndexRegister.SIZE;
			this.bucketSize = (short) (elementSize * maxElements + 3);
		}

		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeByte(localDepth);
			dos.writeShort(numElements);
			int i = 0;
			while (i < numElements) {
				dos.write(elements.get(i).toByteArray());
				i++;
			}
			byte[] vazio = new byte[elementSize];
			while (i < maxElements) {
				dos.write(vazio);
				i++;
			}
			return baos.toByteArray();
		}

		public void fromByteArray(byte[] buf) throws IOException {
			ByteArrayInputStream bais = new ByteArrayInputStream(buf);
			DataInputStream dis = new DataInputStream(bais);
			localDepth = dis.readByte();
			numElements = dis.readShort();
			int i = 0;
			elements = new ArrayList<>(maxElements);
			byte[] newBuf = new byte[elementSize];
			IndexRegister element;
			while (i < maxElements) {
				dis.read(newBuf);
				element = new IndexRegister();
				element.fromByteArray(newBuf);
				elements.add(element);
				i++;
			}
		}

		// Inserir elementos no cesto
		public void insert(IndexRegister register) {
			if (isFull())
				throw new IllegalStateException("Bucket já está cheio.");
			int i = numElements - 1; // posição do último elemento no cesto
			while (i >= 0 && register.hashCode() < elements.get(i).hashCode())
				i--;
			elements.add(i + 1, register);
			numElements += 1;
		}

		// Buscar um elemento no cesto
		public IndexRegister search(int id) {
			if (isEmpty())
				return null;
			int i = 0;
			while (i < numElements && id > elements.get(i).hashCode())
				i++;
			if (i < numElements && id == elements.get(i).hashCode())
				return elements.get(i);
			else
				return null;
		}

		// pagar um elemento do cesto
		public boolean delete(int id) {
			if (isEmpty())
				return false;
			int i = 0;
			while (i < numElements && id > elements.get(i).hashCode())
				i++;
			if (id == elements.get(i).hashCode()) {
				elements.remove(i);
				numElements--;
				return true;
			} else
				return false;
		}

		public boolean isEmpty() {
			return numElements == 0;
		}

		public boolean isFull() {
			return numElements == maxElements;
		}

		public int getSize() {
			return bucketSize;
		}
	}

	protected class Directory {
		byte globalDepth;
		long[] addresses;

		public Directory() {
			globalDepth = 0;
			addresses = new long[1];
			addresses[0] = 0;
		}

		public boolean updateAddresses(int pos, long newAddress) {
			if (pos > Math.pow(2, globalDepth))
				return false;
			addresses[pos] = newAddress;
			return true;
		}

		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeByte(globalDepth);
			int n = (int) Math.pow(2, globalDepth);
			int i = 0;
			while (i < n) {
				dos.writeLong(addresses[i]);
				i++;
			}
			return baos.toByteArray();
		}

		public void fromByteArray(byte[] buf) throws IOException {
			ByteArrayInputStream bais = new ByteArrayInputStream(buf);
			DataInputStream dis = new DataInputStream(bais);
			globalDepth = dis.readByte();
			int n = (int) Math.pow(2, globalDepth);
			addresses = new long[n];
			int i = 0;
			while (i < n) {
				addresses[i] = dis.readLong();
				i++;
			}
		}

		protected long address(int pos) {
			if (pos > Math.pow(2, globalDepth))
				return -1;
			return addresses[pos];
		}

		protected boolean duplicate() {
			if (globalDepth == 0xFF / 2)
				return false;
			globalDepth++;
			int q1 = (int) Math.pow(2, globalDepth - 1.);
			int q2 = (int) Math.pow(2, globalDepth);
			long[] newAddresses = new long[q2];
			int i = 0;
			while (i < q1) { // copia o vetor anterior para a primeiro metade do novo vetor
				newAddresses[i] = addresses[i];
				i += 1;
			}
			while (i < q2) { // copia o vetor anterior para a segunda metade do novo vetor
				newAddresses[i] = addresses[i - q1];
				i += 1;
			}
			addresses = newAddresses;
			return true;
		}

		// Para efeito de determinar o cesto em que o elemento deve ser inserido,
		// só serão considerados valores absolutos da chave.
		protected int hash(int chave) {
			return Math.abs(chave) % (int) Math.pow(2, globalDepth);
		}

		// Método auxiliar para atualizar endereço ao duplicar o diretório
		protected int localHash(int id, int localDepth) { // cálculo do hash para uma dada profundidade local
			return Math.abs(id) % (int) Math.pow(2, localDepth);
		}
	}

	public HashTableIndex(String nc, String nd, String nm) throws IOException {
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
			byte[] bd = directory.toByteArray();
			dirFile.write(bd);

			// Cria um cesto vazio, já apontado pelo único elemento do diretório
			Bucket c = new Bucket(bucketNumElements);
			bd = c.toByteArray();
			bucketFile.seek(0);
			bucketFile.write(bd);
		}
	}

	public HashTableIndex(int n, String nc, String nd, String nm) throws IOException {
		bucketNumElements = n;
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
			byte[] bd = directory.toByteArray();
			dirFile.write(bd);

			// Cria um cesto vazio, já apontado pelo único elemento do diretório
			Bucket c = new Bucket(bucketNumElements);
			bd = c.toByteArray();
			bucketFile.seek(0);
			bucketFile.write(bd);
		}
	}

	public void destruct() throws IOException {
		dirFile.close();
		bucketFile.close();

		Files.delete(Paths.get(bucketFilePath));
		Files.delete(Paths.get(dirFilePath));
		Files.delete(Paths.get(metaFilePath));
	}

	public void insert(int id, long pos) throws IOException {
		this.insert(new IndexRegister(id, pos));
	}

	private void insert(IndexRegister elem) throws IOException {
		byte[] bd = new byte[(int) dirFile.length()];
		dirFile.seek(0);
		dirFile.read(bd);
		directory = new Directory();
		directory.fromByteArray(bd);

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
		int displacement = (int) Math.pow(2, localDepth);
		int max = (int) Math.pow(2, globalDepth);
		boolean troca = false;
		for (int j = begin; j < max; j += displacement) {
			if (troca)
				directory.updateAddresses(j, newAddress);
			troca = !troca;
		}

		// Atualiza o arquivo do diretório
		bd = directory.toByteArray();
		dirFile.seek(0);
		dirFile.write(bd);

		// Reinsere as chaves do cesto antigo
		for (int j = 0; j < bucket.numElements; j++) {
			insert(bucket.elements.get(j));
		}
		insert(elem); // insere o nome elemento
	}

	public long search(int id) throws IOException {
		// Carrega o diretório
		byte[] bd = new byte[(int) dirFile.length()];
		dirFile.seek(0);
		dirFile.read(bd);
		directory = new Directory();
		directory.fromByteArray(bd);

		// Identifica a hash do diretório,
		int i = directory.hash(id);

		// Recupera o cesto
		long enderecoCesto = directory.address(i);
		Bucket c = new Bucket(bucketNumElements);
		byte[] ba = new byte[c.getSize()];
		bucketFile.seek(enderecoCesto);
		bucketFile.read(ba);
		c.fromByteArray(ba);

		IndexRegister res = c.search(id);
		return res != null ? res.getPos() : -1;
	}

	public void delete(int id) throws IOException {
		// Carrega o diretório
		byte[] bd = new byte[(int) dirFile.length()];
		dirFile.seek(0);
		dirFile.read(bd);
		directory = new Directory();
		directory.fromByteArray(bd);

		// Identifica a hash do diretório,
		int i = directory.hash(id);

		// Recupera o cesto
		long enderecoCesto = directory.address(i);
		Bucket c = new Bucket(bucketNumElements);
		byte[] ba = new byte[c.getSize()];
		bucketFile.seek(enderecoCesto);
		bucketFile.read(ba);
		c.fromByteArray(ba);

		// delete a chave
		if (c.delete(id)) {
			// Atualiza o cesto
			bucketFile.seek(enderecoCesto);
			bucketFile.write(c.toByteArray());
		}
	}

	public int getBucketCapacity() {
		return this.bucketNumElements;
	}
}
