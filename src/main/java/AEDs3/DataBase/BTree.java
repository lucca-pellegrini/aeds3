package AEDs3.DataBase;

public class BTree implements Index {
	private static class Page {
		int numElements;
		IndexRegister elements[];
		Page children[];

		public Page(int maxElements) {
			this.numElements = 0;
			this.elements = new IndexRegister[maxElements];
			this.children = new Page[maxElements + 1];
		}
	}

	private Page root;
	private int halfPageCapacity, pageCapacity;

	public BTree(int m) {
		this.root = null;
		this.halfPageCapacity = m;
		this.pageCapacity = 2 * m;
	}

	public long search(int id) {
		IndexRegister res = this.search(new IndexRegister(id, -1));
		return (res != null) ? res.getPos() : -1;
	}

	private IndexRegister search(IndexRegister reg) {
		return this.search(reg, this.root);
	}

	private IndexRegister search(IndexRegister reg, Page page) {
		if (page == null)
			return null; // Registro não encontrado

		int i = 0;
		while ((i < page.numElements - 1) && (reg.compareTo(page.elements[i]) > 0))
			i++;
		if (reg.compareTo(page.elements[i]) == 0)
			return page.elements[i];
		else if (reg.compareTo(page.elements[i]) < 0)
			return search(reg, page.children[i]);
		else
			return search(reg, page.children[i + 1]);
	}

	public void insert(int id, long pos) {
		this.insere(new IndexRegister(id, pos));
	}

	private void insere(IndexRegister reg) {
		IndexRegister regRetorno[] = new IndexRegister[1];
		boolean cresceu[] = new boolean[1];
		Page apRetorno = this.insere(reg, this.root, regRetorno, cresceu);
		if (cresceu[0]) {
			Page apTemp = new Page(this.pageCapacity);
			apTemp.elements[0] = regRetorno[0];
			apTemp.children[0] = this.root;
			apTemp.children[1] = apRetorno;
			this.root = apTemp;
			this.root.numElements++;
		} else
			this.root = apRetorno;
	}

	private Page insere(IndexRegister reg, Page page, IndexRegister[] regRet, boolean[] grown) {
		Page pageRet = null;
		if (page == null) {
			grown[0] = true;
			regRet[0] = reg;
		} else {
			int i = 0;
			while ((i < page.numElements - 1) && (reg.compareTo(page.elements[i]) > 0))
				i++;
			if (reg.compareTo(page.elements[i]) == 0) {
				System.out.println("Erro : Registro ja existente");
				grown[0] = false;
			} else {
				if (reg.compareTo(page.elements[i]) > 0)
					i++;
				pageRet = insere(reg, page.children[i], regRet, grown);
				if (grown[0])
					if (page.numElements < this.pageCapacity) { // Página tem espaço
						this.pageInsert(page, regRet[0], pageRet);
						grown[0] = false;
						pageRet = page;
					} else { // Overflow: Página tem que ser dividida
						Page pageTemp = new Page(this.pageCapacity);
						pageTemp.children[0] = null;
						if (i <= this.halfPageCapacity) {
							this.pageInsert(pageTemp, page.elements[this.pageCapacity - 1],
									page.children[this.pageCapacity]);
							page.numElements--;
							this.pageInsert(page, regRet[0], pageRet);
						} else
							this.pageInsert(pageTemp, regRet[0], pageRet);
						for (int j = this.halfPageCapacity + 1; j < this.pageCapacity; j++) {
							this.pageInsert(pageTemp, page.elements[j], page.children[j + 1]);
							page.children[j + 1] = null; // transfere a posse da memória
						}
						page.numElements = this.halfPageCapacity;
						pageTemp.children[0] = page.children[this.halfPageCapacity + 1];
						regRet[0] = page.elements[this.halfPageCapacity];
						pageRet = pageTemp;
					}
			}
		}
		return (grown[0] ? pageRet : page);
	}

	private void pageInsert(Page page, IndexRegister reg, Page pageRight) {
		int k = page.numElements - 1;
		while ((k >= 0) && (reg.compareTo(page.elements[k]) < 0)) {
			page.elements[k + 1] = page.elements[k];
			page.children[k + 2] = page.children[k + 1];
			k--;
		}
		page.elements[k + 1] = reg;
		page.children[k + 2] = pageRight;
		page.numElements++;
	}

	public void delete(int id) {
		delete(new IndexRegister(id, -1));
	}

	private void delete(IndexRegister reg) {
		boolean diminuiu[] = new boolean[1];
		this.root = this.delete(reg, this.root, diminuiu);
		if (diminuiu[0] && (this.root.numElements == 0)) { // Árvore diminui na altura
			this.root = this.root.children[0];
		}
	}

	private Page delete(IndexRegister reg, Page page, boolean[] shrunk) {
		if (page == null) {
			System.out.println("Erro : Registro nao encontrado");
			shrunk[0] = false;
		} else {
			int i = 0;
			while ((i < page.numElements - 1) && (reg.compareTo(page.elements[i]) > 0))
				i++;
			if (reg.compareTo(page.elements[i]) == 0) { // achou
				if (page.children[i] == null) { // Página folha
					page.numElements--;
					shrunk[0] = page.numElements < this.halfPageCapacity;
					for (int j = i; j < page.numElements; j++) {
						page.elements[j] = page.elements[j + 1];
						page.children[j] = page.children[j + 1];
					}
					page.children[page.numElements] = page.children[page.numElements + 1];
					page.children[page.numElements + 1] = null; // transfere a posse da memória
				} else { // Página não é folha: trocar com antecessor
					shrunk[0] = antecessor(page, i, page.children[i]);
					if (shrunk[0])
						shrunk[0] = reconstruct(page.children[i], page, i);
				}
			} else { // não achou
				if (reg.compareTo(page.elements[i]) > 0)
					i++;
				page.children[i] = delete(reg, page.children[i], shrunk);
				if (shrunk[0])
					shrunk[0] = reconstruct(page.children[i], page, i);
			}
		}
		return page;
	}

	private boolean antecessor(Page page, int ind, Page parentPage) {
		boolean shrunk = true;
		if (parentPage.children[parentPage.numElements] != null) {
			shrunk = antecessor(page, ind, parentPage.children[parentPage.numElements]);
			if (shrunk)
				shrunk = reconstruct(parentPage.children[parentPage.numElements], parentPage, parentPage.numElements);
		} else {
			page.elements[ind] = parentPage.elements[--parentPage.numElements];
			shrunk = parentPage.numElements < this.halfPageCapacity;
		}
		return shrunk;
	}

	private boolean reconstruct(Page page, Page parentPage, int parentIdx) {
		boolean shrunk = true;
		if (parentIdx < parentPage.numElements) { // aux = Página à direita de page
			Page aux = parentPage.children[parentIdx + 1];
			int dispAux = (aux.numElements - this.halfPageCapacity + 1) / 2;
			page.elements[page.numElements++] = parentPage.elements[parentIdx];
			page.children[page.numElements] = aux.children[0];
			aux.children[0] = null; // transfere a posse da memória
			if (dispAux > 0) { // Existe folga: transfere de aux para page
				for (int j = 0; j < dispAux - 1; j++) {
					this.pageInsert(page, aux.elements[j], aux.children[j + 1]);
					aux.children[j + 1] = null; // transfere a posse da memória
				}
				parentPage.elements[parentIdx] = aux.elements[dispAux - 1];
				aux.numElements = aux.numElements - dispAux;
				for (int j = 0; j < aux.numElements; j++)
					aux.elements[j] = aux.elements[j + dispAux];
				for (int j = 0; j <= aux.numElements; j++)
					aux.children[j] = aux.children[j + dispAux];
				aux.children[aux.numElements + dispAux] = null; // transfere a posse da memória
				shrunk = false;
			} else { // Fusão: intercala aux em page e libera aux
				for (int j = 0; j < this.halfPageCapacity; j++) {
					this.pageInsert(page, aux.elements[j], aux.children[j + 1]);
					aux.children[j + 1] = null; // transfere a posse da memória
				}
				aux = parentPage.children[parentIdx + 1] = null; // libera aux
				for (int j = parentIdx; j < parentPage.numElements - 1; j++) {
					parentPage.elements[j] = parentPage.elements[j + 1];
					parentPage.children[j + 1] = parentPage.children[j + 2];
				}
				parentPage.children[parentPage.numElements--] = null; // transfere a posse da memória
				shrunk = parentPage.numElements < this.halfPageCapacity;
			}
		} else { // aux = Página à esquerda de page
			Page aux = parentPage.children[parentIdx - 1];
			int dispAux = (aux.numElements - this.halfPageCapacity + 1) / 2;
			for (int j = page.numElements - 1; j >= 0; j--)
				page.elements[j + 1] = page.elements[j];
			page.elements[0] = parentPage.elements[parentIdx - 1];
			for (int j = page.numElements; j >= 0; j--)
				page.children[j + 1] = page.children[j];
			page.numElements++;
			if (dispAux > 0) { // Existe folga: transfere de aux para page
				for (int j = 0; j < dispAux - 1; j++) {
					this.pageInsert(page, aux.elements[aux.numElements - j - 1],
							aux.children[aux.numElements - j]);
					aux.children[aux.numElements - j] = null; // transfere a posse da memória
				}
				page.children[0] = aux.children[aux.numElements - dispAux + 1];
				aux.children[aux.numElements - dispAux + 1] = null; // transfere a posse da memória
				parentPage.elements[parentIdx - 1] = aux.elements[aux.numElements - dispAux];
				aux.numElements = aux.numElements - dispAux;
				shrunk = false;
			} else { // Fusão: intercala page em aux e libera page
				for (int j = 0; j < this.halfPageCapacity; j++) {
					this.pageInsert(aux, page.elements[j], page.children[j + 1]);
					page.children[j + 1] = null; // transfere a posse da memória
				}
				page = null; // libera page
				parentPage.children[parentPage.numElements--] = null; // transfere a posse da memória
				shrunk = parentPage.numElements < this.halfPageCapacity;
			}
		}
		return shrunk;
	}
}
