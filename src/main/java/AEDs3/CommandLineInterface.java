package AEDs3;

import static AEDs3.DataBase.Track.Field.*;
import static org.fusesource.jansi.Ansi.*;

import AEDs3.DataBase.BalancedMergeSort;
import AEDs3.DataBase.CSVManager;
import AEDs3.DataBase.Track;
import AEDs3.DataBase.Track.Field;
import AEDs3.DataBase.TrackDB;
import AEDs3.DataBase.TrackDB.TrackFilter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.fusesource.jansi.Ansi;
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
import picocli.CommandLine.ArgGroup;
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
			"Hit @|magenta <TAB>|@ to see available commands.",
			"hit @|magenta Alt-S|@ to toggle tailtip hints.",
			"" }, footer = { "", "Hit @|magenta Ctrl-C|@ to exit." }, subcommands = { OpenCommand.class,
					CloseCommand.class, InfoCommand.class, UsageCommand.class,
					ImportCommand.class, ReadCommand.class, DeleteCommand.class, CreateCommand.class,
					SortCommand.class })
	class CliCommands implements Runnable {
		LineReader reader;
		PrintWriter out;
		AutosuggestionWidgets suggestions;
		TrackDB db;

		String prompt;
		String rightPrompt;

		static final String DEFAULT_PROMPT = ansi().fgYellow().bold().a("TrackDB> ").toString();
		static final String DEFAULT_RIGHT_PROMPT = ansi().fgRed().a("[Nenhum arquivo aberto]").toString();
		static final String ERROR_PROMPT = ansi().bold().render("@|red Erro:|@ ").toString();
		static final String WARN_PROMPT = ansi().bold().render("@|yellow Warn:|@ ").toString();
		static final String INFO_PROMPT = ansi().bold().render("@|blue Info:|@ ").toString();

		CliCommands() {
			prompt = DEFAULT_PROMPT;
			rightPrompt = DEFAULT_RIGHT_PROMPT;
		}

		void printTrack(Track track) {
			if (track == null) {
				warn("Nenhuma track foi encontrada.");
				return;
			}

			List<String> tmp;

			out.println();

			out.print(ansi().bold().fgGreen().a(track.getId() + ":\t\t"));
			out.println(ansi().bold().fgMagenta().a(track.getName()));

			out.println(ansi().bold().fgMagenta().a(
					"──────────────────────────────────────────────────────────────────────"));

			tmp = track.getTrackArtists();
			out.print(ansi().bold().fgYellow().a("Artists:\t").reset());
			for (int i = 0; i < tmp.size() - 1; ++i)
				out.print(tmp.get(i) + ", ");
			out.println(tmp.get(tmp.size() - 1));

			out.print(ansi().bold().fgYellow().a("Album:\t\t").reset());
			out.println(track.getAlbumName());

			out.print(ansi().bold().fgYellow().a("Release Date:\t").reset());
			out.println(track.getAlbumReleaseDate());

			out.print(ansi().bold().fgYellow().a("Album Type:\t").reset());
			out.println(track.getAlbumType());

			tmp = track.getGenres();
			out.print(ansi().bold().fgYellow().a("Genres:\t\t").reset());
			for (int i = 0; i < tmp.size() - 1; ++i)
				out.print(tmp.get(i) + ", ");
			out.println(tmp.get(tmp.size() - 1));

			out.print(ansi().bold().fgYellow().a("Explicit:\t").reset());
			out.println(track.isExplicit());

			out.print(ansi().bold().fgYellow().a("Spotify ID:\t").reset());
			out.println(new String(track.getTrackId()));

			out.print(ansi().bold().fgYellow().a("Popularity:\t").reset());
			out.println(track.getPopularity());

			out.print(ansi().bold().fgYellow().a("Key:\t\t").reset());
			out.println(track.getKey());

			out.print(ansi().bold().fgYellow().a("Danceability:\t").reset());
			out.println(track.getDanceability());

			out.print(ansi().bold().fgYellow().a("Energy:\t\t").reset());
			out.println(track.getEnergy());

			out.print(ansi().bold().fgYellow().a("Loudness:\t").reset());
			out.println(track.getLoudness());

			out.print(ansi().bold().fgYellow().a("Tempo:\t\t").reset());
			out.println(track.getTempo());

			out.print(ansi().bold().fgYellow().a("Valence:\t").reset());
			out.println(track.getValence());

			out.println();
		}

		void printAllTracks() {
			for (Track track : db)
				printTrack(track);
		}

		void error(String msg) {
			out.println(ERROR_PROMPT + msg);
		}

		void warn(String msg) {
			out.println(WARN_PROMPT + msg);
		}

		void info(String msg) {
			out.println(INFO_PROMPT + msg);
		}

		public void setReader(LineReader reader) {
			this.reader = reader;
			this.out = reader.getTerminal().writer();
		}

		public void setSuggestions(AutosuggestionWidgets suggestions) {
			this.suggestions = suggestions;
		}

		public void run() {
			out.println(new CommandLine(this).getUsageMessage());
		}
	}

	@Command(name = "usage", mixinStandardHelpOptions = true, description = "Print main program help.")
	static class UsageCommand implements Runnable {
		@ParentCommand
		CliCommands parent;

		public void run() {
			parent.out.println(new CommandLine(parent).getUsageMessage());
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

				parent.prompt = ansi().bold().fgCyan().a(param + "> ").toString();
				parent.rightPrompt = ansi().fgGreen().a("[Nenhum filtro aplicado]").toString();
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

	@Command(name = "info", mixinStandardHelpOptions = true, description = "Print information about the opened file.")
	static class InfoCommand implements Runnable {
		@ParentCommand
		CliCommands parent;

		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			int numTracks = parent.db.getNumTracks();
			int numSpaces = parent.db.getNumSpaces();
			float efficiency = numTracks / (float) numSpaces;

			parent.out.println(
					ansi().bold().fgGreen().a("File ID:\t").reset().a(parent.db.getUUID()));
			parent.out.println(
					ansi().bold().fgGreen().a("Last ID:\t").reset().a(parent.db.getLastId()));
			parent.out.println(ansi().bold().fgGreen().a("Total Tracks:\t").reset().a(numTracks));
			parent.out.println(ansi().bold().fgGreen().a("Used Spaces:\t").reset().a(numSpaces));
			parent.out.println(
					ansi().bold().fgGreen().a("Efficiency:\t").reset().a(100. * efficiency + "%"));
		}
	}

	@Command(name = "import", mixinStandardHelpOptions = true, description = "Import tracks from a CSV file.")
	static class ImportCommand implements Runnable {
		@Parameters(paramLabel = "<path>", description = "Path to the CSV source file.")
		private Path param;

		@ParentCommand
		CliCommands parent;

		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			try (CSVManager csvManager = new CSVManager(param.toString())) {
				int count = 0;
				for (Track t : csvManager) {
					++count;
					parent.db.create(t);
				}
				parent.info("Imported " + count + " items. Last ID: " + parent.db.getLastId());
			} catch (IOException e) {
				e.printStackTrace();
				parent.error("Erro fatal de IO ao tentar ler o CSV.");
				return;
			}
		}
	}

	@Command(name = "read", mixinStandardHelpOptions = true, description = "Read track(s) by field or by ID.", footer = {
			"Examples:", "read 117 @|magenta (read by ID 117)|@",
			"read --field=GENRES pop rock @|magenta (read entries with Genres including pop "
					+ "and rock)|@",
			"read --field=TRACK_ARTISTS \"Frank Sinatra\" @|magenta (read tracks by Frank "
					+ "Sinatra)|@",
			"read --field=NAME --regex 'You.*Gone' @|magenta (read track names matching RegEx)|@" })
	static class ReadCommand implements Runnable {
		@ArgGroup(exclusive = true)
		private AllOrField allOrField = new AllOrField();

		static class AllOrField {
			@Option(names = { "-f", "--field" }, description = "What field to search on.", defaultValue = "ID")
			Field field = ID;

			@Option(names = { "-a", "--all" }, description = "Read all registers.", defaultValue = "false")
			boolean all = false;
		}

		@Option(names = { "-r", "--regex" }, description = "Match strings by regex.", defaultValue = "false")
		boolean regex = false;

		@Parameters(paramLabel = "<value>", description = "Value to search.")
		String[] params;

		@ParentCommand
		CliCommands parent;

		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			if (allOrField.all) {
				TrackFilter oldFilter = parent.db.getFilter();
				try {
					parent.db.clearFilter();
					parent.printAllTracks();
				} finally {
					parent.db.setFilter(oldFilter);
				}
				return;
			}

			Field field = allOrField.field;

			if (params == null) {
				parent.out.println(new CommandLine(this).getUsageMessage());
				return;
			} else if (params.length > 1) {
				switch (field) {
					case ID:
					case NAME:
					case ALBUM_NAME:
					case ALBUM_RELEASE_DATE:
					case ALBUM_TYPE:
					case TRACK_ID:
					case POPULARITY:
					case KEY:
						parent.error(
								"O campo " + field.toString() + " exige exatamente um parâmetro.");
						return;
					default:
				}
			}

			String singleParam = params[0];

			try {
				if (field == ID) {
					parent.printTrack(parent.db.read(Integer.parseInt(singleParam)));
					return;
				}
			} catch (IOException e) {
				e.printStackTrace();
				parent.error("Erro fatal de IO ao tentar ler o registro.");
				return;
			} catch (NumberFormatException e) {
				parent.error(singleParam + " não é um numero inteiro.");
				return;
			}

			// Salva o filtro de busca anterior.
			TrackFilter oldFilter = parent.db.getFilter();

			try {
				TrackFilter newFilter = switch (field) {
					case NAME, ALBUM_NAME -> {
						if (regex)
							yield new TrackFilter(field,
									Pattern.compile(
											singleParam, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
						else
							yield new TrackFilter(field, singleParam);
					}

					case TRACK_ID -> {
						if (singleParam.length() != Track.getTrackIdNumChars()) {
							parent.error(field.toString() + " deve conter exatamente "
									+ Track.getTrackIdNumChars() + " caracteres.");
							throw new IllegalArgumentException();
						}
						yield new TrackFilter(field, singleParam);
					}

					case ALBUM_RELEASE_DATE -> new TrackFilter(field, LocalDate.parse(singleParam));
					case ALBUM_TYPE -> new TrackFilter(field, singleParam);
					case POPULARITY, KEY -> new TrackFilter(field, Integer.parseInt(singleParam));
					case TRACK_ARTISTS, GENRES -> new TrackFilter(field, Arrays.asList(params));

					case DANCEABILITY, ENERGY, LOUDNESS, TEMPO, VALENCE -> {
						parent.error("Não é possível buscar por valores de tipo float.");
						throw new IllegalArgumentException();
					}

					case EXPLICIT -> {
						parent.error("Não é possível buscar por valores de tipo booleano.");
						throw new IllegalArgumentException();
					}

					case ID -> {
						// ID é um caso especial, tratado acima.
						throw new AssertionError();
					}
				};

				parent.db.setFilter(newFilter);
				parent.printAllTracks();
			} catch (NumberFormatException | DateTimeParseException e) {
				parent.error("O valor não está formatado corretamente.");
			} catch (Exception e) {
				parent.warn("Por favor, tente outro termo de busca.");
			} finally {
				parent.db.setFilter(oldFilter);
			}
		}
	}

	@Command(name = "delete", mixinStandardHelpOptions = true, description = "Delete a track by ID.")
	static class DeleteCommand implements Runnable {
		@Parameters(paramLabel = "<ID>", description = "Track's primary key.")
		int id;

		@ParentCommand
		CliCommands parent;

		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			try {
				parent.db.delete(id);
			} catch (NoSuchElementException e) {
				parent.error("O ID " + id + " não existe nesse arquivo.");
				return;
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Erro ao deletar " + id);
			}
		}
	}

	@Command(name = "create", mixinStandardHelpOptions = true, description = "Create a new track.")
	static class CreateCommand implements Runnable {
		private Ansi rightPrompt = ansi().bold().fgBrightYellow();
		LineReader reader;

		@ParentCommand
		CliCommands parent;

		private String read(String prompt) {
			return reader.readLine(ansi().bold().fgBrightBlue().a(prompt + ": ").reset().toString(),
					this.rightPrompt.toString(), (MaskingCallback) null, null);
		}

		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			reader = LineReaderBuilder.builder().terminal(parent.reader.getTerminal()).build();
			rightPrompt = rightPrompt.a("[Editando ID " + (parent.db.getLastId() + 1) + "]");

			try {
				parent.suggestions.disable();
				Track t = new Track();
				t.setName(read("Name"));
				t.setTrackArtists(Arrays.asList(read("Artists").split(",")));
				t.setAlbumName(read("Album"));
				t.setAlbumReleaseDate(LocalDate.parse(read("Release Date (YYYY-MM-DD)")));
				t.setAlbumType(read("Album Type"));
				t.setGenres(Arrays.asList(read("Genres").split(",")));
				t.setExplicit(Boolean.parseBoolean(read("Explicit (true/false)")));
				t.setTrackId(read("Spotify ID (22 characters)").toCharArray());
				t.setPopularity(Integer.parseInt(read("Popularity")));
				t.setDanceability(Float.parseFloat(read("Danceability")));
				t.setEnergy(Float.parseFloat(read("Energy")));
				t.setLoudness(Float.parseFloat(read("Loudness")));
				t.setTempo(Float.parseFloat(read("Tempo")));
				t.setValence(Float.parseFloat(read("Valence")));
				parent.db.create(t);
			} catch (EndOfFileException e) {
				parent.warn("Criação de registro cancelada.");
			} catch (IOException e) {
				e.printStackTrace();
				parent.error("Erro fatal de IO ao tentar criar o registro.");
				return;
			} finally {
				parent.suggestions.enable();
			}
		}
	}

	@Command(name = "sort", mixinStandardHelpOptions = true, description = "Sort the database.")
	static class SortCommand implements Runnable {
		@Option(names = { "-f", "--fanout" }, description = { "Fanout for the balanced merge sort.",
				"(Number of elements merged at a time.)" }, defaultValue = "8")
		int fanout = 8;

		@Option(names = { "-n", "--num" }, description = { "Maximum number of elements in the in-memory heap",
				"to use during sorting." }, defaultValue = "64")
		int maxHeapSize = 64;

		@Option(names = {"-v", "--verbose"}, description = "Enable verbose output.")
		boolean verbose = false;

		@ParentCommand
		CliCommands parent;

		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			try {
				BalancedMergeSort sorter = new BalancedMergeSort(parent.db, fanout, maxHeapSize);
				sorter.setVerbose(verbose);
				sorter.sort();
			} catch (IllegalArgumentException e) {
				parent.error(e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				parent.error("Erro fatal de IO ao tentar ordenar o banco de dados.");
				return;
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
			commands.setSuggestions(suggestions);
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
