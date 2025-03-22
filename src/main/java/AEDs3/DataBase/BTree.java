package AEDs3.DataBase;

import java.io.IOException;
import java.io.RandomAccessFile;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

public class BTree implements Index {
	private class Page {
		private int numElements;
		private IndexRegister elements[];
		private Page children[];

		// Atributos no arquivo.
		private boolean loaded;
		// private long pos;

		public Page(int maxElements) throws IOException {
			// this.pos = file.length();
			this.loaded = true;
			this.numElements = 0;
			this.elements = new IndexRegister[maxElements];
			this.children = new Page[maxElements + 1];
			// save();
		}

		// public Page(long pos) {
		// this.pos = pos;
		// this.loaded = false;
		// }

		private void load() throws IOException {
			if (this.isLoaded())
				return;

			// this.pos = (this.pos > 0) ? this.pos : file.length();
			//
			// file.seek(pos);
			// this.numElements = file.readInt();
			// this.elements = new IndexRegister[pageCapacity];
			// this.children = new Page[pageCapacity + 1];
			// for (int i = 0; i < numElements; ++i) {
			// this.children[i] = new Page((long) file.readLong());
			// int id = file.readInt();
			// long pos = file.readLong();
			// this.elements[i] = new IndexRegister(id, pos);
			// }
			// this.children[numElements] = new Page((long) file.readLong());
			this.setLoaded(true);
		}

		// private void save() throws IOException {
		// if (!isLoaded())
		// return;
		//
		// file.seek(this.pos);
		// file.writeInt(this.getNumElements());
		// for (int i = 0; i < pageCapacity; ++i) {
		// if (children[i] != null)
		// file.writeLong(children[i].getPos());
		// else
		// file.writeLong(-1);
		// if (elements[i] != null) {
		// file.writeInt(elements[i].getId());
		// file.writeLong(elements[i].getPos());
		// } else {
		// file.writeInt(-1);
		// file.writeLong(-1);
		// }
		// }
		//
		// // Add a null check for the last child
		// if (children[pageCapacity] != null) {
		// file.writeLong(children[pageCapacity].getPos());
		// } else {
		// file.writeLong(-1);
		// }
		//
		// for (int i = 0; i <= numElements; ++i)
		// if (children[i] != null && children[i].isLoaded())
		// children[i].save();
		// }
		//
		public int getNumElements() throws IOException {
			load();
			return numElements;
		}

		public void setNumElements(int numElements) throws IOException {
			load();
			this.numElements = numElements;
		}

		public IndexRegister[] getElements() throws IOException {
			load();
			return elements;
		}

		public void setElements(IndexRegister[] elements) throws IOException {
			load();
			this.elements = elements;
		}

		public Page[] getChildren() throws IOException {
			load();
			return children;
		}

		public void setChildren(Page[] children) throws IOException {
			load();
			this.children = children;
		}

		public boolean isLoaded() throws IOException {
			return loaded;
		}

		public void setLoaded(boolean loaded) throws IOException {
			this.loaded = loaded;
		}

		// public long getPos() throws IOException {
		// 	return pos;
		// }
		//
		// public void setPos(long pos) throws IOException {
		// 	this.pos = pos;
		// }
	}

	private Page root;
	private int halfPageCapacity, pageCapacity;
	private RandomAccessFile file;
	private int pageSize;

	// public BTree(String filePath) throws IOException {
	// file = new RandomAccessFile(filePath + ".Btree.idx", "rw");
	// file.seek(0);
	// long rootPos = file.readLong();
	// this.halfPageCapacity = file.readInt();
	// this.pageCapacity = 2 * this.halfPageCapacity;
	//
	// this.pageSize = (Integer.SIZE / 8) + pageCapacity * IndexRegister.SIZE
	// + (pageCapacity - 1) * (Long.SIZE / 8);
	//
	// root = new Page((long) rootPos);
	// }

	public BTree(int m, String filePath) throws IOException {
		file = new RandomAccessFile(filePath + ".Btree.idx", "rw");
		this.root = null;
		this.halfPageCapacity = m;
		this.pageCapacity = 2 * m;

		this.pageSize = (Integer.SIZE / 8) + pageCapacity * IndexRegister.SIZE
				+ (pageCapacity - 1) * (Long.SIZE / 8);

		file.seek(0);
		file.writeLong(-1);
		file.writeInt(pageCapacity);
	}

	public long search(int id) throws IOException {
		IndexRegister res = this.search(new IndexRegister(id, -1));
		return (res != null) ? res.getPos() : -1;
	}

	private IndexRegister search(IndexRegister reg) throws IOException {
		return this.search(reg, this.root);
	}

	private IndexRegister search(IndexRegister reg, Page page) throws IOException {
		if (page == null)
			return null; // Registro não encontrado

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

	public void insert(int id, long pos) throws IOException {
		this.insere(new IndexRegister(id, pos));

		// file.seek(0);
		// file.writeLong(root.pos);
		// this.root.save();
	}

	private void insere(IndexRegister reg) throws IOException {
		IndexRegister regRetorno[] = new IndexRegister[1];
		boolean cresceu[] = new boolean[1];
		Page apRetorno = this.insere(reg, this.root, regRetorno, cresceu);
		if (cresceu[0]) {
			Page apTemp = new Page(this.pageCapacity);
			apTemp.getElements()[0] = regRetorno[0];
			apTemp.getChildren()[0] = this.root;
			apTemp.getChildren()[1] = apRetorno;
			this.root = apTemp;
			this.root.setNumElements(this.root.getNumElements() + 1);
		} else
			this.root = apRetorno;
	}

	private Page insere(IndexRegister reg, Page page, IndexRegister[] regRet, boolean[] grown)
			throws IOException {
		Page pageRet = null;
		if (page == null) {
			grown[0] = true;
			regRet[0] = reg;
		} else {
			int i = 0;
			while ((i < page.getNumElements() - 1) && (reg.compareTo(page.getElements()[i]) > 0))
				i++;
			if (reg.compareTo(page.getElements()[i]) == 0) {
				System.out.println("Erro : Registro ja existente");
				grown[0] = false;
			} else {
				if (reg.compareTo(page.getElements()[i]) > 0)
					i++;
				pageRet = insere(reg, page.getChildren()[i], regRet, grown);
				if (grown[0])
					if (page.getNumElements() < this.pageCapacity) { // Página tem espaço
						this.pageInsert(page, regRet[0], pageRet);
						grown[0] = false;
						pageRet = page;
					} else { // Overflow: Página tem que ser dividida
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
							page.getChildren()[j + 1] = null; // transfere a posse da memória
						}
						page.setNumElements(this.halfPageCapacity);
						pageTemp.getChildren()[0] = page.getChildren()[this.halfPageCapacity + 1];
						regRet[0] = page.getElements()[this.halfPageCapacity];
						pageRet = pageTemp;
					}
			}
		}
		return (grown[0] ? pageRet : page);
	}

	private void pageInsert(Page page, IndexRegister reg, Page pageRight) throws IOException {
		int k = page.getNumElements() - 1;
		while ((k >= 0) && (reg.compareTo(page.getElements()[k]) < 0)) {
			page.getElements()[k + 1] = page.getElements()[k];
			page.getChildren()[k + 2] = page.getChildren()[k + 1];
			k--;
		}
		page.getElements()[k + 1] = reg;
		page.getChildren()[k + 2] = pageRight;
		page.setNumElements(page.getNumElements() + 1);
	}

	public void delete(int id) throws IOException {
		delete(new IndexRegister(id, -1));
	}

	private void delete(IndexRegister reg) throws IOException {
		boolean diminuiu[] = new boolean[1];
		this.root = this.delete(reg, this.root, diminuiu);
		if (diminuiu[0] && (this.root.getNumElements() == 0)) { // Árvore diminui na altura
			this.root = this.root.getChildren()[0];
		}

		// file.seek(0);
		// file.writeLong(root.pos);
		// this.root.save();
	}

	private Page delete(IndexRegister reg, Page page, boolean[] shrunk) throws IOException {
		if (page == null) {
			System.out.println("Erro : Registro nao encontrado");
			shrunk[0] = false;
		} else {
			int i = 0;
			while ((i < page.getNumElements() - 1) && (reg.compareTo(page.getElements()[i]) > 0))
				i++;
			if (reg.compareTo(page.getElements()[i]) == 0) { // achou
				if (page.getChildren()[i] == null) { // Página folha
					page.setNumElements(page.getNumElements() - 1);
					shrunk[0] = page.getNumElements() < this.halfPageCapacity;
					for (int j = i; j < page.getNumElements(); j++) {
						page.getElements()[j] = page.getElements()[j + 1];
						page.getChildren()[j] = page.getChildren()[j + 1];
					}
					page.getChildren()[page.getNumElements()] = page.getChildren()[page.getNumElements() + 1];
					page.getChildren()[page.getNumElements() + 1] = null; // transfere a posse da memória
				} else { // Página não é folha: trocar com antecessor
					shrunk[0] = antecessor(page, i, page.getChildren()[i]);
					if (shrunk[0])
						shrunk[0] = reconstruct(page.getChildren()[i], page, i);
				}
			} else { // não achou
				if (reg.compareTo(page.getElements()[i]) > 0)
					i++;
				page.getChildren()[i] = delete(reg, page.getChildren()[i], shrunk);
				if (shrunk[0])
					shrunk[0] = reconstruct(page.getChildren()[i], page, i);
			}
		}
		return page;
	}

	private boolean antecessor(Page page, int ind, Page parentPage) throws IOException {
		boolean shrunk = true;
		if (parentPage.getChildren()[parentPage.getNumElements()] != null) {
			shrunk = antecessor(page, ind, parentPage.getChildren()[parentPage.getNumElements()]);
			if (shrunk)
				shrunk = reconstruct(parentPage.getChildren()[parentPage.getNumElements()],
						parentPage, parentPage.getNumElements());
		} else {
			parentPage.setNumElements(parentPage.getNumElements() - 1);
			page.getElements()[ind] = parentPage.getElements()[parentPage.getNumElements()];
			shrunk = parentPage.getNumElements() < this.halfPageCapacity;
		}
		return shrunk;
	}

	private boolean reconstruct(Page page, Page parentPage, int parentIdx) throws IOException {
		boolean shrunk = true;
		if (parentIdx < parentPage.getNumElements()) { // aux = Página à direita de page
			Page aux = parentPage.getChildren()[parentIdx + 1];
			int dispAux = (aux.getNumElements() - this.halfPageCapacity + 1) / 2;
			page.getElements()[page.getNumElements()] = parentPage.getElements()[parentIdx];
			page.setNumElements(page.getNumElements() + 1);
			page.getChildren()[page.getNumElements()] = aux.getChildren()[0];
			aux.getChildren()[0] = null; // transfere a posse da memória
			if (dispAux > 0) { // Existe folga: transfere de aux para page
				for (int j = 0; j < dispAux - 1; j++) {
					this.pageInsert(page, aux.getElements()[j], aux.getChildren()[j + 1]);
					aux.getChildren()[j + 1] = null; // transfere a posse da memória
				}
				parentPage.getElements()[parentIdx] = aux.getElements()[dispAux - 1];
				aux.setNumElements(aux.getNumElements() - dispAux);
				for (int j = 0; j < aux.getNumElements(); j++)
					aux.getElements()[j] = aux.getElements()[j + dispAux];
				for (int j = 0; j <= aux.getNumElements(); j++)
					aux.getChildren()[j] = aux.getChildren()[j + dispAux];
				aux.getChildren()[aux.getNumElements() + dispAux] = null; // transfere a posse da memória
				shrunk = false;
			} else { // Fusão: intercala aux em page e libera aux
				for (int j = 0; j < this.halfPageCapacity; j++) {
					this.pageInsert(page, aux.getElements()[j], aux.getChildren()[j + 1]);
					aux.getChildren()[j + 1] = null; // transfere a posse da memória
				}
				aux = parentPage.getChildren()[parentIdx + 1] = null; // libera aux
				for (int j = parentIdx; j < parentPage.getNumElements() - 1; j++) {
					parentPage.getElements()[j] = parentPage.getElements()[j + 1];
					parentPage.getChildren()[j + 1] = parentPage.getChildren()[j + 2];
				}
				parentPage.getChildren()[parentPage.getNumElements()] = null; // transfere a posse da memória
				parentPage.setNumElements(parentPage.getNumElements() - 1);
				shrunk = parentPage.getNumElements() < this.halfPageCapacity;
			}
		} else { // aux = Página à esquerda de page
			Page aux = parentPage.getChildren()[parentIdx - 1];
			int dispAux = (aux.getNumElements() - this.halfPageCapacity + 1) / 2;
			for (int j = page.getNumElements() - 1; j >= 0; j--)
				page.getElements()[j + 1] = page.getElements()[j];
			page.getElements()[0] = parentPage.getElements()[parentIdx - 1];
			for (int j = page.getNumElements(); j >= 0; j--)
				page.getChildren()[j + 1] = page.getChildren()[j];
			page.setNumElements(page.getNumElements() + 1);
			if (dispAux > 0) { // Existe folga: transfere de aux para page
				for (int j = 0; j < dispAux - 1; j++) {
					this.pageInsert(page, aux.getElements()[aux.getNumElements() - j - 1],
							aux.getChildren()[aux.getNumElements() - j]);
					aux.getChildren()[aux.getNumElements() - j] = null; // transfere a posse da memória
				}
				page.getChildren()[0] = aux.getChildren()[aux.getNumElements() - dispAux + 1];
				aux.getChildren()[aux.getNumElements() - dispAux + 1] = null; // transfere a posse da memória
				parentPage.getElements()[parentIdx - 1] = aux.getElements()[aux.getNumElements() - dispAux];
				aux.setNumElements(aux.getNumElements() - dispAux);
				shrunk = false;
			} else { // Fusão: intercala page em aux e libera page
				for (int j = 0; j < this.halfPageCapacity; j++) {
					this.pageInsert(aux, page.getElements()[j], page.getChildren()[j + 1]);
					page.getChildren()[j + 1] = null; // transfere a posse da memória
				}
				page = null; // libera page
				parentPage.getChildren()[parentPage.getNumElements()] = null; // transfere a posse da memória
				parentPage.setNumElements(parentPage.getNumElements() - 1);
				shrunk = parentPage.getNumElements() < this.halfPageCapacity;
			}
		}
		return shrunk;
	}
}
