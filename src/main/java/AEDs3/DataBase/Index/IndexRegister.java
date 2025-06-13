package AEDs3.DataBase.Index;

import java.io.*;

/**
 * Classe que representa um registro de índice, contendo um ID e uma posição.
 */
class IndexRegister implements Externalizable, Comparable<IndexRegister> {
	/**
	 * ID do registro de índice.
	 */
	int id;

	/**
	 * Posição do registro no arquivo.
	 */
	long pos;

	/**
	 * Tamanho do registro de índice em bytes.
	 */
	static final int SIZE = (Integer.SIZE + Long.SIZE) / 8;

	/**
	 * Construtor padrão para criação de um registro de índice vazio.
	 */
	public IndexRegister() {
	}

	/**
	 * Construtor para criação de um registro de índice com ID e posição
	 * especificados.
	 *
	 * @param id  O ID do registro.
	 * @param pos A posição do registro no arquivo.
	 */
	public IndexRegister(int id, long pos) {
		this.id = id;
		this.pos = pos;
	}

	/**
	 * Retorna o código hash para este registro de índice.
	 *
	 * @return O código hash calculado com base no ID do registro.
	 */
	@Override
	public int hashCode() {
		return Integer.hashCode(id);
	}

	/**
	 * Compara este registro de índice com o objeto especificado para igualdade.
	 *
	 * @param obj O objeto a ser comparado para igualdade.
	 * @return true se o objeto especificado for igual a este registro de índice,
	 *         caso contrário, false.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		IndexRegister other = (IndexRegister) obj;
		return this.id == other.id;
	}

	/**
	 * Compara este registro de índice com outro baseado no ID.
	 *
	 * @param other O outro registro de índice a ser comparado.
	 * @return Um valor negativo, zero ou positivo conforme este registro seja
	 *         menor, igual ou maior
	 *         que o outro.
	 */
	public int compareTo(IndexRegister other) {
		return Integer.compare(id, other.getId());
	}

	/**
	 * Lê os dados do registro de um arquivo de acesso aleatório.
	 *
	 * @param in O arquivo de acesso aleatório de onde ler os dados.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	public void readExternal(RandomAccessFile in) throws IOException {
		id = in.readInt();
		pos = in.readLong();
	}

	/**
	 * Escreve os dados do registro em um arquivo de acesso aleatório.
	 *
	 * @param out O arquivo de acesso aleatório onde escrever os dados.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	public void writeExternal(RandomAccessFile out) throws IOException {
		out.writeInt(id);
		out.writeLong(pos);
	}

	/**
	 * Lê os dados do registro de um fluxo de entrada.
	 *
	 * @param in O fluxo de entrada de onde ler os dados.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	public void readExternal(ObjectInput in) throws IOException {
		id = in.readInt();
		pos = in.readLong();
	}

	/**
	 * Escreve os dados do registro em um fluxo de saída.
	 *
	 * @param out O fluxo de saída onde escrever os dados.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(id);
		out.writeLong(pos);
	}

	/**
	 * Converte o registro de índice em um array de bytes.
	 *
	 * @return Um array de bytes representando o registro de índice.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	public byte[] toByteArray() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(out);
		dataOut.writeInt(id);
		dataOut.writeLong(pos);
		return out.toByteArray();
	}

	/**
	 * Reconstrói o registro de índice a partir de um array de bytes.
	 *
	 * @param in O array de bytes de onde ler os dados.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	public void fromByteArray(byte[] in) throws IOException {
		ByteArrayInputStream byteIn = new ByteArrayInputStream(in);
		DataInputStream dataIn = new DataInputStream(byteIn);
		id = dataIn.readInt();
		pos = dataIn.readLong();
	}

	/**
	 * Retorna o tamanho do registro de índice em bytes.
	 *
	 * @return O tamanho do registro de índice.
	 */
	public short size() {
		return SIZE;
	}

	/**
	 * Obtém o ID do registro.
	 *
	 * @return O ID do registro.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Define o ID do registro.
	 *
	 * @param id O novo ID do registro.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * Obtém a posição do registro no arquivo.
	 *
	 * @return A posição do registro.
	 */
	public long getPos() {
		return pos;
	}

	/**
	 * Define a posição do registro no arquivo.
	 *
	 * @param pos A nova posição do registro.
	 */
	public void setPos(long pos) {
		this.pos = pos;
	}
}
