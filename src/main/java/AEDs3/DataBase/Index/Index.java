package AEDs3.DataBase.Index;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Interface para definir operações básicas de um índice.
 */
public interface Index {
	/**
	 * Procura um registro pelo ID.
	 *
	 * @param id O ID do registro a ser procurado.
	 * @return A posição do registro no arquivo.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	long search(int id) throws IOException;

	/**
	 * Insere um novo registro no índice.
	 *
	 * @param id  O ID do registro a ser inserido.
	 * @param pos A posição do registro no arquivo.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	void insert(int id, long pos) throws IOException;

	/**
	 * Remove um registro do índice pelo ID.
	 *
	 * @param id O ID do registro a ser removido.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	void delete(int id) throws IOException;

	/**
	 * Destrói o índice, liberando recursos associados.
	 *
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	void destruct() throws IOException;

	/**
	 * Lista os caminhos relativos de todos os arquivos associados ao índice atual.
	 *
	 * @return Um array de strings contendo os caminhos relativos de todos os
	 *         arquivos utilizados por este índice.
	 */
	String[] listFilePaths();
}
