package AEDs3.DataBase.Index;

/**
 * Exceção lançada quando a capacidade de uma Tabela Hash é inválida.
 */
public class InvalidHashTableCapacityException extends IllegalArgumentException {
	/**
	 * Razões pelas quais a capacidade de uma Tabela Hash pode ser considerada
	 * inválida.
	 */
	public enum Reason {
		/**
		 * A capacidade do Bucket excede o valor máximo permitido.
		 * A capacidade deve ser menor que 2^{13}.
		 */
		TOO_LARGE("A capacidade do Bucket deve ser < 8192."),

		/**
		 * A capacidade do Bucket não é positiva.
		 * A capacidade deve ser um valor positivo.
		 */
		NON_POSITIVE("A capacidade do Bucket deve ser positiva.");

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
	 * A razão pela qual a capacidade da Tabela Hash é considerada inválida.
	 */
	private final Reason reason;

	/**
	 * A capacidade inválida da Tabela Hash que causou a exceção.
	 */
	private final int capacity;

	/**
	 * Construtor para criar uma exceção com a capacidade e a razão especificadas.
	 *
	 * @param capacity A capacidade inválida da Tabela Hash.
	 * @param reason   A razão pela qual a capacidade é inválida.
	 */
	public InvalidHashTableCapacityException(int capacity, Reason reason) {
		super(reason.getMessage() + " Recebemos: " + capacity);
		this.capacity = capacity;
		this.reason = reason;
	}

	/**
	 * Obtém a razão pela qual a capacidade da Tabela Hash é inválida.
	 *
	 * @return A razão da invalidade.
	 */
	public Reason getReason() {
		return reason;
	}

	/**
	 * Obtém a capacidade inválida da Tabela Hash.
	 *
	 * @return A capacidade inválida.
	 */
	public int getCapacity() {
		return capacity;
	}
}
