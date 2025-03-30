package AEDs3;

/**
 * Classe principal do programa que inicia a interface de linha de comando.
 * <p>
 * A classe {@link App} contém o método {@link #main(String[])} que é o ponto de
 * entrada
 * do aplicativo. Ao ser executada, ela cria uma nova instância da classe
 * {@link CommandLineInterface} com os argumentos fornecidos.
 */
public class App {
	/**
	 * Método principal do programa.
	 * <p>
	 * Este método é o ponto de entrada do aplicativo e é responsável por iniciar
	 * a interface de linha de comando. Ele cria uma nova instância da classe
	 * {@link CommandLineInterface}, passando os argumentos fornecidos na execução.
	 *
	 * @param args Argumentos de linha de comando fornecidos na execução do
	 *             programa.
	 */
	public static void main(String[] args) {
		new CommandLineInterface(args);
	}
}
