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
	String nomeArquivoDiretorio;
	String nomeArquivoCestos;
	String nomeArquivoMeta;
	RandomAccessFile arqDiretorio;
	RandomAccessFile arqCestos;
	int quantidadeDadosPorCesto;
	Diretorio diretorio;

	public class Cesto {
		short quantidadeMaxima; // quantidade máxima de elementos que o cesto pode conter
		short bytesPorElemento; // tamanho fixo de cada elemento em bytes
		short bytesPorCesto; // tamanho fixo do cesto em bytes

		byte profundidadeLocal; // profundidade local do cesto
		short quantidade; // quantidade de elementos presentes no cesto
		ArrayList<IndexRegister> elementos; // sequência de elementos armazenados

		public Cesto(int qtdmax) {
			this(qtdmax, 0);
		}

		public Cesto(int qtdmax, int pl) {
			if (qtdmax > Short.MAX_VALUE)
				throw new IllegalArgumentException(
						"Quantidade máxima de " + Short.MAX_VALUE + " elementos");
			if (pl > 0xFF / 2)
				throw new IllegalArgumentException(
						"Profundidade local máxima de " + 0xFF / 2 + " bits");
			profundidadeLocal = (byte) pl;
			quantidade = 0;
			quantidadeMaxima = (short) qtdmax;
			elementos = new ArrayList<>(quantidadeMaxima);
			bytesPorElemento = IndexRegister.SIZE;
			bytesPorCesto = (short) (bytesPorElemento * quantidadeMaxima + 3);
		}

		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeByte(profundidadeLocal);
			out.writeShort(quantidade);
			int i;
			for (i = 0; i < quantidade; ++i)
				elementos.get(i).writeExternal(out);
			byte[] vazio = new byte[bytesPorElemento];
			while (i++ < quantidadeMaxima)
				out.write(vazio);
			return baos.toByteArray();
		}

		public void fromByteArray(byte[] ba) throws IOException {
			ByteArrayInputStream bais = new ByteArrayInputStream(ba);
			DataInputStream dis = new DataInputStream(bais);
			profundidadeLocal = dis.readByte();
			quantidade = dis.readShort();
			int i = 0;
			elementos = new ArrayList<>(quantidadeMaxima);
			byte[] dados = new byte[bytesPorElemento];
			IndexRegister elem;
			while (i < quantidadeMaxima) {
				dis.read(dados);
				elem = new IndexRegister();
				elem.readExternal(new ObjectInputStream(new ByteArrayInputStream(dados)));
				elementos.add(elem);
				i++;
			}
		}

		// Inserir elementos no cesto
		public void insert(IndexRegister elem) {
			if (full())
				throw new IllegalStateException("Bucket já está cheio.");
			int i = quantidade - 1; // posição do último elemento no cesto
			while (i >= 0 && elem.hashCode() < elementos.get(i).hashCode())
				i--;
			elementos.add(i + 1, elem);
			quantidade += 1;
		}

		// Buscar um elemento no cesto
		public IndexRegister search(int chave) {
			if (empty())
				return null;
			int i = 0;
			while (i < quantidade && chave > elementos.get(i).hashCode())
				i++;
			if (i < quantidade && chave == elementos.get(i).hashCode())
				return elementos.get(i);
			else
				return null;
		}

		// pagar um elemento do cesto
		public boolean delete(int chave) {
			if (empty())
				return false;
			int i = 0;
			while (i < quantidade && chave > elementos.get(i).hashCode())
				i++;
			if (chave == elementos.get(i).hashCode()) {
				elementos.remove(i);
				quantidade--;
				return true;
			} else
				return false;
		}

		public boolean empty() {
			return quantidade == 0;
		}

		public boolean full() {
			return quantidade == quantidadeMaxima;
		}

		public int size() {
			return bytesPorCesto;
		}
	}

	protected class Diretorio {
		byte profundidadeGlobal;
		long[] enderecos;

		public Diretorio() {
			profundidadeGlobal = 0;
			enderecos = new long[1];
			enderecos[0] = 0;
		}

		public boolean atualizaEndereco(int p, long e) {
			if (p > Math.pow(2, profundidadeGlobal))
				return false;
			enderecos[p] = e;
			return true;
		}

		public byte[] toByteArray() throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeByte(profundidadeGlobal);
			int quantidade = (int) Math.pow(2, profundidadeGlobal);
			int i = 0;
			while (i < quantidade) {
				dos.writeLong(enderecos[i]);
				i++;
			}
			return baos.toByteArray();
		}

		public void fromByteArray(byte[] ba) throws IOException {
			ByteArrayInputStream bais = new ByteArrayInputStream(ba);
			DataInputStream dis = new DataInputStream(bais);
			profundidadeGlobal = dis.readByte();
			int quantidade = (int) Math.pow(2, profundidadeGlobal);
			enderecos = new long[quantidade];
			int i = 0;
			while (i < quantidade) {
				enderecos[i] = dis.readLong();
				i++;
			}
		}

		protected long address(int p) {
			if (p > Math.pow(2, profundidadeGlobal))
				return -1;
			return enderecos[p];
		}

		protected boolean duplica() {
			if (profundidadeGlobal == 0xFF / 2)
				return false;
			profundidadeGlobal++;
			int q1 = (int) Math.pow(2, profundidadeGlobal - 1.);
			int q2 = (int) Math.pow(2, profundidadeGlobal);
			long[] novosEnderecos = new long[q2];
			int i = 0;
			while (i < q1) { // copia o vetor anterior para a primeiro metade do novo vetor
				novosEnderecos[i] = enderecos[i];
				i += 1;
			}
			while (i < q2) { // copia o vetor anterior para a segunda metade do novo vetor
				novosEnderecos[i] = enderecos[i - q1];
				i += 1;
			}
			enderecos = novosEnderecos;
			return true;
		}

		// Para efeito de determinar o cesto em que o elemento deve ser inserido,
		// só serão considerados valores absolutos da chave.
		protected int hash(int chave) {
			return Math.abs(chave) % (int) Math.pow(2, profundidadeGlobal);
		}

		// Método auxiliar para atualizar endereço ao duplicar o diretório
		protected int hash2(int chave, int pl) { // cálculo do hash para uma dada profundidade local
			return Math.abs(chave) % (int) Math.pow(2, pl);
		}
	}

	public HashTableIndex(String nc, String nd, String nm) throws IOException {
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(nm))) {
			quantidadeDadosPorCesto = in.readInt();
		}

		nomeArquivoDiretorio = nd;
		nomeArquivoCestos = nc;
		nomeArquivoMeta = nm;

		arqDiretorio = new RandomAccessFile(nomeArquivoDiretorio, "rw");
		arqCestos = new RandomAccessFile(nomeArquivoCestos, "rw");

		// Se o diretório ou os cestos estiverem vazios, cria um novo diretório e lista
		// de cestos
		if (arqDiretorio.length() == 0 || arqCestos.length() == 0) {
			// Cria um novo diretório, com profundidade de 0 bits (1 único elemento)
			diretorio = new Diretorio();
			byte[] bd = diretorio.toByteArray();
			arqDiretorio.write(bd);

			// Cria um cesto vazio, já apontado pelo único elemento do diretório
			Cesto c = new Cesto(quantidadeDadosPorCesto);
			bd = c.toByteArray();
			arqCestos.seek(0);
			arqCestos.write(bd);
		}
	}

	public HashTableIndex(int n, String nc, String nd, String nm) throws IOException {
		quantidadeDadosPorCesto = n;
		nomeArquivoDiretorio = nd;
		nomeArquivoCestos = nc;
		nomeArquivoMeta = nm;

		arqDiretorio = new RandomAccessFile(nomeArquivoDiretorio, "rw");
		arqCestos = new RandomAccessFile(nomeArquivoCestos, "rw");

		// Se o diretório ou os cestos estiverem vazios, cria um novo diretório e lista
		// de cestos
		if (arqDiretorio.length() == 0 || arqCestos.length() == 0) {
			try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(nm))) {
				out.writeInt(quantidadeDadosPorCesto);
			}

			// Cria um novo diretório, com profundidade de 0 bits (1 único elemento)
			diretorio = new Diretorio();
			byte[] bd = diretorio.toByteArray();
			arqDiretorio.write(bd);

			// Cria um cesto vazio, já apontado pelo único elemento do diretório
			Cesto c = new Cesto(quantidadeDadosPorCesto);
			bd = c.toByteArray();
			arqCestos.seek(0);
			arqCestos.write(bd);
		}
	}

	public void destruct() throws IOException {
		arqDiretorio.close();
		arqCestos.close();

		Files.delete(Paths.get(nomeArquivoCestos));
		Files.delete(Paths.get(nomeArquivoDiretorio));
		Files.delete(Paths.get(nomeArquivoMeta));
	}

	public void insert(int id, long pos) throws IOException {
		this.insert(new IndexRegister(id, pos));
	}

	private void insert(IndexRegister elem) throws IOException {
		byte[] bd = new byte[(int) arqDiretorio.length()];
		arqDiretorio.seek(0);
		arqDiretorio.read(bd);
		diretorio = new Diretorio();
		diretorio.fromByteArray(bd);

		// Identifica a hash do diretório,
		int i = diretorio.hash(elem.hashCode());

		// Recupera o cesto
		long enderecoCesto = diretorio.address(i);
		Cesto c = new Cesto(quantidadeDadosPorCesto);
		byte[] ba = new byte[c.size()];
		arqCestos.seek(enderecoCesto);
		arqCestos.read(ba);
		c.fromByteArray(ba);

		// Testa se a chave já não existe no cesto
		if (c.search(elem.hashCode()) != null)
			throw new IllegalStateException("Elemento já existe");

		// Testa se o cesto já não está cheio
		// Se não estiver, insert o par de chave e dado
		if (!c.full()) {
			// Insere a chave no cesto e o atualiza
			c.insert(elem);
			arqCestos.seek(enderecoCesto);
			arqCestos.write(c.toByteArray());
			return;
		}

		// Duplica o diretório
		byte pl = c.profundidadeLocal;
		if (pl >= diretorio.profundidadeGlobal)
			diretorio.duplica();
		byte pg = diretorio.profundidadeGlobal;

		// Cria os novos cestos, com os seus dados no arquivo de cestos
		Cesto c1 = new Cesto(quantidadeDadosPorCesto, pl + 1);
		arqCestos.seek(enderecoCesto);
		arqCestos.write(c1.toByteArray());

		Cesto c2 = new Cesto(quantidadeDadosPorCesto, pl + 1);
		long novoEndereco = arqCestos.length();
		arqCestos.seek(novoEndereco);
		arqCestos.write(c2.toByteArray());

		// Atualiza os dados no diretório
		int inicio = diretorio.hash2(elem.hashCode(), c.profundidadeLocal);
		int deslocamento = (int) Math.pow(2, pl);
		int max = (int) Math.pow(2, pg);
		boolean troca = false;
		for (int j = inicio; j < max; j += deslocamento) {
			if (troca)
				diretorio.atualizaEndereco(j, novoEndereco);
			troca = !troca;
		}

		// Atualiza o arquivo do diretório
		bd = diretorio.toByteArray();
		arqDiretorio.seek(0);
		arqDiretorio.write(bd);

		// Reinsere as chaves do cesto antigo
		for (int j = 0; j < c.quantidade; j++) {
			insert(c.elementos.get(j));
		}
		insert(elem); // insere o nome elemento
	}

	public long search(int chave) throws IOException {
		// Carrega o diretório
		byte[] bd = new byte[(int) arqDiretorio.length()];
		arqDiretorio.seek(0);
		arqDiretorio.read(bd);
		diretorio = new Diretorio();
		diretorio.fromByteArray(bd);

		// Identifica a hash do diretório,
		int i = diretorio.hash(chave);

		// Recupera o cesto
		long enderecoCesto = diretorio.address(i);
		Cesto c = new Cesto(quantidadeDadosPorCesto);
		byte[] ba = new byte[c.size()];
		arqCestos.seek(enderecoCesto);
		arqCestos.read(ba);
		c.fromByteArray(ba);

		IndexRegister res = c.search(chave);
		return res != null ? res.getPos() : -1;
	}

	public void delete(int chave) throws IOException {
		// Carrega o diretório
		byte[] bd = new byte[(int) arqDiretorio.length()];
		arqDiretorio.seek(0);
		arqDiretorio.read(bd);
		diretorio = new Diretorio();
		diretorio.fromByteArray(bd);

		// Identifica a hash do diretório,
		int i = diretorio.hash(chave);

		// Recupera o cesto
		long enderecoCesto = diretorio.address(i);
		Cesto c = new Cesto(quantidadeDadosPorCesto);
		byte[] ba = new byte[c.size()];
		arqCestos.seek(enderecoCesto);
		arqCestos.read(ba);
		c.fromByteArray(ba);

		// delete a chave
		if (c.delete(chave)) {
			// Atualiza o cesto
			arqCestos.seek(enderecoCesto);
			arqCestos.write(c.toByteArray());
		}
	}

	public int getBucketCapacity() {
		return this.quantidadeDadosPorCesto;
	}
}
