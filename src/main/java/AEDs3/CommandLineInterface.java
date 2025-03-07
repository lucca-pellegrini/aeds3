package AEDs3;

import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

import AEDs3.DataBase.TrackDB;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import org.fusesource.jansi.AnsiConsole;
import org.jline.builtins.ConfigurationPath;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp.Capability;
import org.jline.widget.AutosuggestionWidgets;
import org.jline.widget.TailTipWidgets;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory;

public class CommandLineInterface {
	private Terminal terminal;

	// Comando top-level. Somente printa ajuda.
	@Command(name = "", description = { "Music tracks binary database.",
			"Pressione @|magenta <TAB>|@ para ver os comandos.",
			"Pressione @|magenta Alt-S|@ para (des)habilitar tailtips.",
			"" }, footer = { "", "Pressione @|magenta Ctrl-D|@ para sair." }, subcommands = { OpenCommand.class,
					CloseCommand.class })
	class CliCommands implements Runnable {
		PrintWriter out;
		TrackDB db;

		String prompt;
		String rightPrompt;

		static final String DEFAULT_PROMPT = ansi().fg(YELLOW).bold().a("TrackDB> ").toString();
		static final String DEFAULT_RIGHT_PROMPT = ansi().fg(YELLOW).a("Nenhum arquivo aberto").toString();

		CliCommands() {
			prompt = DEFAULT_PROMPT;
			rightPrompt = DEFAULT_RIGHT_PROMPT;
		}

		void error(String msg) {
			out.println(ansi().bold().render("@|red Erro:|@ " + msg));
		}

		void warn(String msg) {
			out.println(ansi().bold().render("@|yellow Warn:|@ " + msg));
		}

		void info(String msg) {
			out.println(ansi().bold().render("@|blue Info:|@ " + msg));
		}

		public void setReader(LineReader reader) {
			out = reader.getTerminal().writer();
		}

		public void run() {
			out.println(new CommandLine(this).getUsageMessage());
		}
	}

	// Abertura do DB.
	@Command(name = "open", mixinStandardHelpOptions = true, description = "Open a TrackDB file.")
	static class OpenCommand implements Runnable {
		@Option(names = { "-n", "--new" }, description = "Create a new file if it does not exist.")
		private boolean create;

		@Parameters(paramLabel = "<path>", description = "Path to the file.")
		private Path param;

		@ParentCommand
		CliCommands parent;

		public void run() {
			if (!create && !new File(param.toString()).exists()) {
				parent.error("O arquivo não existe.");
			} else {
				try {
					parent.db = new TrackDB(param.toString());
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Erro ao abrir " + param);
				}

				parent.prompt = ansi().bold().fg(CYAN).a(param + "> ").toString();
				parent.rightPrompt = ansi().fg(GREEN).a("Nenhum filtro aplicado").toString();
				parent.info("Arquivo aberto.");
			}
		}
	}

	@Command(name = "close", mixinStandardHelpOptions = true, description = "Close the opened TrackDB file.")
	static class CloseCommand implements Runnable {
		@ParentCommand
		CliCommands parent;

		public void run() {
			if (parent.db == null) {
				parent.warn("Não há nenhum arquivo aberto.");
			} else {
				parent.db = null;
				parent.prompt = CliCommands.DEFAULT_PROMPT;
				parent.rightPrompt = CliCommands.DEFAULT_RIGHT_PROMPT;
			}
		}
	}

	public CommandLineInterface(String[] args) {
		AnsiConsole.systemInstall();

		try {
			Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
			Builtins builtins = new Builtins(workDir, new ConfigurationPath(workDir.get(), workDir.get()), null);
			CliCommands commands = new CliCommands();
			PicocliCommandsFactory factory = new PicocliCommandsFactory();
			CommandLine cmd = new CommandLine(commands, factory);
			PicocliCommands picocliCommands = new PicocliCommands(cmd);
			picocliCommands.name("TrackDB commands");
			Parser parser = new DefaultParser();
			terminal = TerminalBuilder.builder().build();
			terminal.puts(Capability.clear_screen); // Limpa a tela.
			SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
			systemRegistry.setCommandRegistries(builtins, picocliCommands);
			systemRegistry.register("help", picocliCommands);

			LineReader reader = LineReaderBuilder.builder()
					.terminal(terminal)
					.completer(systemRegistry.completer())
					.parser(parser)
					.variable(LineReader.LIST_MAX, 100)
					.build();

			builtins.setLineReader(reader);
			commands.setReader(reader);
			factory.setTerminal(terminal);
			TailTipWidgets tailtip = new TailTipWidgets(
					reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
			tailtip.enable();
			AutosuggestionWidgets suggestions = new AutosuggestionWidgets(reader);
			suggestions.enable();
			KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
			keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

			showWelcomeBanner();

			String line;
			while (true) {
				try {
					systemRegistry.cleanUp();
					line = reader.readLine(
							commands.prompt, commands.rightPrompt, (MaskingCallback) null, null);
					systemRegistry.execute(line);
				} catch (UserInterruptException | EndOfFileException e) {
					commands.info("Programa finalizado");
					tailtip.disable();
					suggestions.disable();
					System.exit(0);
				} catch (Exception e) {
					systemRegistry.trace(e);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			AnsiConsole.systemUninstall();
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
}
