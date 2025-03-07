package AEDs3;

import AEDs3.DataBase.TrackDB;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

public class CommandLineInterface {
	private Terminal terminal;
	private LineReader reader;
	private TrackDB db;

	public CommandLineInterface() {
		try {
			terminal = TerminalBuilder.builder().build();
			reader = LineReaderBuilder.builder()
					.terminal(terminal)
					.option(LineReader.Option.AUTO_FRESH_LINE, true)
					.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void showWelcomeBanner() {
		// Monta a banner em duas partes: esquerda e direita, com cores diferentes.
		String[] bannerLeft = new String[8];
		bannerLeft[0] = "";
		bannerLeft[1] = "▄▄▄▄▄▄▄                      █      ▄▄▄▄   ▄▄▄▄▄   ";
		bannerLeft[2] = "   █     ▄ ▄▄   ▄▄▄    ▄▄▄   █   ▄  █   ▀▄ █    █  ";
		bannerLeft[3] = "   █     █▀  ▀ ▀   █  █▀  ▀  █ ▄▀   █    █ █▄▄▄▄▀  ";
		bannerLeft[4] = "   █     █     ▄▀▀▀█  █      █▀█    █    █ █    █  ";
		bannerLeft[5] = "   █     █     ▀▄▄▀█  ▀█▄▄▀  █  ▀▄  █▄▄▄▀  █▄▄▄▄▀  ";
		bannerLeft[6] = "";
		bannerLeft[7] = "══════════════════════════════════════════════════════════════════════\n";

		// Colore o título principal em ASCII art.
		for (int i = 1; i <= 5; ++i)
			bannerLeft[i] = new AttributedString(
					bannerLeft[i], AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
					.toAnsi();
		// Colore a linha separadora.
		bannerLeft[7] = new AttributedString(
				bannerLeft[7], AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA))
				.toAnsi();

		String[] bannerRight = new String[5];
		bannerRight[0] = "Banco de Dados";
		bannerRight[1] = "de Trilhas Musicais";
		bannerRight[2] = "em formato binário";
		bannerRight[3] = "Lucca Pellegrini";
		bannerRight[4] = "Pedro Vitor Andrade";

		// Colore a descrição do programa.
		for (int i = 0; i <= 2; ++i)
			bannerRight[i] = new AttributedString(
					bannerRight[i], AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
					.toAnsi();
		// Colore os nomes dos autores.
		for (int i = 3; i <= 4; ++i)
			bannerRight[i] = new AttributedString(
					bannerRight[i], AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
					.toAnsi();

		// Apende os segmentos.
		for (int i = 0; i <= 4; ++i)
			bannerLeft[i + 1] += bannerRight[i];

		// Printa o resultado
		for (String s : bannerLeft)
			terminal.writer().println(s);
	}

	private void start() {
		showWelcomeBanner();

		try {
			while (true) {
				String prompt = new AttributedString(
						"No File> ", AttributedStyle.BOLD.foreground(AttributedStyle.RED))
						.toAnsi();
				String input = reader.readLine(prompt).trim();
			}
		} catch (EndOfFileException e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		CommandLineInterface cli = new CommandLineInterface();
		cli.start();
	}
}
