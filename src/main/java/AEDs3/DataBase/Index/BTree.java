package AEDs3.DataBase.Index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Classe que representa uma Árvore B, implementando a interface Index.
 * Esta classe fornece métodos para gerenciar uma estrutura de Árvore B,
 * que é uma estrutura de dados auto-balanceada que mantém dados ordenados
 * e permite operações eficientes de inserção, exclusão e busca.
 * <p>
 * A Árvore B é armazenada em um arquivo, permitindo o armazenamento persistente
 * da estrutura da árvore. Cada nó na Árvore B é representado por uma Página,
 * que contém elementos e páginas filhas. Páginas só são mantidas na memória
 * enquanto necessário.
 */
public class BTree implements Index {
	/**
	 * Representa uma página na Árvore B, que contém elementos e filhos.
	 */
	private class Page {
		/**
		 * Número de elementos na página.
		 */
		private int numElements;

		/**
		 * Array de registros de índice armazenados na página.
		 */
		private IndexRegister[] elements;

		/**
		 * Array de filhos da página.
		 */
		private Page[] children;

		/**
		 * Indica se a página está carregada na memória.
		 */
		private boolean loaded;

		/**
		 * Posição da página no arquivo.
		 */
		private long pos;

		/**
		 * Construtor para criar uma nova página com um número máximo de elementos.
		 *
		 * @param maxElements O número máximo de elementos que a página pode conter.
		 * @throws IOException Se ocorrer um erro de I/O ao salvar a página.
		 */
		public Page(int maxElements) throws IOException {
			this.pos = file.length();
			this.loaded = true;
			this.numElements = 0;
			this.elements = new IndexRegister[maxElements];
			this.children = new Page[maxElements + 1];
			save();
		}

		/**
		 * Construtor para criar uma página a partir de uma posição específica no
		 * arquivo.
		 *
		 * @param pos A posição da página no arquivo.
		 */
		public Page(long pos) {
			this.pos = pos;
			this.loaded = false;
		}

		/**
		 * Carrega a página do arquivo para a memória, se ainda não estiver carregada.
		 *
		 * @throws IOException Se ocorrer um erro de I/O ao carregar a página.
		 */
		private void load() throws IOException {
			if (this.loaded)
				return;

			file.seek(this.pos);
			this.numElements = file.readInt();
			this.elements = new IndexRegister[pageCapacity];
			this.children = new Page[pageCapacity + 1];

			for (int i = 0; i < pageCapacity; i++) {
				long childPos = file.readLong();
				if (childPos >= 0)
					this.children[i] = new Page(childPos);
				else
					this.children[i] = null; // Placeholder, não precisamos de nova página.

				if (i < this.numElements) {
					this.elements[i] = new IndexRegister();
					this.elements[i].readExternal(file);
				} else {
					this.elements[i] = null; // Placeholder para elementos não inicializados.
				}
			}

			// Lê a posição do último filho.
			long lastChildPos = file.readLong();
			if (lastChildPos >= 0)
				this.children[pageCapacity] = new Page(lastChildPos);
			else
				this.children[pageCapacity] = null;

			this.loaded = true;
		}

		/**
		 * Salva o estado atual da Árvore B no arquivo.
		 *
		 * @throws IOException Se ocorrer um erro de I/O ao escrever no arquivo.
		 */
		private void save() throws IOException {
			file.seek(this.pos);
			file.writeInt(this.numElements);

			long[] childrenPositions = new long[pageCapacity + 1];

			// Primeiro, escrevemos todos os filhos e todas as posições.
			for (int i = 0; i < pageCapacity; i++) {
				saveChildren(childrenPositions, i);

				if (i < this.numElements && this.elements[i] != null)
					this.elements[i].writeExternal(file);
				else
					// Placeholder para elementos não inicializados.
					new IndexRegister(-1, -1).writeExternal(file);
			}

			// Salva a posição do último filho.
			saveChildren(childrenPositions, pageCapacity);

			// Apenas após salvar a página atual, salvamos recusivamente os filhos.
			// Isso é necessário para impedir posições inválidas no RandomAccessFile.
			for (int i = 0; i <= pageCapacity; i++)
				if (this.children[i] != null && this.children[i].isLoaded())
					this.children[i].save();
		}

		private void saveChildren(long[] childrenPositions, int i) throws IOException {
			if (this.children[i] != null) {
				if (this.children[i].getPos() < 0)
					// Aloca uma nova posição para o filho no fim do arquivo.
					this.children[i].setPos(file.length());
				childrenPositions[i] = this.children[i].getPos();
			} else {
				childrenPositions[i] = -1; // Placeholder para filhos não inicializados.
			}
			file.writeLong(childrenPositions[i]);
		}

		/**
		 * Descarrega a página da memória, liberando os recursos associados.
		 */
		public void unload() {
			this.elements = null;
			this.children = null;
			this.loaded = false;
		}

		/**
		 * Obtém o número de elementos na página.
		 *
		 * @return O número de elementos na página.
		 * @throws IOException Se ocorrer um erro de I/O ao carregar a página.
		 */
		public int getNumElements() throws IOException {
			load();
			return numElements;
		}

		/**
		 * Define o número de elementos na página.
		 *
		 * @param numElements O novo número de elementos na página.
		 * @throws IOException Se ocorrer um erro de I/O ao carregar a página.
		 */
		public void setNumElements(int numElements) throws IOException {
			load();
			this.numElements = numElements;
		}

		/**
		 * Obtém os elementos da página.
		 *
		 * @return Um array de registros de índice na página.
		 * @throws IOException Se ocorrer um erro de I/O ao carregar a página.
		 */
		public IndexRegister[] getElements() throws IOException {
			load();
			return elements;
		}

		/**
		 * Obtém os filhos da página.
		 *
		 * @return Um array de páginas filhas.
		 * @throws IOException Se ocorrer um erro de I/O ao carregar a página.
		 */
		public Page[] getChildren() throws IOException {
			load();
			return children;
		}

		/**
		 * Verifica se a página está carregada na memória.
		 *
		 * @return {@code true} se a página estiver carregada, caso contrário
		 *         {@code false}.
		 */
		public boolean isLoaded() {
			return loaded;
		}

		/**
		 * Obtém a posição da página no arquivo.
		 *
		 * @return A posição da página no arquivo.
		 */
		public long getPos() {
			return pos;
		}

		/**
		 * Define a posição da página no arquivo.
		 *
		 * @param pos A nova posição da página no arquivo.
		 */
		public void setPos(long pos) {
			this.pos = pos;
		}
	}

	private Page root;
	private final int halfPageCapacity;
	private final int pageCapacity;
	private String filePath;
	private RandomAccessFile file;

	/**
	 * Construtor que cria uma Árvore B a partir de um arquivo existente.
	 *
	 * @param filePath O caminho para o arquivo onde a Árvore B está armazenada.
	 * @throws IOException Se ocorrer um erro de I/O ao acessar o arquivo.
	 */
	public BTree(String filePath) throws IOException {
		if (!Files.exists(Paths.get(filePath)))
			throw new FileNotFoundException("Arquivo de Árvore B inexistente.");

		this.filePath = filePath;
		this.file = new RandomAccessFile(filePath, "rw");
		file.seek(0);
		long rootPos = file.readLong();
		this.halfPageCapacity = file.readInt();
		this.pageCapacity = 2 * this.halfPageCapacity;

		if (rootPos >= 0) {
			root = new Page(rootPos);
		} else {
			root = null;
		}
	}

	/**
	 * Construtor que cria uma nova Árvore B com uma ordem especificada e caminho de
	 * arquivo.
	 *
	 * @param m        A ordem da Árvore B, que determina o número máximo de filhos
	 *                 que cada nó pode ter.
	 * @param filePath O caminho para o arquivo onde a Árvore B será armazenada.
	 * @throws IOException Se ocorrer um erro de I/O ao criar o arquivo.
	 */
	public BTree(int m, String filePath) throws IOException {
		this.filePath = filePath;
		this.file = new RandomAccessFile(filePath, "rw");
		this.root = null;
		this.halfPageCapacity = m;
		this.pageCapacity = 2 * m;

		file.seek(0);
		file.writeLong(-1);
		file.writeInt(halfPageCapacity);
	}

	/**
	 * Destrói a Árvore B, fechando o arquivo e deletando-o do sistema de arquivos.
	 *
	 * @throws IOException Se ocorrer um erro de I/O ao fechar ou deletar o arquivo.
	 */
	public void destruct() throws IOException {
		file.close();
		Files.delete(Paths.get(this.filePath));
		this.root = null;
		this.file = null;
		this.filePath = null;
	}

	/**
	 * Salva todas as páginas carregadas, partindo da raiz e seguindo
	 * recursivamente.
	 */
	private void save() throws IOException {
		if (root != null) {
			file.seek(0);
			file.writeLong(root.getPos());
			root.save();
		}
	}

	/**
	 * Descarrega a Árvore B da memória, liberando os recursos associados.
	 */
	private void unload() {
		root.unload();
	}

	/**
	 * Busca um registro na Árvore B pelo identificador fornecido.
	 *
	 * @param id O identificador do registro a ser buscado.
	 * @return A posição do registro no arquivo, ou -1 se o registro não for
	 *         encontrado.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação de busca.
	 */
	public long search(int id) throws IOException {
		IndexRegister res = this.search(new IndexRegister(id, -1));
		this.save();
		this.unload();
		return (res != null) ? res.getPos() : -1;
	}

	private IndexRegister search(IndexRegister reg) throws IOException {
		return this.search(reg, this.root);
	}

	private IndexRegister search(IndexRegister reg, Page page) throws IOException {
		if (page == null)
			return null; // Registro não encontrado.

		// Procura o índice do registro na página.
		int i = 0;
		while ((i < page.getNumElements() - 1) && (reg.compareTo(page.getElements()[i]) > 0))
			i++;
		if (reg.compareTo(page.getElements()[i]) == 0)
			return page.getElements()[i];
		else if (reg.compareTo(page.getElements()[i]) < 0)
			return search(reg, page.getChildren()[i]);
		else
			return search(reg, page.getChildren()[i + 1]);
	}

	/**
	 * Insere um novo registro na Árvore B com o identificador e posição fornecidos.
	 *
	 * @param id  O identificador do registro a ser inserido.
	 * @param pos A posição do registro no arquivo.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação de inserção.
	 */
	public void insert(int id, long pos) throws IOException {
		this.insere(new IndexRegister(id, pos));
		this.save();
		this.unload();
	}

	/**
	 * Insere um novo registro na árvore B.
	 *
	 * @param reg O registro a ser inserido.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação de inserção.
	 */
	private void insere(IndexRegister reg) throws IOException {
		IndexRegister[] regRetorno = new IndexRegister[1]; // Array para armazenar o registro retornado.
		boolean[] cresceu = new boolean[1]; // Array para indicar se a árvore cresceu.
		Page apRetorno = this.insere(reg, this.root, regRetorno, cresceu); // Insere o registro na árvore.
		if (cresceu[0]) { // Se a árvore cresceu, cria uma nova raiz.
			Page apTemp = new Page(this.pageCapacity);
			apTemp.getElements()[0] = regRetorno[0];
			apTemp.getChildren()[0] = this.root;
			apTemp.getChildren()[1] = apRetorno;
			this.root = apTemp;
			this.root.setNumElements(this.root.getNumElements() + 1);
		} else
			this.root = apRetorno; // Atualiza a raiz se não houve crescimento.
	}

	/**
	 * Insere um registro em uma página específica da árvore B.
	 *
	 * @param reg    O registro a ser inserido.
	 * @param page   A página atual onde a inserção é realizada.
	 * @param regRet Array para armazenar o registro retornado.
	 * @param grown  Array para indicar se a árvore cresceu.
	 * @return A página resultante após a inserção.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação de inserção.
	 */
	private Page insere(IndexRegister reg, Page page, IndexRegister[] regRet, boolean[] grown)
			throws IOException {
		Page pageRet = null;
		// Se a página for nula, o registro não foi encontrado.
		if (page == null) {
			grown[0] = true;
			regRet[0] = reg;
		} else {
			int i = 0;
			// Procura a posição correta para inserção.
			while ((i < page.getNumElements() - 1) && (reg.compareTo(page.getElements()[i]) > 0))
				i++;
			if (reg.compareTo(page.getElements()[i]) == 0) {
				System.out.println("Erro : Registro ja existente");
				grown[0] = false;
			} else {
				if (reg.compareTo(page.getElements()[i]) > 0)
					i++;
				pageRet = insere(reg, page.getChildren()[i], regRet, grown);
				if (grown[0]) {
					if (page.getNumElements() < this.pageCapacity) { // Página tem espaço.
						this.pageInsert(page, regRet[0], pageRet);
						grown[0] = false;
						pageRet = page;
					} else { // Overflow: Página tem que ser dividida.
						Page pageTemp = new Page(this.pageCapacity);
						pageTemp.getChildren()[0] = null;
						if (i <= this.halfPageCapacity) {
							this.pageInsert(pageTemp, page.getElements()[this.pageCapacity - 1],
									page.getChildren()[this.pageCapacity]);
							page.setNumElements(page.getNumElements() - 1);
							this.pageInsert(page, regRet[0], pageRet);
						} else
							this.pageInsert(pageTemp, regRet[0], pageRet);
						for (int j = this.halfPageCapacity + 1; j < this.pageCapacity; j++) {
							this.pageInsert(
									pageTemp, page.getElements()[j], page.getChildren()[j + 1]);
							page.getChildren()[j + 1] = null; // Transfere a posse da memória.
						}
						page.setNumElements(this.halfPageCapacity);
						pageTemp.getChildren()[0] = page.getChildren()[this.halfPageCapacity + 1];
						regRet[0] = page.getElements()[this.halfPageCapacity];
						pageRet = pageTemp;
					}
				}
			}
		}
		return (grown[0] ? pageRet : page);
	}

	/**
	 * Insere um registro em uma página específica, ajustando os elementos e filhos.
	 *
	 * @param page      A página onde o registro será inserido.
	 * @param reg       O registro a ser inserido.
	 * @param pageRight A página à direita do registro inserido.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação de inserção.
	 */
	private void pageInsert(Page page, IndexRegister reg, Page pageRight) throws IOException {
		int k = page.getNumElements() - 1;
		// Move os elementos e filhos para abrir espaço para o novo registro.
		while ((k >= 0) && (reg.compareTo(page.getElements()[k]) < 0)) {
			page.getElements()[k + 1] = page.getElements()[k];
			page.getChildren()[k + 2] = page.getChildren()[k + 1];
			k--;
		}
		// Insere o novo registro e ajusta o filho à direita.
		page.getElements()[k + 1] = reg;
		page.getChildren()[k + 2] = pageRight;
		page.setNumElements(page.getNumElements() + 1);
	}

	/**
	 * Remove um registro da Árvore B com o identificador fornecido.
	 *
	 * @param id O identificador do registro a ser removido.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação de remoção.
	 */
	public void delete(int id) throws IOException {
		delete(new IndexRegister(id, -1));

		this.save();
		this.unload();
	}

	/**
	 * Método auxiliar para remover um registro da árvore B.
	 *
	 * @param reg O registro a ser removido.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação de remoção.
	 */
	private void delete(IndexRegister reg) throws IOException {
		boolean[] diminuiu = new boolean[1];
		this.delete(reg, this.root, diminuiu);
		if (diminuiu[0] && (this.root.getNumElements() == 0)) { // Árvore diminui na altura.
			this.root = this.root.getChildren()[0];
		}
	}

	/**
	 * Método recursivo para remover um registro de uma página específica.
	 *
	 * @param reg    O registro a ser removido.
	 * @param page   A página atual onde a busca e remoção são realizadas.
	 * @param shrunk Array booleano que indica se a página diminuiu de tamanho.
	 * @return A página resultante após a remoção.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação de remoção.
	 */
	private Page delete(IndexRegister reg, Page page, boolean[] shrunk) throws IOException {
		if (page == null) {
			System.out.println("Erro : Registro nao encontrado");
			shrunk[0] = false;
		} else {
			int i = 0;
			while ((i < page.getNumElements() - 1) && (reg.compareTo(page.getElements()[i]) > 0))
				i++;
			if (reg.compareTo(page.getElements()[i]) == 0) { // Achou.
				// Se a página for uma folha, remove o registro diretamente.
				if (page.getChildren()[i] == null) {
					page.setNumElements(page.getNumElements() - 1);
					shrunk[0] = page.getNumElements() < this.halfPageCapacity;
					for (int j = i; j < page.getNumElements(); j++) {
						page.getElements()[j] = page.getElements()[j + 1];
						page.getChildren()[j] = page.getChildren()[j + 1];
					}
					page.getChildren()[page.getNumElements()] = page.getChildren()[page.getNumElements() + 1];
					page.getChildren()[page.getNumElements() + 1] = null; // Transfere a posse da memória.
				} else { // Página não é folha: trocar com antecessor.
					// Se a página não for uma folha, troca com o antecessor.
					shrunk[0] = antecessor(page, i, page.getChildren()[i]);
					if (shrunk[0])
						shrunk[0] = reconstruct(page.getChildren()[i], page, i);
				}
			} else { // Não achou.
				if (reg.compareTo(page.getElements()[i]) > 0)
					i++;
				page.getChildren()[i] = delete(reg, page.getChildren()[i], shrunk);
				if (shrunk[0])
					shrunk[0] = reconstruct(page.getChildren()[i], page, i);
			}
		}
		return page;
	}

	/**
	 * Encontra e substitui o registro pelo seu antecessor.
	 *
	 * @param page       A página atual.
	 * @param ind        O índice do registro na página.
	 * @param parentPage A página pai.
	 * @return {@code true} se a página diminuiu de tamanho, caso contrário
	 *         {@code false}.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	private boolean antecessor(Page page, int ind, Page parentPage) throws IOException {
		boolean shrunk;
		// Se o último filho da página pai não for nulo, continua a busca pelo
		// antecessor.
		if (parentPage.getChildren()[parentPage.getNumElements()] != null) {
			shrunk = antecessor(page, ind, parentPage.getChildren()[parentPage.getNumElements()]);
			if (shrunk)
				shrunk = reconstruct(parentPage.getChildren()[parentPage.getNumElements()],
						parentPage, parentPage.getNumElements());
		} else {
			// Substitui o registro pelo antecessor encontrado.
			parentPage.setNumElements(parentPage.getNumElements() - 1);
			page.getElements()[ind] = parentPage.getElements()[parentPage.getNumElements()];
			shrunk = parentPage.getNumElements() < this.halfPageCapacity;
		}
		return shrunk;
	}

	/**
	 * Reconstrói a árvore após a remoção de um registro, se necessário.
	 *
	 * @param page       A página atual.
	 * @param parentPage A página pai.
	 * @param parentIdx  O índice da página atual na página pai.
	 * @return {@code true} se a página diminuiu de tamanho, caso contrário
	 *         {@code false}.
	 * @throws IOException Se ocorrer um erro de I/O durante a operação.
	 */
	private boolean reconstruct(Page page, Page parentPage, int parentIdx) throws IOException {
		boolean shrunk;
		// Se a página atual tiver um irmão à direita, tenta redistribuir ou fundir.
		if (parentIdx < parentPage.getNumElements()) {
			Page aux = parentPage.getChildren()[parentIdx + 1];
			int dispAux = (aux.getNumElements() - this.halfPageCapacity + 1) / 2;
			page.getElements()[page.getNumElements()] = parentPage.getElements()[parentIdx];
			page.setNumElements(page.getNumElements() + 1);
			page.getChildren()[page.getNumElements()] = aux.getChildren()[0];
			aux.getChildren()[0] = null; // Transfere a posse da memória.
			// Se houver espaço extra, transfere elementos do irmão para a página atual.
			if (dispAux > 0) {
				for (int j = 0; j < dispAux - 1; j++) {
					this.pageInsert(page, aux.getElements()[j], aux.getChildren()[j + 1]);
					aux.getChildren()[j + 1] = null; // Transfere a posse da memória.
				}
				parentPage.getElements()[parentIdx] = aux.getElements()[dispAux - 1];
				aux.setNumElements(aux.getNumElements() - dispAux);
				for (int j = 0; j < aux.getNumElements(); j++)
					aux.getElements()[j] = aux.getElements()[j + dispAux];
				for (int j = 0; j <= aux.getNumElements(); j++)
					aux.getChildren()[j] = aux.getChildren()[j + dispAux];
				aux.getChildren()[aux.getNumElements() + dispAux] = null; // Transfere a posse da memória.
				shrunk = false;
			} else {
				// Caso contrário, realiza a fusão das páginas.
				for (int j = 0; j < this.halfPageCapacity; j++) {
					this.pageInsert(page, aux.getElements()[j], aux.getChildren()[j + 1]);
					aux.getChildren()[j + 1] = null; // Transfere a posse da memória.
				}
				parentPage.getChildren()[parentIdx + 1] = null; // Libera o filho.
				for (int j = parentIdx; j < parentPage.getNumElements() - 1; j++) {
					parentPage.getElements()[j] = parentPage.getElements()[j + 1];
					parentPage.getChildren()[j + 1] = parentPage.getChildren()[j + 2];
				}
				parentPage.getChildren()[parentPage.getNumElements()] = null; // Transfere a posse da memória.
				parentPage.setNumElements(parentPage.getNumElements() - 1);
				shrunk = parentPage.getNumElements() < this.halfPageCapacity;
			}
		} else {
			// Se a página atual tiver um irmão à esquerda, tenta redistribuir ou fundir.
			Page aux = parentPage.getChildren()[parentIdx - 1];
			int dispAux = (aux.getNumElements() - this.halfPageCapacity + 1) / 2;
			for (int j = page.getNumElements() - 1; j >= 0; j--)
				page.getElements()[j + 1] = page.getElements()[j];
			page.getElements()[0] = parentPage.getElements()[parentIdx - 1];
			for (int j = page.getNumElements(); j >= 0; j--)
				page.getChildren()[j + 1] = page.getChildren()[j];
			page.setNumElements(page.getNumElements() + 1);
			if (dispAux > 0) { // Existe folga: transfere de aux para page.
				for (int j = 0; j < dispAux - 1; j++) {
					this.pageInsert(page, aux.getElements()[aux.getNumElements() - j - 1],
							aux.getChildren()[aux.getNumElements() - j]);
					aux.getChildren()[aux.getNumElements() - j] = null; // Transfere a posse da memória.
				}
				page.getChildren()[0] = aux.getChildren()[aux.getNumElements() - dispAux + 1];
				aux.getChildren()[aux.getNumElements() - dispAux + 1] = null; // Transfere a posse da memória.
				parentPage.getElements()[parentIdx - 1] = aux.getElements()[aux.getNumElements() - dispAux];
				aux.setNumElements(aux.getNumElements() - dispAux);
				shrunk = false;
			} else {
				// Caso contrário, realiza a fusão das páginas.
				for (int j = 0; j < this.halfPageCapacity; j++) {
					this.pageInsert(aux, page.getElements()[j], page.getChildren()[j + 1]);
					page.getChildren()[j + 1] = null; // Transfere a posse da memória.
				}
				parentPage.getChildren()[parentPage.getNumElements()] = null; // Transfere a posse da memória.
				parentPage.setNumElements(parentPage.getNumElements() - 1);
				shrunk = parentPage.getNumElements() < this.halfPageCapacity;
			}
		}
		return shrunk;
	}

	/**
	 * Obtém a capacidade de meia página da Árvore B. Este é o mesmo valor passado
	 * ao construtor na criação do índice.
	 *
	 * @return A capacidade de meia página, que é a metade do número máximo de
	 *         elementos
	 *         que uma página pode conter.
	 */
	public int getHalfPageCapacity() {
		return halfPageCapacity;
	}

	public String[] listFilePaths() {
		return new String[] {filePath};
	}
}
