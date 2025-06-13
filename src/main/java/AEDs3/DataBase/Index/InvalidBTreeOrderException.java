package AEDs3.DataBase.Index;

/**
 * Exceção lançada quando a ordem de uma Árvore B é inválida.
 */
public class InvalidBTreeOrderException extends IllegalArgumentException {
	/**
	 * Razões pelas quais a ordem de uma Árvore B pode ser considerada inválida.
	 */
	public enum Reason {
		/**
		 * A ordem da Árvore B é muito pequena.
		 * A ordem deve ser maior ou igual a 3.
		 */
		TOO_SMALL("A ordem da Árvore B deve ser ≥ 3."),

		/**
		 * A ordem da Árvore B não é um número par.
		 * A ordem deve ser um número par.
		 */
		NOT_EVEN("A ordem da Árvore B deve ser um número par.");

		/**
		 * Mensagem descritiva da razão.
		 */
		private final String message;

		/**
		 * Construtor para definir a mensagem associada à razão.
		 *
		 * @param message A mensagem descritiva da razão.
		 */
		Reason(String message) {
			this.message = message;
		}

		/**
		 * Obtém a mensagem descritiva da razão.
		 *
		 * @return A mensagem da razão.
		 */
		public String getMessage() {
			return message;
		}
	}

	/**
	 * A razão pela qual a ordem da Árvore B é inválida.
	 */
	private final Reason reason;

	/**
	 * A ordem inválida da Árvore B.
	 */
	private final int order;

	/**
	 * Construtor para criar uma exceção com a ordem e a razão especificadas.
	 *
	 * @param order  A ordem inválida da Árvore B.
	 * @param reason A razão pela qual a ordem é inválida.
	 */
	public InvalidBTreeOrderException(int order, Reason reason) {
		super(reason.getMessage() + " Recebemos: " + order);
		this.order = order;
		this.reason = reason;
	}

	/**
	 * Obtém a razão pela qual a ordem da Árvore B é inválida.
	 *
	 * @return A razão da invalidade.
	 */
	public Reason getReason() {
		return reason;
	}

	/**
	 * Obtém a ordem inválida da Árvore B.
	 *
	 * @return A ordem inválida.
	 */
	public int getOrder() {
		return order;
	}
}
