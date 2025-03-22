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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

/**
 * Classe principal de interface de linha de comando (CLI) para o gerenciamento
 * de arquivos TrackDB. Utiliza JLine3 para manipulação de entrada do usuário e
 * Picocli para definir os comandos da interface.
 */
public class CommandLineInterface {
	private Terminal terminal;

	/**
	 * Comando principal para exibir informações sobre o programa e os comandos
	 * disponíveis.
	 *
	 * <p>
	 * Esta classe serve como o "pai" de todos os outros comandos do sistema(como
	 * <code>OpenCommand</code>, <code>CloseCommand</code>,
	 * <code>InfoCommand</code>, etc.), funcionando como um ponto central para
	 * agrupar e fornecer um ambiente compartilhado onde os subcomandos podem
	 * acessar dados e exibir informações na linha de comando (CLI) para o usuário.
	 * </p>
	 *
	 * <p>
	 * Ela é responsável por configurar e controlar a interface com o usuário,
	 * providenciando prompts personalizados, habilitando e desabilitando as
	 * capabilidades do terminal, e exibindo mensagens de erro, aviso e informação.
	 * </p>
	 */
	@Command(name = "", description = { "Banco de dados binário de faixas de música.",
			"Pressione @|magenta <TAB>|@ para ver os comandos disponíveis.",
			"pressione @|magenta Alt-S|@ para alternar as dicas de tailtip.",
			"" }, footer = { "", "Pressione @|magenta Ctrl-C|@ para sair." }, subcommands = { OpenCommand.class,
					CloseCommand.class, InfoCommand.class, UsageCommand.class,
					ImportCommand.class, ReadCommand.class, DeleteCommand.class, CreateCommand.class,
					UpdateCommand.class, SortCommand.class, IndexCommand.class })
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

		/**
		 * Exibe as informações detalhadas de uma faixa de música.
		 *
		 * @param track A faixa de música a ser exibida.
		 */
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

		/**
		 * Exibe todas as faixas no banco de dados.
		 */
		void printAllTracks() {
			for (Track track : db)
				printTrack(track);
		}

		/**
		 * Exibe uma mensagem de erro.
		 *
		 * @param msg A mensagem de erro a ser exibida.
		 */
		void error(String msg) {
			out.println(ERROR_PROMPT + msg);
		}

		/**
		 * Exibe uma mensagem de aviso.
		 *
		 * @param msg A mensagem de aviso a ser exibida.
		 */
		void warn(String msg) {
			out.println(WARN_PROMPT + msg);
		}

		/**
		 * Exibe uma mensagem de informação.
		 *
		 * @param msg A mensagem informativa a ser exibida.
		 */
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

		/**
		 * Executa o comando que exibe a ajuda da linha de comando.
		 */
		public void run() {
			out.println(new CommandLine(this).getUsageMessage());
		}
	}

	/**
	 * Comando para exibir a ajuda do programa.
	 */
	@Command(name = "usage", mixinStandardHelpOptions = true, description = "Exibe a ajuda principal do programa.")
	static class UsageCommand implements Runnable {
		@ParentCommand
		CliCommands parent;

		public void run() {
			parent.out.println(new CommandLine(parent).getUsageMessage());
		}
	}

	/**
	 * Comando para abrir um banco de dados de faixas TrackDB.
	 *
	 * <p>
	 * Este comando é responsável por abrir um arquivo TrackDB existente ou criar um
	 * novo arquivo caso o parâmetro <code>-n</code> (ou <code>--new</code>) seja
	 * utilizado e o arquivo não exista.
	 * </p>
	 *
	 * <p>
	 * O arquivo de banco de dados será carregado e as informações relacionadas ao
	 * arquivo aberto serão configuradas para o ambiente CLI.
	 * </p>
	 *
	 * @see TrackDB
	 */
	@Command(name = "open", mixinStandardHelpOptions = true, description = "Abre um arquivo TrackDB.")
	static class OpenCommand implements Runnable {
		/**
		 * Define se um novo arquivo será criado caso o arquivo especificado não exista.
		 * Se não for especificado, o arquivo deve existir para ser aberto.
		 */
		@Option(names = { "-n", "--new" }, description = "Cria um novo arquivo se não existir.")
		private boolean create;

		/**
		 * Caminho do arquivo que será aberto ou criado, se necessário.
		 */
		@Parameters(paramLabel = "<path>", description = "Caminho para o arquivo.")
		private Path param;

		/**
		 * Referência para o comando pai, utilizado para acessar a instância do banco de
		 * dados e outros recursos.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Abre o arquivo TrackDB especificado pelo caminho.
		 *
		 * <p>
		 * Se o arquivo não existir e o parâmetro <code>--new</code> não for
		 * especificado, um erro será exibido. Caso o arquivo exista, ele será carregado
		 * e o prompt será alterado para refletir o arquivo aberto.
		 * </p>
		 */
		public void run() {
			if (!create && !new File(param.toString()).exists()) {
				parent.error("O arquivo não existe.");
			} else {
				try {
					parent.db = new TrackDB(param.toString()); // Carrega o banco de dados.
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Erro ao abrir " + param);
				}

				// Atualiza o prompt da CLI para refletir o arquivo aberto.
				parent.prompt = ansi().bold().fgCyan().a(param + "> ").toString();
				parent.rightPrompt = ansi().fgGreen().a("[CRUD]").toString();
				parent.info("Arquivo aberto.");
			}
		}
	}

	/**
	 * Comando para fechar o arquivo do banco de dados aberto.
	 *
	 * <p>
	 * Este comando é utilizado para fechar o arquivo TrackDB atualmente aberto. Se
	 * não houver nenhum arquivo aberto, uma mensagem de aviso será exibida.
	 * </p>
	 *
	 * <p>
	 * Após o fechamento do arquivo, o prompt da linha de comando será restaurado
	 * para seus valores padrão.
	 * </p>
	 */
	@Command(name = "close", mixinStandardHelpOptions = true, description = "Fecha o arquivo TrackDB aberto.")
	static class CloseCommand implements Runnable {
		/**
		 * Referência para o comando pai, utilizado para acessar a instância do banco de
		 * dados e outros recursos.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Fecha o arquivo TrackDB aberto e restaura o prompt padrão.
		 *
		 * <p>
		 * Se nenhum arquivo estiver aberto, uma mensagem de aviso será exibida. Caso
		 * contrário, o banco de dados será fechado e o prompt será restaurado.
		 * </p>
		 */
		public void run() {
			if (parent.db == null) {
				parent.warn("Não há nenhum arquivo aberto.");
			} else {
				parent.db = null; // Fecha o banco de dados.
				parent.prompt = CliCommands.DEFAULT_PROMPT; // Restaura o prompt padrão.
				parent.rightPrompt = CliCommands.DEFAULT_RIGHT_PROMPT; // Restaura o prompt direito padrão.
			}
		}
	}

	/**
	 * Comando para exibir informações sobre o arquivo de banco de dados aberto.
	 *
	 * <p>
	 * Este comando exibe informações detalhadas sobre o banco de dados TrackDB
	 * atualmente aberto, incluindo o ID do arquivo, o último ID utilizado, o número
	 * de faixas e de espaços utilizados, a ordem dos registros e o aproveitamento
	 * de espaço do banco de dados.
	 * </p>
	 *
	 * <p>
	 * A eficiência é calculada como a razão entre o número de faixas e o número
	 * total de espaços utilizados no banco de dados. A cor da eficiência varia
	 * conforme o valor: verde para eficiência alta (>= 90%), amarelo para
	 * eficiência média (>= 50%) e vermelho para eficiência baixa (< 50%).
	 * </p>
	 */
	@Command(name = "info", mixinStandardHelpOptions = true, description = "Exibe informações sobre o arquivo aberto.")
	static class InfoCommand implements Runnable {
		/**
		 * Referência para o comando pai, utilizado para acessar a instância do banco de
		 * dados e outros recursos.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Exibe as informações detalhadas sobre o banco de dados TrackDB aberto.
		 *
		 * <p>
		 * O comando exibe informações como o UUID do arquivo, o último ID registrado, o
		 * número de faixas e espaços utilizados, o estado de ordenação do banco de
		 * dados e a eficiência (número de faixas em relação aos espaços).
		 * </p>
		 *
		 * <p>
		 * Se nenhum banco de dados estiver aberto, uma mensagem de erro será exibida.
		 * </p>
		 */
		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			// Calcula a eficiência do banco de dados.
			Ansi tmp;
			int numTracks = parent.db.getNumTracks();
			int numSpaces = parent.db.getNumSpaces();
			float efficiency = numTracks / (float) numSpaces;

			// Exibe as informações sobre o banco de dados.
			parent.out.println(
					ansi().bold().fgGreen().a("File ID:\t").reset().a(parent.db.getUUID()));
			parent.out.println(
					ansi().bold().fgGreen().a("Last ID:\t").reset().a(parent.db.getLastId()));
			parent.out.println(ansi().bold().fgGreen().a("Total Tracks:\t").reset().a(numTracks));
			parent.out.println(ansi().bold().fgGreen().a("Used Spaces:\t").reset().a(numSpaces));

			// Exibe o estado do índice.
			tmp = ansi().bold().fgGreen().a("Indexed:\t").reset();
			tmp = (parent.db.isIndexed()) ? parent.db.isBTreeIndex()
					? tmp.fgBrightBlue().a("B-Tree")
					: parent.db.isHashIndex() ? tmp.fgBrightBlue().a("Dynamic Hash")
							: tmp.fgBrightBlue().a("Inverted List")
					: tmp.fgBrightRed().a("false");
			parent.out.println(tmp);

			// Exibe o estado de ordenação.
			tmp = ansi().bold().fgGreen().a("Ordered:\t").reset();
			tmp = (parent.db.isOrdered()) ? tmp.fgBrightGreen().a("true")
					: tmp.fgBrightRed().a("false");
			parent.out.println(tmp);

			// Exibe a eficiência com a cor correspondente.
			tmp = ansi().bold().fgGreen().a("Efficiency:\t").reset();
			tmp = (efficiency >= 0.9) ? tmp.fgBrightGreen()
					: (efficiency >= 0.5) ? tmp.fgBrightYellow()
							: tmp.fgBrightRed();
			parent.out.println(tmp.a(100. * efficiency + "%"));
		}
	}

	/**
	 * Comando responsável por importar faixas de música a partir de um arquivo CSV.
	 * Este comando lê o arquivo CSV especificado pelo caminho e insere as faixas no
	 * banco de dados.
	 *
	 * <p>
	 * O comando verifica se o banco de dados está aberto antes de realizar a
	 * importação. Caso contrário, será exibida uma mensagem de erro informando que
	 * não há nenhum arquivo aberto.
	 * </p>
	 *
	 * <p>
	 * Ao importar as faixas, o comando cria uma instância do {@link CSVManager}
	 * para ler os dados do arquivo e insere cada faixa no banco de dados utilizando
	 * o método {@link Database#create(Track)}.
	 * </p>
	 *
	 * <p>
	 * Após a importação, o comando exibe o número total de itens importados e o
	 * último ID gerado.
	 * </p>
	 *
	 * <p>
	 * Em caso de erro ao tentar ler o arquivo CSV, será exibida uma mensagem de
	 * erro de IO.
	 * </p>
	 *
	 * @see CSVManager
	 * @see Track
	 * @see Database
	 */
	@Command(name = "import", mixinStandardHelpOptions = true, description = "Importar faixas de um arquivo CSV.")
	static class ImportCommand implements Runnable {
		/**
		 * Caminho para o arquivo CSV de origem a ser importado.
		 * O caminho é passado como parâmetro ao executar o comando.
		 *
		 * @param param Caminho do arquivo CSV a ser importado.
		 */
		@Parameters(paramLabel = "<path>", description = "Caminho para o arquivo CSV de origem.")
		private Path param;

		/**
		 * Referência para o comando pai, usada para acessar o banco de dados e exibir
		 * mensagens de erro e informações.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Executa a importação das faixas de música a partir do arquivo CSV.
		 * O comando verifica se o banco de dados está aberto e, se sim, processa o
		 * arquivo CSV para criar as faixas no banco de dados.
		 */
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
				parent.info("Importados " + count + " itens. Último ID: " + parent.db.getLastId());
			} catch (IOException e) {
				e.printStackTrace();
				parent.error("Erro fatal de IO ao tentar ler o CSV.");
				return;
			}
		}
	}

	/**
	 * Comando responsável por ler faixas de música no banco de dados, seja por ID
	 * ou por campo específico. O comando pode filtrar as faixas por diversos campos
	 * (como nome, artistas, popularidade, etc.) e também suporta busca por
	 * expressões regulares.
	 *
	 * <p>
	 * Este comando permite a leitura de faixas com base em um campo específico ou,
	 * se o parâmetro <code>--all</code> for fornecido, todas as faixas serão
	 * listadas. Também é possível aplicar filtros por campos como nome, artistas ou
	 * popularidade. O filtro pode ser baseado em expressões regulares quando a
	 * opção <code>--regex</code> for ativada.
	 * </p>
	 *
	 * <p>
	 * O comando valida os parâmetros fornecidos e lida com diferentes tipos de
	 * campos. Por exemplo, para o campo "ID", o parâmetro fornecido deve ser um
	 * número inteiro. Para campos como "GENRES", a busca pode ser feita com
	 * múltiplos valores.
	 * </p>
	 *
	 * <p>
	 * Em caso de erro de formatação ou falha na execução da leitura, o comando
	 * exibirá mensagens de erro apropriadas para o usuário.
	 * </p>
	 *
	 * @see Track
	 * @see TrackFilter
	 * @see Field
	 */
	@Command(name = "read", mixinStandardHelpOptions = true, description = "Ler faixa(s) por campo ou por ID.", footer = {
			"Exemplos:", "read 117 @|magenta (ler por ID 117)|@",
			"read --field=GENRES pop rock @|magenta (ler faixas com Gêneros incluindo pop e "
					+ "rock)|@",
			"read --field=TRACK_ARTISTS \"Frank Sinatra\" @|magenta (ler faixas de Frank "
					+ "Sinatra)|@",
			"read --field=NAME --regex 'You.*Gone' @|magenta (ler nomes de faixas com expressão "
					+ "regular)|@" })
	static class ReadCommand implements Runnable {
		/**
		 * Grupo de opções para escolher entre ler todas as faixas ou especificar um
		 * campo de filtro. As opções incluem:
		 * - Um campo específico para busca (como ID, NAME, GENRES, etc.).
		 * - A opção de ler todas as faixas.
		 */
		@ArgGroup(exclusive = true)
		private AllOrField allOrField = new AllOrField();

		/**
		 * Classe que contém as opções de filtro para a busca das faixas.
		 *
		 * @see Field
		 */
		static class AllOrField {
			/**
			 * Define o campo a ser usado para busca. O valor padrão é "ID".
			 *
			 * @param field O campo a ser usado para a busca.
			 */
			@Option(names = { "-f", "--field" }, description = "Campo a ser usado para busca.", defaultValue = "ID")
			Field field = ID;

			/**
			 * Se ativado, lê todas as faixas no banco de dados, sem aplicar filtros.
			 *
			 * @param all Se verdadeiro, todas as faixas serão lidas.
			 */
			@Option(names = { "-a", "--all" }, description = "Ler todos os registros.", defaultValue = "false")
			boolean all = false;
		}

		/**
		 * Se ativado, a busca por nome da faixa ou do álbum será realizada utilizando
		 * expressões regulares.
		 *
		 * @param regex Se verdadeiro, a busca será feita utilizando uma expressão
		 *              regular.
		 */
		@Option(names = { "-r",
				"--regex" }, description = "Buscar strings com expressão regular.", defaultValue = "false")
		boolean regex = false;

		/**
		 * Parâmetros para a busca. O valor depende do campo escolhido. Pode ser um
		 * único valor ou múltiplos.
		 *
		 * @param params Valor(es) utilizado(s) para a busca.
		 */
		@Parameters(paramLabel = "<valor>", description = "Valor a ser buscado.")
		String[] params;

		/**
		 * Referência para o comando pai, usado para acessar o banco de dados e exibir
		 * mensagens.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Executa a leitura das faixas do banco de dados com base no filtro fornecido.
		 * Se a busca for realizada por um campo, o comando valida os parâmetros e, em
		 * seguida, aplica o filtro de busca ao banco de dados. Caso contrário, exibe um
		 * erro.
		 *
		 * @see TrackFilter
		 * @see Track
		 */
		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			// Se a opção --all foi selecionada, exibe todas as faixas.
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

			// Se os parâmetros de busca não foram fornecidos ou são inválidos, exibe o uso
			// correto.
			if (params == null) {
				parent.out.println(new CommandLine(this).getUsageMessage());
				return;
			} else if (params.length > 1) {
				// Validações específicas de campo.
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

			// Caso o campo seja ID, realiza a busca pelo ID específico.
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
				parent.error(singleParam + " não é um número inteiro.");
				return;
			}

			// Salva o filtro de busca anterior para restaurá-lo após a execução.
			TrackFilter oldFilter = parent.db.getFilter();

			try {
				// Aplica o filtro de busca conforme o campo selecionado.
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
						// ID é um caso especial, já tratado antes.
						throw new AssertionError();
					}
				};

				// Aplica o novo filtro e exibe as faixas filtradas.
				parent.db.setFilter(newFilter);
				parent.printAllTracks();
			} catch (NumberFormatException | DateTimeParseException e) {
				parent.error("O valor não está formatado corretamente.");
			} catch (Exception e) {
				parent.warn("Por favor, tente outro termo de busca.");
			} finally {
				// Reverte o filtro para o que foi salvo anteriormente.
				parent.db.setFilter(oldFilter);
			}
		}
	}

	/**
	 * Comando responsável por deletar uma faixa de música no banco de dados a
	 * partir do seu ID.
	 *
	 * <p>
	 * Este comando recebe um ID de faixa como parâmetro e tenta deletar a faixa
	 * correspondente no banco de dados. Caso o ID não exista ou ocorra algum erro,
	 * uma mensagem de erro será exibida.
	 * </p>
	 *
	 * <p>
	 * Se o banco de dados não estiver aberto, o comando irá informar que não há
	 * nenhum arquivo aberto.
	 * </p>
	 *
	 * @see Track
	 * @see CliCommands
	 */
	@Command(name = "delete", mixinStandardHelpOptions = true, description = "Deletar uma faixa pelo ID.")
	static class DeleteCommand implements Runnable {
		/**
		 * ID da faixa a ser deletada.
		 *
		 * @param id ID da chave primária da faixa no banco de dados.
		 */
		@Parameters(paramLabel = "<ID>", description = "Chave primária da faixa.")
		int id;

		/**
		 * Comando pai que permite acessar o banco de dados e exibir mensagens.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Executa a exclusão da faixa a partir do ID fornecido.
		 *
		 * <p>
		 * Se o banco de dados estiver aberto, o comando tentará deletar a faixa
		 * correspondente ao ID fornecido. Se o ID não for encontrado, uma mensagem de
		 * erro será exibida. Caso ocorra outro erro, o comando irá relatar a falha na
		 * operação.
		 * </p>
		 */
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

	/**
	 * Comando responsável por criar uma nova faixa de música no banco de dados.
	 *
	 * <p>
	 * Este comando solicita que o usuário forneça os dados para a criação de uma
	 * nova faixa, como nome, artistas, álbum, data de lançamento, etc. O processo
	 * de criação é interativo e requer a entrada de vários campos. Caso o usuário
	 * cancele a operação ou ocorra um erro, a operação será interrompida.
	 * </p>
	 *
	 * <p>
	 * Se o banco de dados não estiver aberto, o comando irá informar que não há
	 * nenhum arquivo aberto.
	 * </p>
	 *
	 * @see Track
	 * @see CliCommands
	 */
	@Command(name = "create", mixinStandardHelpOptions = true, description = "Criar uma nova faixa.")
	static class CreateCommand implements Runnable {
		private Ansi rightPrompt = ansi().bold().fgBrightYellow();
		LineReader reader;

		/**
		 * Comando pai que permite acessar o banco de dados e exibir mensagens.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Método auxiliar para ler os dados de entrada do usuário de maneira
		 * interativa.
		 *
		 * @param prompt Mensagem que será exibida ao usuário.
		 * @return O valor fornecido pelo usuário.
		 */
		private String read(String prompt) {
			return reader.readLine(ansi().bold().fgBrightBlue().a(prompt + ": ").reset().toString(),
					this.rightPrompt.toString(), (MaskingCallback) null, null);
		}

		/**
		 * Executa a criação de uma nova faixa de música no banco de dados.
		 *
		 * <p>
		 * O comando solicita dados interativos do usuário para preencher todos os
		 * campos necessários para a criação de uma nova faixa. Caso a operação seja
		 * cancelada ou ocorra algum erro, uma mensagem de erro será exibida.
		 * </p>
		 *
		 * @see Track
		 */
		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			reader = LineReaderBuilder.builder().terminal(parent.reader.getTerminal()).build();
			rightPrompt = rightPrompt.a("[Criando ID " + (parent.db.getLastId() + 1) + "]");

			try {
				parent.suggestions.disable();
				Track t = new Track();
				t.setName(read("Name"));
				t.setTrackArtists(Arrays.stream(read("Artists").split(","))
						.map(String::trim)
						.collect(Collectors.toList()));
				t.setAlbumName(read("Album"));
				t.setAlbumReleaseDate(LocalDate.parse(read("Release Date (YYYY-MM-DD)")));
				t.setAlbumType(read("Album Type"));
				t.setGenres(Arrays.stream(read("Genres").split(","))
						.map(String::trim)
						.collect(Collectors.toList()));
				t.setExplicit(Boolean.parseBoolean(read("Explicit (true/false)")));
				t.setTrackId(read("Spotify ID (22 characters)").toCharArray());
				t.setKey(Integer.parseInt(read("Key")));
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
			} catch (Exception e) {
				parent.error("Impossível atualizar registro: " + e.getMessage());
			} finally {
				rightPrompt = ansi().bold().fgBrightYellow();
				parent.suggestions.enable();
			}
		}
	}

	/**
	 * Comando responsável por atualizar uma faixa de música existente no banco de
	 * dados.
	 *
	 * <p>
	 * Este comando permite ao usuário atualizar os campos de uma faixa existente no
	 * banco de dados, como nome, artistas, álbum, popularidade, entre outros. O
	 * comando solicita ao usuário os novos valores para os campos que deseja
	 * atualizar. Caso a operação seja cancelada ou ocorra algum erro, o processo de
	 * atualização será interrompido.
	 * </p>
	 *
	 * <p>
	 * Se o banco de dados não estiver aberto, o comando irá informar que não há
	 * nenhum arquivo aberto.
	 * </p>
	 *
	 * @see Track
	 * @see CliCommands
	 */
	@Command(name = "update", mixinStandardHelpOptions = true, description = "Atualizar uma faixa existente.")
	static class UpdateCommand implements Runnable {
		private Ansi rightPrompt = ansi().bold().fgBrightMagenta();
		LineReader reader;

		/**
		 * Campos que devem ser atualizados. Se não fornecido, todos os campos serão
		 * atualizados.
		 *
		 * @param field Lista de campos a serem atualizados.
		 */
		@Option(names = { "-f", "--field" }, description = "Campos a serem atualizados.")
		Field[] field;

		/**
		 * ID da faixa a ser atualizada.
		 *
		 * @param id ID da chave primária da faixa no banco de dados.
		 */
		@Parameters(paramLabel = "<ID>", description = "Chave primária da faixa.")
		int id;

		/**
		 * Comando pai que permite acessar o banco de dados e exibir mensagens.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Método auxiliar para ler os dados de entrada do usuário de maneira
		 * interativa.
		 *
		 * @param prompt Mensagem que será exibida ao usuário.
		 * @return O valor fornecido pelo usuário.
		 */
		private String read(String prompt) {
			return reader.readLine(ansi().bold().fgBrightBlue().a(prompt + ": ").reset().toString(),
					this.rightPrompt.toString(), (MaskingCallback) null, null);
		}

		/**
		 * Executa a atualização de uma faixa existente no banco de dados.
		 *
		 * <p>
		 * O comando solicita ao usuário os novos valores para os campos a serem
		 * atualizados. Caso a operação seja cancelada ou ocorra algum erro, o processo
		 * de atualização será interrompido.
		 * </p>
		 *
		 * @see Track
		 */
		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			reader = LineReaderBuilder.builder().terminal(parent.reader.getTerminal()).build();
			rightPrompt = rightPrompt.a("[Editando " + id + "]");

			try {
				parent.suggestions.disable();
				parent.db.update(id,
						(field == null || field.length == 0) ? updateFull(id)
								: updateFields(id, field));
			} catch (NoSuchElementException e) {
				parent.error("O ID " + id + " não existe nesse arquivo.");
			} catch (IllegalArgumentException e) {
				parent.error(e.getMessage());
			} catch (EndOfFileException e) {
				parent.warn("Atualização de registro cancelada.");
			} catch (IOException e) {
				e.printStackTrace();
				parent.error("Erro fatal de IO ao tentar criar o registro.");
			} catch (Exception e) {
				parent.error("Impossível atualizar registro: " + e.getMessage());
			} finally {
				rightPrompt = ansi().bold().fgBrightMagenta();
				parent.suggestions.enable();
			}
		}

		/**
		 * Atualiza todos os campos de uma faixa, exceto o campo ID.
		 *
		 * @param id ID da faixa a ser atualizada.
		 * @return A faixa com os campos atualizados.
		 * @throws IOException Se ocorrer um erro durante a leitura ou escrita dos
		 *                     dados.
		 */
		private Track updateFull(int id) throws IOException {
			List<Field> fieldsList = new ArrayList<>(Arrays.asList(Field.values()));
			fieldsList.remove(ID); // Remove o ID, que não pode ser editado.
			Field[] allFields = fieldsList.toArray(new Field[0]);

			return updateFields(id, allFields);
		}

		/**
		 * Atualiza os campos específicos de uma faixa.
		 *
		 * @param id     ID da faixa a ser atualizada.
		 * @param fields Campos a serem atualizados.
		 * @return A faixa com os campos atualizados.
		 * @throws IOException Se ocorrer um erro durante a leitura ou escrita dos
		 *                     dados.
		 */
		private Track updateFields(int id, Field[] fields) throws IOException {
			Track t = parent.db.read(id);

			for (Field field : Arrays.asList(fields)) {
				switch (field) {
					case NAME:
						t.setName(read("Name"));
						break;
					case TRACK_ARTISTS:
						t.setTrackArtists(Arrays.stream(read("Artists").split(","))
								.map(String::trim)
								.collect(Collectors.toList()));
						break;
					case ALBUM_NAME:
						t.setAlbumName(read("Album"));
						break;
					case ALBUM_RELEASE_DATE:
						t.setAlbumReleaseDate(LocalDate.parse(read("Release Date (YYYY-MM-DD)")));
						break;
					case ALBUM_TYPE:
						t.setAlbumType(read("Album Type"));
						break;
					case GENRES:
						t.setGenres(Arrays.stream(read("Genres").split(","))
								.map(String::trim)
								.collect(Collectors.toList()));
						break;
					case EXPLICIT:
						t.setExplicit(Boolean.parseBoolean(read("Explicit (true/false)")));
						break;
					case TRACK_ID:
						t.setTrackId(read("Spotify ID (22 characters)").toCharArray());
						break;
					case POPULARITY:
						t.setPopularity(Integer.parseInt(read("Popularity")));
						break;
					case KEY:
						t.setKey(Integer.parseInt(read("Key")));
						break;
					case DANCEABILITY:
						t.setDanceability(Float.parseFloat(read("Danceability")));
						break;
					case ENERGY:
						t.setEnergy(Float.parseFloat(read("Energy")));
						break;
					case LOUDNESS:
						t.setLoudness(Float.parseFloat(read("Loudness")));
						break;
					case TEMPO:
						t.setTempo(Float.parseFloat(read("Tempo")));
						break;
					case VALENCE:
						t.setValence(Float.parseFloat(read("Valence")));
						break;
					case ID:
						throw new IllegalArgumentException("Não é possível alterar o ID.");
				}
			}

			return t;
		}
	}

	/**
	 * Comando responsável por ordenar o banco de dados utilizando o algoritmo de
	 * ordenação externa Balanced Merge Sort (intercalação balanceada).
	 *
	 * <p>
	 * Este comando permite que o usuário ordene as faixas de música no banco de
	 * dados de acordo com a lógica de ordenação definida no algoritmo de Balanced
	 * Merge Sort. Ele oferece opções para configurar o fanout (número de elementos
	 * a serem intercalados de cada vez) e o tamanho máximo da pilha em memória,
	 * além de uma opção para ativar a saída detalhada de informações sobre o
	 * processo.
	 * </p>
	 *
	 * <p>
	 * Se o banco de dados não estiver aberto ou não houver faixas para ordenar, uma
	 * mensagem de erro ou aviso será exibida.
	 * </p>
	 *
	 * @see CliCommands
	 * @see BalancedMergeSort
	 */
	@Command(name = "sort", mixinStandardHelpOptions = true, description = "Ordenar o banco de dados.")
	static class SortCommand implements Runnable {
		/**
		 * O fanout especifica o número de elementos que são intercalados de cada vez
		 * durante a ordenação. O valor padrão é 8.
		 *
		 * @param order O número de elementos a serem intercalados de cada vez.
		 */
		@Option(names = { "-f", "--fanout" }, description = { "Fanout para o algoritmo Balanced Merge Sort.",
				"(Número de elementos mesclados de cada vez.)" }, defaultValue = "8")
		int fanout = 8;

		/**
		 * O tamanho máximo da pilha em memória a ser usada durante a ordenação.
		 * O valor padrão é 64.
		 *
		 * @param maxHeapSize O número máximo de elementos na pilha em memória.
		 */
		@Option(names = { "-n", "--num" }, description = { "Número máximo de elementos na pilha em memória",
				"a ser utilizado durante a ordenação." }, defaultValue = "64")
		int maxHeapSize = 64;

		/**
		 * Ativa ou desativa a saída detalhada durante a ordenação.
		 *
		 * @param verbose Se ativado, imprime informações detalhadas sobre o processo de
		 *                ordenação.
		 */
		@Option(names = { "-v", "--verbose" }, description = "Ativar saída detalhada.")
		boolean verbose = false;

		/**
		 * Comando pai que permite acessar o banco de dados e exibir mensagens.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Executa a ordenação do banco de dados utilizando o algoritmo Balanced Merge
		 * Sort.
		 *
		 * <p>
		 * O comando verifica se o banco de dados está aberto e se há registros a serem
		 * ordenados. Caso contrário, ele exibe uma mensagem de erro ou aviso
		 * apropriada.
		 * </p>
		 *
		 * <p>
		 * Se as condições forem atendidas, o comando cria uma instância do algoritmo de
		 * ordenação `BalancedMergeSort`, configurando-o com os parâmetros fornecidos e
		 * iniciando a ordenação.
		 * </p>
		 *
		 * @see BalancedMergeSort
		 */
		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			} else if (parent.db.getNumTracks() == 0) {
				parent.warn("Não há nenhum registro para ordenar.");
				return;
			}

			// Avisa que a operação pode demorar. Forçamos saída antes de iniciar indexação,
			// para garantir que o aviso será exibido.
			if (parent.db.isIndexed() && parent.db.getNumTracks() >= 50000) {
				parent.warn("Ordenando arquivo com muitos elementos. Será necessário reindexar. "
						+ "Isso pode demorar.");
				parent.out.flush();
			}

			try {
				// Cria instância do algoritmo BalancedMergeSort com os parâmetros fornecidos
				BalancedMergeSort sorter = new BalancedMergeSort(parent.db, fanout, maxHeapSize);
				sorter.setVerbose(verbose); // Ativa ou desativa a saída detalhada
				sorter.sort(); // Realiza a ordenação
			} catch (IllegalArgumentException e) {
				parent.error(e.getMessage()); // Exibe erro caso haja um argumento inválido
			} catch (IOException e) {
				e.printStackTrace();
				parent.error("Erro fatal de IO ao tentar ordenar o banco de dados.");
				return;
			}
		}
	}

	/**
	 * Comando responsável por gerenciar o índice do banco de dados.
	 *
	 * <p>
	 * Este comando permite ao usuário habilitar ou desabilitar diferentes tipos de
	 * índices no banco de dados, como Árvore B, Hash Dinâmico e Lista Invertida.
	 * Também é possível reindexar o arquivo inteiro ou deletar o índice atual.
	 * </p>
	 *
	 * <p>
	 * Se o banco de dados não estiver aberto, o comando irá informar que não há
	 * nenhum arquivo aberto.
	 * </p>
	 *
	 * @see CliCommands
	 */
	@Command(name = "index", mixinStandardHelpOptions = true, description = "Gerencia o índice do banco de dados.")
	static class IndexCommand implements Runnable {
		@ArgGroup(exclusive = true)
		private IndexType indexType = new IndexType();

		/**
		 * Grupo de opções para escolher o tipo de índice a ser gerenciado.
		 */
		static class IndexType {
			@Option(names = "--tree", description = "Habilita índice por Árvore B.", required = true)
			boolean btree = false;

			@Option(names = "--hash", description = "Habilita índice por Hash Dinâmico.", required = true)
			boolean hash = false;

			@Option(names = "--inverted", description = "Habilita índice por Lista Invertida.", required = true)
			boolean invertedList = false;

			@Option(names = "--disable", description = "Deleta o índice atual.", required = true)
			boolean disable = false;

			@Option(names = "--reindex", description = "Reindexa o arquivo inteiro.", required = true)
			boolean reindex = false;
		}

		/**
		 * Número máximo de filhos de uma página na Árvore B.
		 * Deve ser um número par.
		 */
		@Option(names = { "-o", "--order" }, description = { "Número máximo de filhos de uma página na Árvore B.",
				"(Deve ser par)" }, defaultValue = "16")
		int order = 16;

		/**
		 * Comando pai que permite acessar o banco de dados e exibir mensagens.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Executa a operação de gerenciamento de índice no banco de dados.
		 *
		 * <p>
		 * O comando verifica se o banco de dados está aberto e, em seguida, executa a
		 * operação de índice especificada. Caso contrário, ele exibe uma mensagem de
		 * erro apropriada.
		 * </p>
		 *
		 * <p>
		 * Se as condições forem atendidas, o comando habilita ou desabilita o índice
		 * conforme a opção selecionada.
		 * </p>
		 *
		 * @throws IllegalArgumentException Se um parâmetro inválido for recebido.
		 * @throws IllegalStateException    Se ocorrer um erro ao executar a operação.
		 * @throws IOException              Se ocorrer um erro de IO ao tentar indexar o
		 *                                  banco de dados.
		 */
		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			// Avisa que a operação pode demorar. Forçamos saída antes de iniciar indexação,
			// para garantir que o aviso será exibido.
			if ((indexType.btree || indexType.hash || indexType.invertedList)
					&& parent.db.getNumTracks() >= 50000) {
				parent.warn("Indexando arquivo com muitos elementos. Isso pode demorar.");
				parent.out.flush();
			}

			try {
				if (indexType.btree)
					parent.db.setBTreeIndex(true, order);
				else if (indexType.hash)
					// parent.db.setHashIndex(true, bucketSize);
					parent.error("Tipo de índice ainda não implementado");
				else if (indexType.invertedList)
					// parent.db.setInvertedIndex(true);
					parent.error("Tipo de índice ainda não implementado");
				else if (indexType.disable)
					parent.db.disableIndex();
				else if (indexType.reindex)
					parent.db.reindex();
				else
					parent.error("É necessário especificar exatamente uma operação. Use `index "
							+ "--help` para mais detalhes.");
			} catch (IllegalArgumentException e) {
				parent.error("Parâmetro inválido recebido: " + e.getMessage());
			} catch (IllegalStateException e) {
				parent.error("Erro ao executar operação: " + e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				parent.error("Erro fatal de IO ao tentar indexar o banco de dados.");
			}
		}
	}

	/**
	 * Classe responsável pela configuração e execução da interface de linha de
	 * comando (CLI) para o banco de dados de faixas musicais.
	 *
	 * <p>
	 * Este construtor inicializa o sistema de interface de linha de comando,
	 * configurando todos os componentes necessários, como o terminal, comandos do
	 * Picocli, sugestões de autocompletar, manipulação de teclas e outros recursos.
	 * O método `showWelcomeBanner` é utilizado para exibir um banner de boas-vindas
	 * personalizado na inicialização.
	 * </p>
	 *
	 * <p>
	 * O fluxo de execução continua em um loop, aguardando o input do usuário,
	 * executando os comandos inseridos e exibindo informações sobre o progresso ou
	 * erros durante a execução.
	 * </p>
	 *
	 * <p>
	 * O sistema acompanha recursos de autocompletar, sugestões de comandos e
	 * exibição de mensagens detalhadas durante a execução.
	 * </p>
	 *
	 * @see CliCommands
	 * @see PicocliCommandsFactory
	 * @see PicocliCommands
	 * @see SystemRegistry
	 * @see Terminal
	 * @see LineReader
	 * @see AutosuggestionWidgets
	 * @see TailTipWidgets
	 */
	public CommandLineInterface(String[] args) {
		// Inicializa o sistema de console com suporte a cores.
		AnsiConsole.systemInstall();

		try {
			// Configuração do diretório de trabalho e comandos do sistema.
			Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
			Builtins builtins = new Builtins(workDir, new ConfigurationPath(workDir.get(), workDir.get()), null);
			CliCommands commands = new CliCommands();
			PicocliCommandsFactory factory = new PicocliCommandsFactory();
			CommandLine cmd = new CommandLine(commands, factory);
			PicocliCommands picocliCommands = new PicocliCommands(cmd);
			picocliCommands.name("TrackDB commands");

			// Configuração do terminal e parser.
			Parser parser = new DefaultParser();
			if (System.getProperty("os.name").toLowerCase().contains("win"))
				// Use terminal de sistema à força no Windows.
				terminal = TerminalBuilder.builder().system(true).build();
			else
				terminal = TerminalBuilder.builder().build();

			terminal.puts(Capability.clear_screen); // Limpa a tela no terminal.
			SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
			systemRegistry.setCommandRegistries(builtins, picocliCommands);
			systemRegistry.register("help", picocliCommands);

			// Configuração do leitor de linhas e sugestões de autocompletar.
			LineReader reader = LineReaderBuilder.builder()
					.terminal(terminal)
					.completer(systemRegistry.completer())
					.parser(parser)
					.variable(LineReader.LIST_MAX, 100)
					.build();

			builtins.setLineReader(reader);
			commands.setReader(reader);
			factory.setTerminal(terminal);

			// Configuração de widgets de sugestões e dicas de comandos.
			TailTipWidgets tailtip = new TailTipWidgets(
					reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
			tailtip.enable();
			AutosuggestionWidgets suggestions = new AutosuggestionWidgets(reader);
			suggestions.enable();
			commands.setSuggestions(suggestions);

			// Mapeia teclas de atalho.
			KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
			keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

			// Exibe o banner de boas-vindas ao iniciar o programa.
			showWelcomeBanner();

			// Loop principal de execução do CLI.
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
			// Remove o suporte ao console ao encerrar.
			AnsiConsole.systemUninstall();
		}
	}

	/**
	 * Exibe um banner de boas-vindas no início do programa.
	 *
	 * <p>
	 * Este método monta e exibe um banner estilizado em duas partes: uma parte
	 * à esquerda com uma arte em ASCII e uma parte à direita com a descrição
	 * do programa, incluindo o nome do banco de dados e os autores do projeto.
	 * </p>
	 *
	 * <p>
	 * A arte ASCII e as informações de descrição são coloridas para proporcionar
	 * uma
	 * apresentação visual atraente no terminal.
	 * </p>
	 */
	public void showWelcomeBanner() {
		// Cria a arte ASCII à esquerda com cores.
		String[] bannerLeft = new String[8];
		bannerLeft[0] = "";
		bannerLeft[1] = "▄▄▄▄▄▄▄                      █      ▄▄▄▄   ▄▄▄▄▄   ";
		bannerLeft[2] = "   █     ▄ ▄▄   ▄▄▄    ▄▄▄   █   ▄  █   ▀▄ █    █  ";
		bannerLeft[3] = "   █     █▀  ▀ ▀   █  █▀  ▀  █ ▄▀   █    █ █▄▄▄▄▀  ";
		bannerLeft[4] = "   █     █     ▄▀▀▀█  █      █▀█    █    █ █    █  ";
		bannerLeft[5] = "   █     █     ▀▄▄▀█  ▀█▄▄▀  █  ▀▄  █▄▄▄▀  █▄▄▄▄▀  ";
		bannerLeft[6] = "";
		bannerLeft[7] = "══════════════════════════════════════════════════════════════════════\n";

		// Aplica a formatação de cor nas linhas da arte ASCII.
		for (int i = 1; i <= 5; ++i)
			bannerLeft[i] = new AttributedString(
					bannerLeft[i], AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
					.toAnsi();
		bannerLeft[7] = new AttributedString(
				bannerLeft[7], AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA))
				.toAnsi();

		// Cria a descrição do programa à direita.
		String[] bannerRight = new String[5];
		bannerRight[0] = "Banco de Dados";
		bannerRight[1] = "de Faixas Musicais";
		bannerRight[2] = "em formato binário";
		bannerRight[3] = "Lucca Pellegrini";
		bannerRight[4] = "Pedro Vitor Andrade";

		// Aplica a formatação de cor nas linhas de descrição.
		for (int i = 0; i <= 2; ++i)
			bannerRight[i] = new AttributedString(
					bannerRight[i], AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
					.toAnsi();
		for (int i = 3; i <= 4; ++i)
			bannerRight[i] = new AttributedString(
					bannerRight[i], AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
					.toAnsi();

		// Combina a arte ASCII com a descrição do programa.
		for (int i = 0; i <= 4; ++i)
			bannerLeft[i + 1] += bannerRight[i];

		// Exibe o banner completo no terminal.
		for (String s : bannerLeft)
			terminal.writer().println(s);
	}
}
