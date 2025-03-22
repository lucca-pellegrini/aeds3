package AEDs3.DataBase;

public class BTree implements Index {
	private static class Pagina {
		int n;
		IndexRegister r[];
		Pagina p[];

		public Pagina(int mm) {
			this.n = 0;
			this.r = new IndexRegister[mm];
			this.p = new Pagina[mm + 1];
		}
	}

	private Pagina raiz;
	private int m, mm;

	public BTree(int m) {
		this.raiz = null;
		this.m = m;
		this.mm = 2 * m;
	}

	public long search(int id) {
		return this.pesquisa(new IndexRegister(id, -1)).getPos();
	}

	private IndexRegister pesquisa(IndexRegister reg) {
		return this.pesquisa(reg, this.raiz);
	}

	private IndexRegister pesquisa(IndexRegister reg, Pagina ap) {
		if (ap == null)
			return null; // Registro não encontrado
		else {
			int i = 0;
			while ((i < ap.n - 1) && (reg.compareTo(ap.r[i]) > 0))
				i++;
			if (reg.compareTo(ap.r[i]) == 0)
				return ap.r[i];
			else if (reg.compareTo(ap.r[i]) < 0)
				return pesquisa(reg, ap.p[i]);
			else
				return pesquisa(reg, ap.p[i + 1]);
		}
	}

	public void insert(int id, long pos) {
		this.insere(new IndexRegister(id, pos));
	}

	private void insere(IndexRegister reg) {
		IndexRegister regRetorno[] = new IndexRegister[1];
		boolean cresceu[] = new boolean[1];
		Pagina apRetorno = this.insere(reg, this.raiz, regRetorno, cresceu);
		if (cresceu[0]) {
			Pagina apTemp = new Pagina(this.mm);
			apTemp.r[0] = regRetorno[0];
			apTemp.p[0] = this.raiz;
			apTemp.p[1] = apRetorno;
			this.raiz = apTemp;
			this.raiz.n++;
		} else
			this.raiz = apRetorno;
	}

	private Pagina insere(
			IndexRegister reg, Pagina ap, IndexRegister[] regRetorno, boolean[] cresceu) {
		Pagina apRetorno = null;
		if (ap == null) {
			cresceu[0] = true;
			regRetorno[0] = reg;
		} else {
			int i = 0;
			while ((i < ap.n - 1) && (reg.compareTo(ap.r[i]) > 0))
				i++;
			if (reg.compareTo(ap.r[i]) == 0) {
				System.out.println("Erro : Registro ja existente");
				cresceu[0] = false;
			} else {
				if (reg.compareTo(ap.r[i]) > 0)
					i++;
				apRetorno = insere(reg, ap.p[i], regRetorno, cresceu);
				if (cresceu[0])
					if (ap.n < this.mm) { // Página tem espaço
						this.insereNaPagina(ap, regRetorno[0], apRetorno);
						cresceu[0] = false;
						apRetorno = ap;
					} else { // Overflow: Página tem que ser dividida
						Pagina apTemp = new Pagina(this.mm);
						apTemp.p[0] = null;
						if (i <= this.m) {
							this.insereNaPagina(apTemp, ap.r[this.mm - 1], ap.p[this.mm]);
							ap.n--;
							this.insereNaPagina(ap, regRetorno[0], apRetorno);
						} else
							this.insereNaPagina(apTemp, regRetorno[0], apRetorno);
						for (int j = this.m + 1; j < this.mm; j++) {
							this.insereNaPagina(apTemp, ap.r[j], ap.p[j + 1]);
							ap.p[j + 1] = null; // transfere a posse da memória
						}
						ap.n = this.m;
						apTemp.p[0] = ap.p[this.m + 1];
						regRetorno[0] = ap.r[this.m];
						apRetorno = apTemp;
					}
			}
		}
		return (cresceu[0] ? apRetorno : ap);
	}

	private void insereNaPagina(Pagina ap, IndexRegister reg, Pagina apDir) {
		int k = ap.n - 1;
		while ((k >= 0) && (reg.compareTo(ap.r[k]) < 0)) {
			ap.r[k + 1] = ap.r[k];
			ap.p[k + 2] = ap.p[k + 1];
			k--;
		}
		ap.r[k + 1] = reg;
		ap.p[k + 2] = apDir;
		ap.n++;
	}

	// public void retira(IndexRegister reg) {
	// vide transparências 30, 31 e 32
	// }
}
