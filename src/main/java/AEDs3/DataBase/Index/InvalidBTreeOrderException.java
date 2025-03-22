package AEDs3.DataBase.Index;

public class InvalidBTreeOrderException extends IllegalArgumentException {
	public enum Reason {
		TOO_SMALL("A ordem da Árvore B deve ser ≥ 3."),
		NOT_EVEN("A ordem da Árvore B deve ser um número par.");

		private final String message;

		Reason(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}
	}

	private final Reason reason;
	private final int order;

	public InvalidBTreeOrderException(int order, Reason reason) {
		super(reason.getMessage() + " Recebemos: " + order);
		this.order = order;
		this.reason = reason;
	}

	public Reason getReason() {
		return reason;
	}

	public int getOrder() {
		return order;
	}
}
