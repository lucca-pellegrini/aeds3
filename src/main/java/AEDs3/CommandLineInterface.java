package AEDs3;

import AEDs3.DataBase.TrackDB;
import java.io.FileNotFoundException;
import org.jline.builtins.Completers.FileNameCompleter;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.widget.AutosuggestionWidgets;

public class CommandLineInterface {
	private Terminal terminal;
	private LineReader reader;

	public CommandLineInterface() {
		try {
			terminal = TerminalBuilder.builder().build();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		CommandLineInterface cli = new CommandLineInterface();
		cli.start();
	}

	private void start() {
		showWelcomeBanner();
		new MainMenu(terminal).loop();
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
}

abstract class CommandLineMenu {
	protected Terminal terminal;
	protected LineReader reader;
	protected String prompt;

	// Abstract method for building the completer specific to each menu
	protected abstract Completer buildCompleter();

	protected abstract void loop();

	// Constructor to initialize terminal and LineReader
	public CommandLineMenu(Terminal terminal) {
		this.terminal = terminal;
		this.reader = LineReaderBuilder.builder()
				.terminal(terminal)
				.option(LineReader.Option.AUTO_FRESH_LINE, true)
				.completer(buildCompleter())
				.build();
		new AutosuggestionWidgets(this.reader).enable();
	}

	// Optionally add more common methods for menus, like reading input, etc.
	public String readInput(String prompt) {
		return reader.readLine(prompt);
	}
}

class MainMenu extends CommandLineMenu {
	public MainMenu(Terminal terminal) {
		super(terminal);
		prompt = new AttributedString(
				"TrackDB> ", AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW))
				.toAnsi();
	}

	@Override
	protected void loop() {
		try {
			while (true) {
				String input = reader.readLine(prompt).trim();
				try (TrackDB db = new TrackDB(input)) {
					terminal.writer().println(new AttributedString(
							"Arquivo aberto", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
							.toAnsi());
					// crudMenu(db);
				} catch (FileNotFoundException e) {
					terminal.writer().println(new AttributedString("Diretório inexistente!",
							AttributedStyle.BOLD.foreground(AttributedStyle.RED))
							.toAnsi());
				}
			}
		} catch (EndOfFileException e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Completer buildCompleter() {
		// MainMenu-specific completer logic
		return new ArgumentCompleter(new StringsCompleter("open", "new"), // Command completion
				new FileNameCompleter() // File path completion
		);
	}
}
