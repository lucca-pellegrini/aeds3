package AEDs3;

import static AEDs3.DataBase.Track.Field.*;
import static org.fusesource.jansi.Ansi.*;

import AEDs3.DataBase.BalancedMergeSort;
import AEDs3.DataBase.CSVManager;
import AEDs3.Compression.CompressionType;
import AEDs3.Compression.Compressor;
import AEDs3.Cryptography.CryptType;
import AEDs3.Cryptography.EncryptionSystem;
import AEDs3.Cryptography.Vigenere;
import AEDs3.Cryptography.VigenereKey;
import AEDs3.Cryptography.RSAHybridCryptography;
import AEDs3.Cryptography.RSAKeyGenerator;
import AEDs3.Cryptography.RSAKeyLoader;
import AEDs3.DataBase.Track.Field;
import AEDs3.DataBase.Track;
import AEDs3.DataBase.TrackDB.TrackFilter;
import AEDs3.DataBase.TrackDB;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.jline.builtins.ConfigurationPath;
import org.jline.builtins.Completers.FilesCompleter;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Candidate;
import org.jline.reader.EndOfFileException;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.Widget;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp.Capability;
import org.jline.widget.AutosuggestionWidgets;
import org.jline.widget.TailTipWidgets;
import picocli.AutoComplete;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliJLineCompleter;

/**
 * Classe principal de interface de linha de comando (CLI) para o gerenciamento
 * de arquivos TrackDB. Utiliza JLine3 para manipulação de entrada do usuário e
 * Picocli para definir os comandos da interface.
 */
public class CommandLineInterface {
	/**
	 * Terminal utilizado para interação com o usuário.
	 */
	private Terminal terminal;

	/**
	 * Indica se o banner de boas-vindas já foi exibido.
	 */
	boolean welcomeBannerShown = false;

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
			"Pressione @|magenta Ctrl-T|@ para alternar as dicas de tailtip.",
			"Pressione @|magenta Ctrl-B|@ ou digite @|magenta keybindings|@ para ver os atalhos disponíveis.",
			"" }, footer = { "",
					"Para exibir ajuda sobre um comando, digite:\n@|magenta <comando> --help|@ e pressione @|magenta <ENTER>|@\n",
					"Pressione @|magenta Ctrl-C|@ para sair." }, subcommands = { OpenCommand.class,
							CloseCommand.class, InfoCommand.class, UsageCommand.class, ImportCommand.class,
							ReadCommand.class, DeleteCommand.class, CreateCommand.class, UpdateCommand.class,
							PlayCommand.class, SortCommand.class, IndexCommand.class, CompressCommand.class,
							DecompressCommand.class, KeyBindingsCommand.class,
							EncryptCommand.class, DecryptCommand.class, KeygenCommand.class})
	static class CliCommands implements Runnable {
		/**
		 * Leitor de linha para entrada do usuário.
		 */
		LineReader reader;

		/**
		 * Saída para exibição de mensagens.
		 */
		PrintWriter out;

		/**
		 * Widgets de sugestão automática.
		 */
		AutosuggestionWidgets suggestions;

		/**
		 * Banco de dados de faixas.
		 */
		TrackDB db;

		/**
		 * Prompt exibido na linha de comando.
		 */
		String prompt;

		/**
		 * Prompt exibido à direita na linha de comando.
		 */
		String rightPrompt;

		/**
		 * Prompt padrão exibido na linha de comando.
		 */
		static final String DEFAULT_PROMPT = ansi().fgYellow().bold().a("TrackDB> ").toString();

		/**
		 * Prompt padrão exibido à direita quando nenhum arquivo está aberto.
		 */
		static final String DEFAULT_RIGHT_PROMPT = ansi().fgRed().a("[Nenhum arquivo aberto]").toString();

		/**
		 * Mensagem de erro padrão.
		 */
		static final String ERROR_PROMPT = ansi().bold().render("@|red Erro:|@ ").toString();

		/**
		 * Mensagem de aviso padrão.
		 */
		static final String WARN_PROMPT = ansi().bold().render("@|yellow Warn:|@ ").toString();

		/**
		 * Mensagem de dica padrão.
		 */
		static final String HINT_PROMPT = ansi().bold().render("@|green Dica:|@ ").toString();

		/**
		 * Mensagem de informação padrão.
		 */
		static final String INFO_PROMPT = ansi().bold().render("@|blue Info:|@ ").toString();

		/**
		 * Construtor que inicializa os prompts padrão.
		 */
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
		 * Exibe uma dica.
		 *
		 * @param msg A dica a ser exibida.
		 */
		void hint(String msg) {
			out.println(HINT_PROMPT + msg);
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

		/**
		 * Define o leitor de linha para entrada do usuário.
		 *
		 * @param reader O leitor de linha a ser configurado.
		 */
		public void setReader(LineReader reader) {
			this.reader = reader;
			this.out = reader.getTerminal().writer();
		}

		/**
		 * Define os widgets de sugestão automática.
		 *
		 * @param suggestions Os widgets de sugestão a serem configurados.
		 */
		public void setSuggestions(AutosuggestionWidgets suggestions) {
			this.suggestions = suggestions;
		}

		/**
		 * Define o banco de dados a ser utilizado.
		 *
		 * @param dbPath O caminho para o arquivo do banco de dados.
		 */
		public void setDb(String dbPath) {
			if (dbPath == null) {
				if (this.db != null) {
					try {
						this.db.close();
					} catch (IOException e) {
						this.error("Erro ao fechar o DB aberto: " + e.getMessage());
					}
				}
				this.db = null;
				this.prompt = CliCommands.DEFAULT_PROMPT; // Restaura o prompt padrão.
				this.rightPrompt = CliCommands.DEFAULT_RIGHT_PROMPT; // Restaura o prompt direito padrão.
				return;
			}

			try {
				this.db = new TrackDB(dbPath);
			} catch (IllegalStateException e) {
				this.error("Arquivo inválido ou corrompido: " + e.getMessage());
				this.warn("Provavelmente não será possível operar nesse arquivo.");
				this.warn("É possível que não se trate de um arquivo TrackDB.");
				this.hint(ansi().render("Recomendo fechar o arquivo (@|magenta Ctrl-X|@) e não proceder.").toString());
				this.prompt = ansi().bold().fgBrightRed().a(dbPath + "> ").toString();
				this.rightPrompt = ansi().bold().bgBrightRed().fgBrightDefault().a(' ').a(e.getMessage()).a(' ')
						.toString();
				return;
			} catch (FileNotFoundException e) {
				this.error("O arquivo especificado não existe: " + e.getMessage());
				return;
			} catch (IOException e) {
				this.error("Erro desconhecido ao tentar abrir arquivo: " + e.getMessage());
				return;
			}
			this.prompt = ansi().bold().fgCyan().a(dbPath + "> ").toString();
			this.rightPrompt = ansi().bgGreen().a(" CRUD ").toString();
			this.info("Arquivo carregado.");
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
		/**
		 * Comando pai que permite acessar o banco de dados e exibir mensagens.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Executa o comando para exibir a ajuda do programa.
		 */
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
		 * Opção que indica se a extensão padrão deve ser omitida ao criar um novo
		 * arquivo.
		 *
		 * <p>
		 * Quando esta opção é ativada, a extensão padrão não será adicionada
		 * automaticamente
		 * ao nome do arquivo ao criar um novo arquivo TrackDB.
		 * </p>
		 */
		@Option(names = { "--no-extension" }, description = "Não apende extensão padrão ao criar um novo arquivo.")
		private boolean omitExtension;

		/**
		 * Caminho do arquivo que será aberto ou criado, se necessário.
		 */
		@Parameters(paramLabel = "<path>", description = "Caminho para o arquivo.", completionCandidates = FileCompleter.class)
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
		 * e o prompt será alterado para refletir o arquivo aberto. Se um arquivo novo
		 * for criado, mas nenhuma extensão for especificada, uma extensão padrão será
		 * inserida.
		 * </p>
		 */
		public void run() {
			boolean paramExists = new File(param.toString()).exists();

			if (!create || paramExists) {
				if (!paramExists)
					parent.error("O arquivo não existe.");
				else
					parent.setDb(param.toString()); // Carrega o banco de dados.
			} else {
				String paramString = param.toString();
				StringBuilder nameBuilder = new StringBuilder(paramString);

				if (!(omitExtension || paramString.contains(".")))
					nameBuilder.append('.').append(TrackDB.getDefaultFileExtension());

				String name = nameBuilder.toString();
				String parentDirName = new File(name).getParent();

				if (parentDirName != null) {
					File parentDir = new File(parentDirName);
					if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs())
						throw new RuntimeException("Falha ao criar diretórios para: " + name);
				}

				parent.setDb(name);
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
			parent.setDb(null); // Fecha o banco de dados.
		}
	}

	/**
	 * Comando para comprimir o arquivo TrackDB aberto.
	 */
	@Command(name = "compress", mixinStandardHelpOptions = true, description = "Comprime o arquivo TrackDB aberto.")
	static class CompressCommand implements Runnable {
		/**
		 * Algoritmo de compressão a ser utilizado.
		 */
		@Option(names = { "-m", "--method" }, description = "Algoritmo de compressão a ser utilizado.", required = true)
		CompressionType method;

		/**
		 * Fechar e deletar arquivo após comprimir.
		 */
		@Option(names = { "-d",
				"--delete" }, description = "Fechar e deletar arquivo após comprimir", defaultValue = "false")
		boolean delete = false;

		/**
		 * Especifica um nome customizado para o arquivo comprimido.
		 */
		@Option(names = { "-n",
				"--name" }, description = "Especifica um nome customizado para o arquivo comprimido", defaultValue = "", completionCandidates = FileCompleter.class)
		String customName;

		/**
		 * Cria um backup do DB, incluindo data e hora no nome de arquivo.
		 */
		@Option(names = { "-b",
				"--backup" }, description = "Cria um backup do DB, incluindo data e hora no nome de arquivo", defaultValue = "false")
		boolean backup;

		/**
		 * Especifica arquivos externos a serem comprimidos, em vez do arquivo TrackDB
		 * atualmente aberto.
		 *
		 * <p>
		 * Esta opção permite que o usuário comprima arquivos externos, fornecendo um ou
		 * mais caminhos
		 * para os arquivos a serem comprimidos. Os arquivos especificados não devem ser
		 * arquivos TrackDB,
		 * pois esses devem ser manipulados diretamente pelo sistema.
		 * </p>
		 */
		@Option(names = { "-f",
				"--file" }, description = "Comprime um arquivo externo qualquer, e não o TrackDB aberto.", completionCandidates = FileCompleter.class)
		String[] standaloneFiles;

		/**
		 * Referência para o comando pai, utilizado para acessar a instância do banco de
		 * dados e outros recursos.
		 */
		@ParentCommand
		CliCommands parent;

		public void run() {
			if (parent.db == null && standaloneFiles == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			String[] files;
			String baseFileName;

			// Determina arquivos de origem e base do arquivo destino.
			if (standaloneFiles == null) {
				files = parent.db.listFilePaths();
				baseFileName = files[0].replaceAll("\\." + TrackDB.getDefaultFileExtension() + "$", "");
			} else {
				// Verifica se algum dos arquivos é um TrackDB.
				try {
					for (String file : standaloneFiles) {
						if (TrackDB.isTrackDB(file)) {
							parent.error("Arquivo " + file + " é um arquivo TrackDB");
							parent.error(ansi().render(
									"Não é possível comprimi-lo isoladamente com @|cyan --file|@")
									.toString());
							parent.error(ansi().render(
									"Abra-o (@|magenta Ctrl-O|@) e use o comando @|magenta,bold compress|@ diretamente")
									.toString());
							return;
						}
					}
				} catch (IOException e) {
					parent.error("Não foi possível verificar os arquivos: " + e.getMessage());
					return;
				}
				files = standaloneFiles;
				baseFileName = files[0];
			}

			// Usa nome customizado, se recebido.
			String basename = (customName.isBlank()) ? baseFileName : customName;

			StringBuilder dstBuilder = new StringBuilder(basename);
			if (backup) // Se `--backup` foi passado, apende data e hora
				dstBuilder.append('.')
						.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss")));
			dstBuilder.append('.').append(method.getExtension()); // Apende extensão
			String dst = dstBuilder.toString();

			try {
				// Calcula o tamanho total dos arquivos originais
				long originalSize = 0;
				parent.info("Empacotando e comprimindo os seguintes arquivos:");
				for (String file : files) {
					long len = new File(file).length();
					originalSize += len;
					parent.out.println(ansi().bold().a(String.format("% 6d", len / 1000)).a(" KB ").reset().fgBlue()
							.a(file.equals(files[0]) ? "(Arquivo de dados)\t" : "(Arquivo de índice)\t").bold()
							.fgGreen().a(file).a(' ').reset());
				}
				parent.out.println(ansi().bold().fgBrightYellow().a(String.format("% 6d", originalSize / 1000))
						.a(" KB ").fgBrightBlue().a("(Total)").reset());
				parent.out.flush();

				// Marca o tempo de início
				long startTime = System.currentTimeMillis();

				// Realiza a compressão
				Compressor.compress(files, dst, method);

				// Marca o tempo de fim
				long endTime = System.currentTimeMillis();

				// Calcula o tempo de execução
				long duration = endTime - startTime;
				long minutes = (duration / 1000) / 60;
				long seconds = (duration / 1000) % 60;
				long milliseconds = duration % 1000;

				// Calcula o tamanho do arquivo comprimido
				long compressedSize = new File(dst).length();

				// Calcula a taxa de compressão
				double compressionRate = (1 - ((double) compressedSize / originalSize)) * 100;

				// Exibe as informações
				parent.info(String.format("Arquivos comprimidos em: %s", ansi().bold().fgBrightYellow().a(dst)));
				parent.info(String.format("Tamanho original: %dKB", originalSize / 1000));
				parent.info(String.format("Tamanho comprimido: %dKB", compressedSize / 1000));
				parent.info(String.format("Redução de %.2f%%", compressionRate));
				parent.info(String.format("Tempo de execução: %02d:%02d.%03d", minutes, seconds, milliseconds));

				if (delete && standaloneFiles == null) {
					// Destrói os índices
					parent.db.setBTreeIndex(false);
					parent.db.setDynamicHashIndex(false);
					parent.db.setInvertedListIndex(false);

					// Fecha o arquivo de dados
					parent.db.close();
					parent.setDb(null);

					// Deleta quaisquer arquivos restantes
					for (String file : files)
						if (new File(file).exists())
							Files.delete(Paths.get(file));
				}
			} catch (FileSystemException e) {
				parent.error("Erro ao deletar alguns arquivos: " + e.getMessage());
				parent.hint("Recomendo deletá-los manualmente.");
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Erro ao comprimir " + dst);
			}
		}
	}

	/**
	 * Comando para descomprimir um arquivo TrackDB.
	 */
	@Command(name = "decompress", mixinStandardHelpOptions = true, description = "Descomprime um arquivo TrackDB.")
	static class DecompressCommand implements Runnable {
		/**
		 * Algoritmo de compressão a ser utilizado.
		 */
		@Option(names = { "-m",
				"--method" }, description = "Algoritmo de compressão a ser utilizado.", required = false)
		CompressionType method;

		/**
		 * Abrir arquivo após descomprimir.
		 */
		@Option(names = { "-o", "--open" }, description = "Abrir arquivo após descomprimi-lo", defaultValue = "false")
		boolean open;

		/**
		 * Deletar arquivo após descomprimir.
		 */
		@Option(names = { "-d",
				"--delete" }, description = "Deletar arquivo após descomprimi-lo", defaultValue = "false")
		boolean delete;

		/**
		 * Nome do arquivo a descomprimir.
		 */
		@Parameters(paramLabel = "<path>", description = "Nome do arquivo a descomprimir.", completionCandidates = FileCompleter.class)
		String path;

		/**
		 * Referência para o comando pai, utilizado para acessar a instância do banco de
		 * dados e outros recursos.
		 */
		@ParentCommand
		CliCommands parent;

		public void run() {
			if (method == null) {
				try {
					int dotIndex = path.lastIndexOf('.');
					String extension = (dotIndex == -1) ? "" : path.substring(dotIndex + 1);
					method = CompressionType.fromExtension(extension);
				} catch (NoSuchFieldException e) {
					parent.error("Impossível determinar algoritmo pelo nome do arquivo.");
					throw new IllegalArgumentException("Método de descompressão indeterminado.", e);
				}
			}

			String[] files;
			String dbPath;
			try {
				files = Compressor.decompress(path, method);
				dbPath = files[0];
				if (delete)
					Files.delete(Paths.get(path));
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Erro ao descomprimir " + path);
			}

			long totalSize = 0;
			parent.info("Descomprimi e desempacotei os seguintes arquivos:");
			for (String file : files) {
				long len = new File(file).length();
				totalSize += len;
				parent.out.println(ansi().bold().a(String.format("% 6d", len / 1000)).a(" KB ").reset().fgBlue()
						.a(file.equals(files[0]) ? "(Arquivo de dados)\t" : "(Arquivo de índice)\t").bold()
						.fgGreen().a(file).a(' ').reset());
			}
			parent.out.println(ansi().bold().fgBrightYellow().a(String.format("% 6d", totalSize / 1000))
					.a(" KB ").fgBrightBlue().a("(Total)").reset());
			parent.out.flush();

			if (open) {
				parent.setDb(dbPath);
				return;
			} else if (parent.db == null) {
				return;
			}

			try (TrackDB decompressed = new TrackDB(dbPath)) {
				if (decompressed.getUUID().equals(parent.db.getUUID())) {
					parent.warn("Recarregando arquivo.");
					parent.setDb(dbPath);
				}
			} catch (IOException e) {
				parent.error("Impossível verificar arquivo descomprimido.");
			}
		}
	}

	/**
	 * Comando para criptografar (e opcionalmente comprimir) o arquivo TrackDB
	 * aberto.
	 */
	@Command(name = "encrypt", mixinStandardHelpOptions = true, description = "Comprime o arquivo TrackDB aberto.")
	static class EncryptCommand implements Runnable {
		/**
		 * Sistema criptográfico a ser utilizado..
		 */
		@Parameters(paramLabel = "METHOD", description = "Método de criptografia a ser utilizado.")
		CryptType encryptionMethod;

		/**
		 * Algoritmo de compressão a ser utilizado.
		 */
		@Option(names = { "-c",
				"--compression-method" }, description = "Algoritmo de compressão a ser utilizado. (Padrão: nenhum)", defaultValue = "COPY")
		CompressionType compressionMethod;

		/**
		 * Fechar e deletar arquivo após criptografar.
		 */
		@Option(names = { "-d",
				"--delete" }, description = "Fechar e deletar arquivo após criptografar", defaultValue = "false")
		boolean delete = false;

		/**
		 * Especifica um nome customizado para o arquivo criptografado.
		 */
		@Option(names = { "-n",
				"--name" }, description = "Especifica um nome customizado para o arquivo criptografado", defaultValue = "", completionCandidates = FileCompleter.class)
		String customName;

		/**
		 * Cria um backup criptografado do DB, incluindo data e hora no nome de arquivo.
		 */
		@Option(names = { "-b",
				"--backup" }, description = "Cria um backup criptografado do DB, incluindo data e hora no nome de arquivo", defaultValue = "false")
		boolean backup;

		/**
		 * Especifica arquivos externos a serem criptografados, em vez do arquivo
		 * TrackDB atualmente
		 * aberto.
		 *
		 * <p>
		 * Esta opção permite que o usuário criptografe arquivos externos, fornecendo um
		 * ou mais
		 * caminhos para os arquivos a serem comprimidos. Os arquivos especificados não
		 * devem ser
		 * arquivos TrackDB, pois esses devem ser manipulados diretamente pelo sistema.
		 * </p>
		 */
		@Option(names = { "-f",
				"--file" }, description = "Criptografa um arquivo externo qualquer, e não o TrackDB aberto.", completionCandidates = FileCompleter.class)
		String[] standaloneFiles;

		/**
		 * Referência para o comando pai, utilizado para acessar a instância do banco de
		 * dados e outros recursos.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Leitor de linha para entrada do usuário.
		 */
		private LineReader reader;

		/**
		 * Prompt à direita exibido na linha de comando durante a operação.
		 */
		private Ansi rightPrompt = ansi().bold().bgBrightRed().a(" CRIPTOGRAFANDO ");

		/**
		 * Executa o comando para criptografar o arquivo TrackDB aberto ou arquivos externos.
		 *
		 * <p>
		 * Este método realiza a compressão e criptografia dos arquivos especificados. Se nenhum
		 * arquivo externo for fornecido, o arquivo TrackDB atualmente aberto será utilizado.
		 * O método suporta diferentes sistemas de criptografia, como Vigenere e RSA, e permite
		 * a especificação de um nome customizado para o arquivo de saída.
		 * </p>
		 *
		 * <p>
		 * Se a opção de deletar após criptografar for ativada, o arquivo original será removido
		 * após a operação. Em caso de erro durante a compressão ou criptografia, mensagens de
		 * erro apropriadas serão exibidas.
		 * </p>
		 *
		 * @see EncryptionSystem
		 * @see Compressor
		 */
		public void run() {
			if (parent.db == null && standaloneFiles == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			String[] files;
			String baseFileName;

			// Determina arquivos de origem e base do arquivo destino.
			if (standaloneFiles == null) {
				files = parent.db.listFilePaths();
				baseFileName = files[0].replaceAll("\\." + TrackDB.getDefaultFileExtension() + "$", "");
			} else {
				// Verifica se algum dos arquivos é um TrackDB.
				try {
					for (String file : standaloneFiles) {
						if (TrackDB.isTrackDB(file)) {
							parent.error("Arquivo " + file + " é um arquivo TrackDB");
							parent.error(ansi().render(
									"Não é possível criptografá-lo isoladamente com @|cyan --file|@")
									.toString());
							parent.error(ansi().render(
									"Abra-o (@|magenta Ctrl-O|@) e use o comando @|magenta,bold encrypt|@ diretamente")
									.toString());
							return;
						}
					}
				} catch (IOException e) {
					parent.error("Não foi possível verificar os arquivos: " + e.getMessage());
					return;
				}
				files = standaloneFiles;
				baseFileName = files[0];
			}

			// Usa nome customizado, se recebido.
			String basename = (customName.isBlank()) ? baseFileName : customName;

			StringBuilder compressedDstBuilder = new StringBuilder(basename);
			if (backup) // Se `--backup` foi passado, apende data e hora
				compressedDstBuilder.append('.')
						.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss")));
			compressedDstBuilder.append('.').append(compressionMethod.getExtension()); // Apende extensão
			String compressedDst = compressedDstBuilder.toString();
			String encryptedDst = new StringBuilder(compressedDst).append('.').append(encryptionMethod.getExtension())
					.toString();

			try {
				// Calcula o tamanho total dos arquivos originais
				long originalSize = 0;
				parent.info("Empacotando e comprimindo os seguintes arquivos:");
				for (String file : files) {
					long len = new File(file).length();
					originalSize += len;
					parent.out.println(ansi().bold().a(String.format("% 6d", len / 1000)).a(" KB ").reset().fgBlue()
							.a(file.equals(files[0]) ? "(Arquivo de dados)\t" : "(Arquivo de índice)\t").bold()
							.fgGreen().a(file).a(' ').reset());
				}
				parent.out.println(ansi().bold().fgBrightYellow().a(String.format("% 6d", originalSize / 1000))
						.a(" KB ").fgBrightBlue().a("(Total)").reset());
				parent.out.flush();

				// Marca o tempo de início
				long startTime = System.currentTimeMillis();

				// Realiza a compressão
				Compressor.compress(files, compressedDst, compressionMethod);

				// Marca o tempo de fim
				long endTime = System.currentTimeMillis();

				// Calcula o tempo de execução
				long duration = endTime - startTime;
				long minutes = (duration / 1000) / 60;
				long seconds = (duration / 1000) % 60;
				long milliseconds = duration % 1000;

				// Calcula o tamanho do arquivo comprimido
				long compressedSize = new File(compressedDst).length();

				// Calcula a taxa de compressão
				double compressionRate = (1 - ((double) compressedSize / originalSize)) * 100;

				// Exibe as informações
				parent.info(
						String.format("Arquivos comprimidos em: %s", ansi().bold().fgBrightYellow().a(compressedDst)));
				parent.info(String.format("Tamanho original: %dKB", originalSize / 1000));
				parent.info(String.format("Tamanho comprimido: %dKB", compressedSize / 1000));
				parent.info(String.format("Redução de %.2f%%", compressionRate));
				parent.info(String.format("Tempo de execução: %02d:%02d.%03d", minutes, seconds, milliseconds));

				if (delete && standaloneFiles == null) {
					// Destrói os índices
					parent.db.setBTreeIndex(false);
					parent.db.setDynamicHashIndex(false);
					parent.db.setInvertedListIndex(false);

					// Fecha o arquivo de dados
					parent.db.close();
					parent.setDb(null);

					// Deleta quaisquer arquivos restantes
					for (String file : files)
						if (new File(file).exists())
							Files.delete(Paths.get(file));
				}
			} catch (FileSystemException e) {
				parent.error("Erro ao deletar alguns arquivos: " + e.getMessage());
				parent.hint("Recomendo deletá-los manualmente.");
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Erro ao comprimir " + compressedDst);
			}

			PublicKey encryptionKey;
			EncryptionSystem system;

			switch (encryptionMethod) {
				case VIGENERE:
					reader = LineReaderBuilder.builder().terminal(parent.reader.getTerminal()).build();
					system = new Vigenere();
					encryptionKey = new VigenereKey(read("Digite a chave simétrica", true));
					break;
				case RSA:
					reader = LineReaderBuilder.builder().terminal(parent.reader.getTerminal())
							.completer(new FilesCompleter(new File("."))).build();
					system = new RSAHybridCryptography();
					String keyPath = read("Entre o caminho da chave pública", false).trim();
					try {
						encryptionKey = RSAKeyLoader.loadPublicKey(keyPath);
					} catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
						parent.error("Erro fatal ao carregar chave pública " + keyPath + ": " + e.getMessage());
						return;
					}
					break;
				default:
					parent.error("Sistema de criptografia inválido: " + encryptionMethod);
					return;
			}

			try {
				system.encrypt(compressedDst, encryptedDst, encryptionKey);
				Files.delete(Paths.get(compressedDst));
			} catch (IOException e) {
				e.printStackTrace();
				parent.error("Erro fatal de IO ao criptografar arquivo.");
			}

			parent.info(
					String.format("Arquivos criptografados em: %s", ansi().bold().fgBrightYellow().a(encryptedDst)));
		}

		/**
		 * Lê a entrada do usuário de maneira interativa.
		 *
		 * <p>
		 * Este método exibe um prompt para o usuário e lê a entrada fornecida. Se a
		 * opção de ocultar a entrada estiver ativada, os caracteres digitados serão
		 * mascarados.
		 * </p>
		 *
		 * @param prompt A mensagem a ser exibida ao usuário.
		 * @param hide   Indica se a entrada do usuário deve ser mascarada.
		 * @return A entrada fornecida pelo usuário.
		 */
		private String read(String prompt, boolean hide) {
			return reader.readLine(ansi().bold().fgBrightMagenta().a(prompt + ": ").reset().toString(),
					this.rightPrompt.toString(), hide ? '*' : null, null);
		}
	}

	/**
	 * Comando para descriptografar (e, se necessário, descomprimir) um arquivo especificado;
	 */
	@Command(name = "decrypt", mixinStandardHelpOptions = true, description = "Descriptografa um arquivo TrackDB.")
	static class DecryptCommand implements Runnable {
		/**
		 * Sistema criptográfico a ser utilizado..
		 */
		@Option(names = { "-m", "--method" }, description = "Algoritmo de criptografia a ser utilizado.")
		CryptType decryptionMethod;

		/**
		 * Algoritmo de compressão a ser utilizado.
		 */
		@Option(names = { "-c", "--compression-method" }, description = "Algoritmo de compressão a ser utilizado.")
		CompressionType compressionMethod;

		/**
		 * Abrir arquivo após descomprimir.
		 */
		@Option(names = { "-o",
				"--open" }, description = "Abrir arquivo após descriptografá-lo", defaultValue = "false")
		boolean open;

		/**
		 * Deletar arquivo após descomprimir.
		 */
		@Option(names = { "-d",
				"--delete" }, description = "Deletar arquivo após descriptografá-lo", defaultValue = "false")
		boolean delete;

		/**
		 * Nome do arquivo a descomprimir.
		 */
		@Parameters(paramLabel = "<path>", description = "Nome do arquivo a descriptografar.", completionCandidates = FileCompleter.class)
		String path;

		/**
		 * Referência para o comando pai, utilizado para acessar a instância do banco de
		 * dados e outros recursos.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Leitor de linha para entrada do usuário.
		 */
		private LineReader reader;

		/**
		 * Prompt à direita exibido na linha de comando durante a operação.
		 */
		private Ansi rightPrompt = ansi().bold().bgBrightGreen().a(" DESRIPTOGRAFANDO " + path + ' ');

		/**
		 * Executa o comando para descriptografar um arquivo TrackDB.
		 *
		 * <p>
		 * Este método realiza a descriptografia de um arquivo TrackDB utilizando o
		 * método de criptografia especificado. Se o método não for fornecido, ele tenta
		 * determinar o método de criptografia a partir da extensão do arquivo.
		 * Após a descriptografia, o arquivo pode ser opcionalmente deletado.
		 * </p>
		 *
		 * <p>
		 * Se a opção de abrir o arquivo após a descompressão for ativada, o banco de
		 * dados será carregado automaticamente. Caso contrário, o arquivo será apenas
		 * descomprimido e desempacotado.
		 * </p>
		 *
		 * <p>
		 * Em caso de erro durante a descriptografia ou descompressão, mensagens de erro
		 * apropriadas serão exibidas.
		 * </p>
		 *
		 * @see EncryptionSystem
		 * @see Compressor
		 */
		public void run() {
			reader = LineReaderBuilder.builder().terminal(parent.reader.getTerminal()).build();

			if (decryptionMethod == null) {
				try {
					int dotIndex = path.lastIndexOf('.');
					String extension = (dotIndex == -1) ? "" : path.substring(dotIndex + 1);
					decryptionMethod = CryptType.fromExtension(extension);
				} catch (NoSuchFieldException e) {
					parent.error("Impossível determinar algoritmo pelo nome do arquivo.");
					throw new IllegalArgumentException("Método de descriptografia indeterminado.", e);
				}
			}

			int lastDotIndex = path.lastIndexOf('.');
			String decryptedPath;

			if (!(lastDotIndex == -1 || lastDotIndex == 0))
				decryptedPath = path.substring(0, lastDotIndex);
			else
				decryptedPath = path + ".dec";

			PrivateKey decryptionKey;
			EncryptionSystem system;

			switch (decryptionMethod) {
				case VIGENERE:
					system = new Vigenere();
					decryptionKey = new VigenereKey(read("Digite a chave simétrica", true));
					break;
				case RSA:
					reader = LineReaderBuilder.builder().terminal(parent.reader.getTerminal())
							.completer(new FilesCompleter(new File("."))).build();
					system = new RSAHybridCryptography();
					String keyPath = read("Entre o caminho da chave privada", false).trim();
					try {
						decryptionKey = RSAKeyLoader.loadPrivateKey(keyPath);
					} catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
						parent.error("Erro fatal ao carregar chave privada " + keyPath + ": " + e.getMessage());
						return;
					}
					break;
				default:
					parent.error("Sistema de criptografia inválido: " + decryptionMethod);
					return;
			}

			try {
				system.decrypt(path, decryptedPath, decryptionKey);
				if (delete)
					Files.delete(Paths.get(path));
			} catch (IOException e) {
				e.printStackTrace();
				parent.error("Erro fatal de IO ao desriptografar arquivo.");
			}

			path = decryptedPath; // Reseta o `path` para a descompressão.

			if (compressionMethod == null) {
				try {
					int dotIndex = path.lastIndexOf('.');
					String extension = (dotIndex == -1) ? "" : path.substring(dotIndex + 1);
					compressionMethod = CompressionType.fromExtension(extension);
				} catch (NoSuchFieldException e) {
					parent.error("Impossível determinar algoritmo pelo nome do arquivo.");
					throw new IllegalArgumentException("Método de descompressão indeterminado.", e);
				}
			}

			String[] files;
			String dbPath;
			try {
				files = Compressor.decompress(path, compressionMethod);
				dbPath = files[0];
				Files.delete(Paths.get(path));
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Erro ao descomprimir " + path);
			}

			long totalSize = 0;
			parent.info("Descomprimi e desempacotei os seguintes arquivos:");
			for (String file : files) {
				long len = new File(file).length();
				totalSize += len;
				parent.out.println(ansi().bold().a(String.format("% 6d", len / 1000)).a(" KB ").reset().fgBlue()
						.a(file.equals(files[0]) ? "(Arquivo de dados)\t" : "(Arquivo de índice)\t").bold()
						.fgGreen().a(file).a(' ').reset());
			}
			parent.out.println(ansi().bold().fgBrightYellow().a(String.format("% 6d", totalSize / 1000))
					.a(" KB ").fgBrightBlue().a("(Total)").reset());
			parent.out.flush();

			if (open) {
				parent.setDb(dbPath);
				return;
			} else if (parent.db == null) {
				return;
			}

			try (TrackDB decompressed = new TrackDB(dbPath)) {
				if (decompressed.getUUID().equals(parent.db.getUUID())) {
					parent.warn("Recarregando arquivo.");
					parent.setDb(dbPath);
				}
			} catch (IOException e) {
				parent.error("Impossível verificar arquivo descomprimido.");
			}
		}

		/**
		 * Lê a entrada do usuário de maneira interativa.
		 *
		 * <p>
		 * Este método exibe um prompt para o usuário e lê a entrada fornecida. Se a
		 * opção de ocultar a entrada estiver ativada, os caracteres digitados serão
		 * mascarados.
		 * </p>
		 *
		 * @param prompt A mensagem a ser exibida ao usuário.
		 * @param hide   Indica se a entrada do usuário deve ser mascarada.
		 * @return A entrada fornecida pelo usuário.
		 */
		private String read(String prompt, boolean hide) {
			return reader.readLine(ansi().bold().fgBrightMagenta().a(prompt + ": ").reset().toString(),
					this.rightPrompt.toString(), hide ? '*' : null, null);
		}
	}

	/**
	 * Comando responsável por gerar um par de chaves pública e privada para RSA.
	 *
	 * <p>
	 * Este comando cria um par de chaves RSA e salva os arquivos de chave privada e
	 * pública nos caminhos especificados. Se os arquivos já existirem, o comando
	 * não sobrescreverá os arquivos existentes e exibirá uma mensagem de erro.
	 * </p>
	 *
	 * <p>
	 * Se o banco de dados não estiver aberto, o comando informará que não há
	 * nenhum arquivo aberto.
	 * </p>
	 *
	 * @see RSAKeyGenerator
	 */
	@Command(name = "keygen", mixinStandardHelpOptions = true, description = "Gera um par de chaves pública e privada para RSA.")
	static class KeygenCommand implements Runnable {
		/**
		 * Caminho para o arquivo privado.
		 * O caminho é passado como parâmetro ao executar o comando.
		 */
		@Parameters(paramLabel = "<path>", description = "Caminho para o arquivo privado.", completionCandidates = FileCompleter.class)
		private String param;

		/**
		 * Comando pai que permite exibir mensagens.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Executa a geração do par de chaves RSA.
		 *
		 * <p>
		 * O comando verifica se os arquivos de chave já existem e, se não existirem,
		 * gera um novo par de chaves RSA. Caso contrário, exibe uma mensagem de erro.
		 * </p>
		 */
		public void run() {
			if (new File(param).exists() || new File(param + ".pub").exists()) {
				parent.error("Recusando a criar chave com caminho de arquivo já existente.");
				return;
			}

			try {
				RSAKeyGenerator.generateKeys(param, param + ".pub");
			} catch (IOException e) {
				parent.error("Erro fatal ao gerar par de chaves.");
				return;
			}

			parent.info(String.format("Chaves geradas em: %s e %s",
				ansi().bold().fgBrightYellow().a(param).reset(),
				ansi().bold().fgBrightYellow().a(param + ".pub")));
		}
	}

	/**
	 * Comando responsável por exibir os atalhos de teclado disponíveis no programa.
	 *
	 * <p>
	 * Este comando lista todos os atalhos de teclado que podem ser utilizados
	 * na interface de linha de comando para facilitar a navegação e execução
	 * de comandos. Ele exibe uma lista organizada de atalhos, categorizados
	 * por operações gerais do programa e operações específicas sobre os dados.
	 * </p>
	 *
	 * <p>
	 * Se o banco de dados não estiver aberto, o comando ainda pode ser executado
	 * para exibir os atalhos disponíveis.
	 * </p>
	 *
	 * @see CliCommands
	 */
	@Command(name = "keybindings", mixinStandardHelpOptions = true, description = "Exibe os atalhos do programa disponíveis")
	static class KeyBindingsCommand implements Runnable {
		/**
		 * Referência para o comando pai, utilizado para acessar a instância do banco de
		 * dados e outros recursos.
		 */
		@ParentCommand
		CliCommands parent;

		public void run() {
			parent.out.println(
					ansi().a('\n')
							.fgBrightBlue().bold().a("Atalhos do Programa\n")
							.a("━━━━━━━━━━━━━━━━━━━\n").reset()
							.fgMagenta().a("Ctrl-Y\t").reset().bold().a("Exibe página de ajuda geral\n")
							.fgMagenta().a("Ctrl-T\t").reset().bold().a("Alterna dicas de tailtip\n")
							.fgMagenta().a("Ctrl-L\t").reset().bold().a("Limpa a tela, exibindo a banner").reset()
							.a(" (se possível)\n")
							.fgMagenta().a("Ctrl-K\t").reset().bold().a("Limpa a tela, sem exibir a banner\n")
							.fgMagenta().a("Ctrl-O\t").reset().bold().a("Abre um arquivo TrackDB\n")
							.fgMagenta().a("Ctrl-N\t").reset().bold().a("Cria um novo arquivo TrackDB\n")
							.fgMagenta().a("Ctrl-X\t").reset().bold().a("Fecha o arquivo TrackDB aberto\n")
							.fgMagenta().a("Ctrl-B\t").reset().bold().a("Exibe atalhos disponíveis").reset()
							.a(" (o que você está lendo agora)\n")
							.fgMagenta().a("Ctrl-Q\t").reset().bold().a("Termina o programa\n")
							.a('\n')
							.fgBrightBlue().bold().a("Atalhos do Arquivo de Dados\n")
							.a("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n").reset()
							.fgMagenta().a(" Alt-N\t").reset().bold().a("Exibe informações sobre o arquivo TrackDB\n")
							.fgMagenta().a(" Alt-M\t").reset().bold()
							.a("Importa dados de um arquivo CSV para o arquivo TrackDB\n")
							.fgMagenta().a(" Alt-C\t").reset().bold().a("Cria um novo registro").reset().fgYellow()
							.a("\t(create)\n")
							.fgMagenta().a(" Alt-R\t").reset().bold().a("Lê um registro").reset().fgYellow()
							.a("\t\t(read)\n")
							.fgMagenta().a(" Alt-U\t").reset().bold().a("Atualiza um registro").reset().fgYellow()
							.a("\t(update)\n")
							.fgMagenta().a(" Alt-D\t").reset().bold().a("Deleta um registro").reset().fgYellow()
							.a("\t(delete)\n")
							.fgMagenta().a(" Alt-P\t").reset().bold().a("Toca uma faixa no Spotify").reset()
							.a(" (se presente)\n")
							.fgMagenta().a(" Alt-S\t").reset().bold().a("Ordena o arquivo de dados\n")
							.fgMagenta().a(" Alt-I\t").reset().bold().a("Gerencia índices do arquivo\n")
							.fgMagenta().a(" Alt-H\t").reset().bold().a("Comprime o arquivo usando Huffman\n")
							.fgMagenta().a(" Alt-L\t").reset().bold().a("Comprime o arquivo usando LZW\n")
							.fgMagenta().a(" Alt-A\t").reset().bold().a("Comprime o arquivo e depois o deleta").reset()
							.a(" (archive)\n")
							.fgMagenta().a(" Alt-X\t").reset().bold().a("Extrai e abre um arquivo Huffman ou LZW\n")
							.a('\n')
							.fgBrightBlue().bold().a("Exemplo de uso\n")
							.a("━━━━━━━━━━━━━━\n").reset()
							.fgMagenta().a("Ctrl-N ").fgGreen().a("dados.db ").fgMagenta().a("<ENTER>\n")
							.reset().a("Cria um arquivo novo chamado ").fgGreen().a("dados.db\n")
							.fgMagenta().a("Alt-M ").fgGreen().a("dataset.csv ").fgMagenta().a("<ENTER>\n")
							.reset().a("Importa os registros de um arquivo CSV chamado ").fgGreen().a("dataset.csv\n")
							.fgMagenta().a("Alt-U ").fgGreen().a("NAME 10 ").fgMagenta().a("<ENTER> ").fgGreen()
							.a("40s homecoming ").fgMagenta().a("<ENTER>\n")
							.reset().a("Atualiza o ").fgGreen().a("nome ").reset().a("da faixa de ID ").fgGreen()
							.a("10 ").reset().a("para ").fgGreen().a("40s homecoming\n")
							.fgMagenta().a("Alt-R ").fgGreen().a("KMP \"home\" ").fgMagenta().a("<ENTER>\n")
							.reset().a("Busca por todas as faixas contendo a string ").fgGreen().a("home ").reset()
							.a("usando ").fgGreen().a("KMP\n")
							.fgMagenta().a("Alt-D ").fgGreen().a("99881 ").fgMagenta().a("<ENTER>\n")
							.reset().a("Deleta o registro de ID ").fgGreen().a("99881\n")
							.fgMagenta().a("Alt-S\n")
							.reset().a("Reordena o arquivo usando intercalação balanceada ").fgBlue()
							.a("(removendo lápides)\n")
							.fgMagenta().a("Alt-I ").fgGreen().a("tree ").fgMagenta().a("<ENTER>\n")
							.reset().a("Cria e habilita um índice direto utilizando ").fgGreen().a("Árvore B\n")
							.fgMagenta().a("Alt-N\n")
							.reset().a("Exibe os dados no cabeçalho do arquivo\n")
							.fgMagenta().a("Alt-A ").fgGreen().a("HUFFMAN ").fgMagenta().a("<ENTER>\n")
							.reset().a("Comprime os arquivos (incluindo índices) com ").fgGreen().a("Huffman ").reset()
							.a("e os deleta\n")
							.fgMagenta().a("Ctrl-Q\n")
							.reset().a("Fecha o programa\n"));

			parent.warn("Alguns atalhos podem não funcionar em todos os sistemas ou terminais.");
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
	 * eficiência média (maior que 50%) e vermelho para eficiência baixa (menor
	 * que 50%).
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

			Ansi tmp; // Ponteiro temporário para strings coloridas.

			// Calcula a eficiência do banco de dados.
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

			// Exibe o estado dos índices.
			tmp = ansi().bold().fgGreen().a("Forward index:\t").reset();
			tmp = (parent.db.hasPrimaryIndex()) ? parent.db.hasBTreeIndex()
					? tmp.fgBrightBlue().a("B-Tree")
					: tmp.fgBrightBlue().a("Dynamic Hash")
					: tmp.fgBrightRed().a("false");
			parent.out.println(tmp);

			// Exibe o estado dos índices invertidos
			tmp = ansi().bold().fgGreen().a("Inverted index:\t").reset();
			tmp = (parent.db.hasInvertedListIndex()) ? tmp.fgBrightBlue().a("true")
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
					: (efficiency >= 0.8) ? tmp.fgBrightYellow()
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
	 * o método {@link TrackDB#create(Track)}.
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
	 * @see TrackDB
	 */
	@Command(name = "import", mixinStandardHelpOptions = true, description = "Importar faixas de um arquivo CSV.")
	static class ImportCommand implements Runnable {
		/**
		 * Caminho para o arquivo CSV de origem a ser importado.
		 * O caminho é passado como parâmetro ao executar o comando.
		 */
		@Parameters(paramLabel = "<path>", description = "Caminho para o arquivo CSV de origem.", completionCandidates = FileCompleter.class)
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

			if (parent.db.hasInvertedListIndex()) {
				parent.warn("Importando com lista invertida habilitada. Isso pode demorar algumas horas.");
				parent.out.flush();
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
	@SuppressWarnings("CanBeFinal")
	@Command(name = "read", mixinStandardHelpOptions = true, description = "Ler faixa(s) por campo ou por ID.", footer = {
			"Exemplos:", "read 117 @|magenta (ler por ID 117)|@",
			"read --method GENRES pop rock @|magenta (ler faixas com Gêneros incluindo pop e "
					+ "rock)|@",
			"read --method TRACK_ARTISTS \"Frank Sinatra\" @|magenta (ler faixas de Frank "
					+ "Sinatra)|@",
			"read --method NAME --regex 'You.*Gone' @|magenta (ler nomes de faixas com expressão "
					+ "regular)|@" })
	static class ReadCommand implements Runnable {
		/**
		 * Grupo de opções para escolher entre ler todas as faixas ou especificar um
		 * campo de filtro. As opções incluem:
		 * - Um campo específico para busca (como ID, NAME, GENRES, etc.).
		 * - A opção de ler todas as faixas.
		 */
		@ArgGroup()
		private final ReadCommandType type = new ReadCommandType();

		/**
		 * Classe que contém as opções de filtro para a busca das faixas.
		 *
		 * @see Field
		 */
		@SuppressWarnings("CanBeFinal")
		static class ReadCommandType {
			/**
			 * Define o campo a ser usado para busca. O valor padrão é "ID".
			 */
			@Option(names = { "-m",
					"--method" }, description = "Campo ou método a ser usado para busca.", defaultValue = "ID")
			Field method = ID;

			/**
			 * Se ativado, lê todas as faixas no banco de dados, sem aplicar filtros.
			 */
			@Option(names = { "-a", "--all" }, description = "Ler todos os registros.", defaultValue = "false")
			boolean all = false;

			/**
			 * Realizar busca por listas invertidas.
			 */
			@Option(names = { "-l",
					"--list" }, description = "Realizar busca por listas invertidas.", defaultValue = "false")
			boolean invertedList = false;
		}

		/**
		 * Se ativado, a busca por nome da faixa ou do álbum será realizada utilizando
		 * expressões regulares.
		 */
		@Option(names = { "-r",
				"--regex" }, description = "Buscar strings com expressão regular.", defaultValue = "false")
		boolean regex = false;

		/**
		 * Termo de busca para lista invertida de nomes.
		 */
		@Option(names = "--name", description = "Termo de busca para lista invertida de nomes.")
		String nameList;

		/**
		 * Termo de busca para lista invertida de álbuns.
		 */
		@Option(names = "--album", description = "Termo de busca para lista invertida de álbuns.")
		String albumList;

		/**
		 * Termo de busca para lista invertida de artistas.
		 */
		@Option(names = "--artist", description = "Termo de busca para lista invertida de artistas.")
		String artistList;

		/**
		 * Parâmetros para a busca. O valor depende do campo escolhido. Pode ser um
		 * único valor ou múltiplos.
		 */
		@Parameters(paramLabel = "<valor>...", description = "Valor(es) a ser(em) buscado(s).")
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
			if (type.all) {
				TrackFilter oldFilter = parent.db.getFilter();
				try {
					parent.db.clearFilter();
					parent.printAllTracks();
				} finally {
					parent.db.setFilter(oldFilter);
				}
				return;
			}

			// Se a opção --list foi selecionada, readliza busca por listas invertidas.
			if (type.invertedList) {
				if (!parent.db.hasInvertedListIndex()) {
					parent.error("Os índices por listas invertidas não estão habilitados.");
					return;
				}

				if (nameList == null && albumList == null && artistList == null) {
					parent.error("Pelo menos um termo de busca deve ser especificado para as "
							+ "listas invertidas.");
					return;
				}

				try {
					int[] ids = parent.db.readInvertedIndexes(nameList, albumList, artistList);
					if (ids.length == 0) {
						parent.error("Nenhuma track atendendo estes termos foi encontrada.");
						return;
					}

					for (int id : ids)
						parent.printTrack(parent.db.read(id));
					return;
				} catch (IOException e) {
					e.printStackTrace();
					parent.error("Erro fatal de IO ao tentar ler os registros.");
					return;
				}
			}

			Field method = type.method;

			// Se os parâmetros de busca não foram fornecidos ou são inválidos, exibe o uso
			// correto.
			if (params == null) {
				parent.out.println(new CommandLine(this).getUsageMessage());
				return;
			} else if (params.length > 1) {
				// Validações específicas de método.
				switch (method) {
					case ID, NAME, ALBUM_NAME, ALBUM_RELEASE_DATE, ALBUM_TYPE, TRACK_ID, POPULARITY,
							KEY:
						parent.error("O método " + method + " exige exatamente um parâmetro.");
						return;
					default:
				}
			}

			String singleParam = params[0];

			// Caso o método seja ID, realiza a busca pelo ID específico.
			try {
				if (method == ID) {
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
				// Aplica o filtro de busca conforme o método selecionado.
				TrackFilter newFilter = switch (method) {
					case NAME, ALBUM_NAME -> {
						if (regex)
							yield new TrackFilter(method,
									Pattern.compile(
											singleParam, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
						else
							yield new TrackFilter(method, singleParam);
					}
					case KMP, BOYER_MOORE -> new TrackFilter(method, singleParam);
					case TRACK_ID -> {
						if (singleParam.length() != Track.getTrackIdNumChars()) {
							parent.error(method + " deve conter exatamente "
									+ Track.getTrackIdNumChars() + " caracteres.");
							throw new IllegalArgumentException();
						}
						yield new TrackFilter(method, singleParam);
					}
					case ALBUM_RELEASE_DATE -> new TrackFilter(method, LocalDate.parse(singleParam));
					case ALBUM_TYPE -> new TrackFilter(method, singleParam);
					case POPULARITY, KEY -> new TrackFilter(method, Integer.parseInt(singleParam));
					case TRACK_ARTISTS, GENRES -> new TrackFilter(method, Arrays.asList(params));
					case DANCEABILITY, ENERGY, LOUDNESS, TEMPO, VALENCE -> {
						parent.error("Não é possível buscar por valores de tipo float.");
						throw new IllegalArgumentException();
					}
					case EXPLICIT -> {
						parent.error("Não é possível buscar por valores de tipo booleano.");
						throw new IllegalArgumentException();
					}
					case ID -> // ID é um caso especial, já tratado antes.
						throw new AssertionError();
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
	 * Se o banco de dados não estiver aberto, o comando informará que não há
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
		 * erro será exibida. Caso ocorra outro erro, o comando relatará a falha na
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
	 * Se o banco de dados não estiver aberto, o comando informará que não há
	 * nenhum arquivo aberto.
	 * </p>
	 *
	 * @see Track
	 * @see CliCommands
	 */
	@Command(name = "create", mixinStandardHelpOptions = true, description = "Criar uma nova faixa.")
	static class CreateCommand implements Runnable {
		/**
		 * Prompt à direita exibido na linha de comando durante a operação.
		 */
		private Ansi rightPrompt = ansi().bold().bgBrightYellow();

		/**
		 * Leitor de linha para entrada do usuário.
		 */
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
			rightPrompt = rightPrompt.a(" Criando ID " + (parent.db.getLastId() + 1) + ' ');

			try {
				parent.suggestions.disable();
				Track t = new Track();
				t.setName(read("Name"));
				t.setTrackArtists(
						Arrays.stream(read("Artists").split(",")).map(String::trim).toList());
				t.setAlbumName(read("Album"));
				t.setAlbumReleaseDate(LocalDate.parse(read("Release Date (YYYY-MM-DD)")));
				t.setAlbumType(read("Album Type"));
				t.setGenres(Arrays.stream(read("Genres").split(",")).map(String::trim).toList());
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
				rightPrompt = ansi().bold().bgBrightYellow();
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
	 * Se o banco de dados não estiver aberto, o comando informará que não há
	 * nenhum arquivo aberto.
	 * </p>
	 *
	 * @see Track
	 * @see CliCommands
	 */
	@Command(name = "update", mixinStandardHelpOptions = true, description = "Atualizar uma faixa existente.")
	static class UpdateCommand implements Runnable {
		/**
		 * Prompt à direita exibido na linha de comando durante a operação.
		 */
		private Ansi rightPrompt = ansi().bold().bgBrightMagenta();

		/**
		 * Leitor de linha para entrada do usuário.
		 */
		LineReader reader;

		/**
		 * Campos que devem ser atualizados. Se não fornecido, todos os campos serão
		 * atualizados.
		 */
		@Option(names = { "-f", "--field" }, description = "Campos a serem atualizados.")
		Field[] selectedFields;

		/**
		 * ID da faixa a ser atualizada.
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
			rightPrompt = rightPrompt.a(" Editando " + id + " ");

			try {
				parent.suggestions.disable();
				parent.db.update(id,
						(selectedFields == null || selectedFields.length == 0) ? updateFull(id)
								: updateFields(id, selectedFields));
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
				rightPrompt = ansi().bold().bgBrightMagenta();
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
			Track t;

			if ((t = parent.db.read(id)) == null)
				throw new NoSuchElementException();

			for (Field field : fields) {
				switch (field) {
					case NAME:
						t.setName(read("Name"));
						break;
					case TRACK_ARTISTS:
						t.setTrackArtists(
								Arrays.stream(read("Artists").split(",")).map(String::trim).toList());
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
						t.setGenres(
								Arrays.stream(read("Genres").split(",")).map(String::trim).toList());
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
					case KMP, BOYER_MOORE:
						throw new IllegalArgumentException(
								"Recebi um algoritmo de busca (" + field
										+ "), mas esperava um campo de registro para atualizar.");
				}
			}

			return t;
		}
	}

	/**
	 * Comando responsável por tocar uma faixa de música no Spotify.
	 *
	 * <p>
	 * Este comando utiliza o ID da faixa para gerar um URL do Spotify e tenta
	 * abrir esse URL no navegador padrão do sistema. Se o sistema não suportar
	 * operações de desktop ou se o ID não for encontrado, uma mensagem de erro
	 * será exibida.
	 * </p>
	 *
	 * <p>
	 * Se o banco de dados não estiver aberto, o comando informará que não há
	 * nenhum arquivo aberto.
	 * </p>
	 *
	 * @see Track
	 * @see CliCommands
	 */
	@Command(name = "play", mixinStandardHelpOptions = true, description = "Tocar a faixa no Spotify.")
	static class PlayCommand implements Runnable {
		/**
		 * ID da faixa a tocar.
		 */
		@Parameters(paramLabel = "<ID>", description = "Chave primária da faixa.")
		int id;

		/**
		 * Comando pai que permite acessar o banco de dados e exibir mensagens.
		 */
		@ParentCommand
		CliCommands parent;

		/**
		 * Executa o comando para tocar a faixa no Spotify.
		 *
		 * <p>
		 * O comando verifica se o banco de dados está aberto e se o sistema suporta
		 * operações de desktop. Se as condições forem atendidas, ele tenta abrir a
		 * URL da faixa no navegador padrão.
		 * </p>
		 */
		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			try {
				if (!Desktop.isDesktopSupported()) {
					parent.error("Capabilidades de Desktop não são suportadas neste sistema.");
					return;
				}

				Track t = parent.db.read(id);
				if (t == null) {
					parent.error("Nenhuma track com esse ID foi encontrada.");
					return;
				}

				Desktop desktop = Desktop.getDesktop();
				URI uri = new URI("https://open.spotify.com/track/" + new String(t.getTrackId()));
				desktop.browse(uri);
			} catch (URISyntaxException e) {
				parent.error("Erro ao formatar URL: " + e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				parent.error("Erro fatal de IO ao tentar ler o registro.");
			}
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
	@SuppressWarnings("CanBeFinal")
	@Command(name = "sort", mixinStandardHelpOptions = true, description = "Ordenar o banco de dados.")
	static class SortCommand implements Runnable {
		/**
		 * O fanout especifica o número de elementos que são intercalados de cada vez
		 * durante a ordenação. O valor padrão é 8.
		 */
		@Option(names = { "-f", "--fanout" }, description = { "Fanout para o algoritmo Balanced Merge Sort.",
				"(Número de elementos mesclados de cada vez.)" }, defaultValue = "8")
		int fanout = 8;

		/**
		 * O tamanho máximo da pilha em memória a ser usada durante a ordenação.
		 * O valor padrão é 64.
		 */
		@Option(names = { "-n", "--num" }, description = { "Número máximo de elementos na pilha em memória",
				"a ser utilizado durante a ordenação." }, defaultValue = "64")
		int maxHeapSize = 64;

		/**
		 * Ativa ou desativa a saída detalhada durante a ordenação.
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
			if (parent.db.hasPrimaryIndex() && parent.db.getNumTracks() >= 50000) {
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
	 * Se o banco de dados não estiver aberto, o comando informará que não há
	 * nenhum arquivo aberto.
	 * </p>
	 *
	 * @see CliCommands
	 */
	@SuppressWarnings("CanBeFinal")
	@Command(name = "index", mixinStandardHelpOptions = true, description = "Gerencia o índice do banco de dados.")
	static class IndexCommand implements Runnable {
		/**
		 * Grupo de opções para escolher o tipo de índice a ser gerenciado.
		 */
		@ArgGroup()
		private final IndexType indexType = new IndexType();

		/**
		 * Classe interna que define o grupo de opções para escolher o tipo de índice a
		 * ser
		 * gerenciado.
		 */
		@SuppressWarnings("CanBeFinal")
		static class IndexType {
			/**
			 * Habilita índice por Árvore B.
			 */
			@Option(names = "--tree", description = "Habilita índice por Árvore B.", required = true)
			boolean btree = false;

			/**
			 * Habilita índice por Hash Dinâmico.
			 */
			@Option(names = "--hash", description = "Habilita índice por Hash Dinâmico.", required = true)
			boolean hash = false;

			/**
			 * Habilita índice por Lista Invertida.
			 */
			@Option(names = "--inverted", description = "Habilita índice por Lista Invertida.", required = true)
			boolean invertedList = false;

			/**
			 * Deleta o índice atual.
			 */
			@Option(names = "--disable", description = "Deleta o índice atual.", required = true)
			boolean disable = false;

			/**
			 * Reindexa o arquivo inteiro, recriando o índice primário.
			 */
			@Option(names = "--reindex", description = "Reindexa o arquivo inteiro, recriando o índice primário.", required = true)
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
		 * Número máximo de elementos de um bucket na Tabela Hash.
		 */
		@Option(names = { "-b",
				"--bucket" }, description = "Número máximo de elementos de um bucket na Tabela Hash.", defaultValue = "16")
		int bucketSize = 16;

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
		 */
		public void run() {
			if (parent.db == null) {
				parent.error("Não há nenhum arquivo aberto.");
				return;
			}

			// Avisa que a operação pode demorar. Forçamos saída antes de iniciar indexação,
			// para garantir que o aviso será exibido.
			if ((indexType.btree || indexType.hash) && parent.db.getNumTracks() >= 50000) {
				parent.warn("Indexando arquivo com muitos elementos. Isso pode demorar.");
			} else if (indexType.invertedList && parent.db.getNumTracks() >= (1 << 13)) {
				parent.warn("Muitos elementos para indexar com lista invertida!");
				parent.warn(ansi()
						.render("@|magenta,bold,italic Aumentando cache em memória temporariamente para acelerar|@")
						.toString());
			}

			parent.out.flush();

			try {
				if (indexType.btree)
					parent.db.setBTreeIndex(true, order);
				else if (indexType.hash)
					parent.db.setDynamicHashIndex(true, bucketSize);
				else if (indexType.invertedList)
					parent.db.setInvertedListIndex(true);
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
	 * @param args Argumentos de linha de comando passados para o programa.
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
		try {
			if (App.OS.contains("win")) {
				ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "chcp 65001");
				Process process = processBuilder.start();
				process.waitFor();
			}
		} catch (IOException | InterruptedException e) {
			System.err.println("Falha ao configurar caracteres unicode.");
		}

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
			LineReader reader;
			LineReaderBuilder builder = LineReaderBuilder.builder()
					.terminal(terminal)
					.completer((new ModifiedPicocliJLineCompleter(cmd.getCommandSpec())))
					.parser(parser)
					.variable(LineReader.LIST_MAX, 100);

			// Tenta habilitar histórico de comandos antes de construir.
			try {
				// Configura caminho para arquivo de histórico e limita a 10000 linhas.
				builder.variable(LineReader.HISTORY_FILE, App.getAppResourcePath("history.txt"))
						.variable(LineReader.HISTORY_FILE_SIZE, 10_000);
			} finally {
				reader = builder.build();
			}

			History history = reader.getHistory();
			if (history != null)
				history.attach(reader);

			builtins.setLineReader(reader);
			commands.setReader(reader);
			factory.setTerminal(terminal);

			// Configuração de widgets de sugestões e dicas de comandos.
			TailTipWidgets tailtip = new TailTipWidgets(
					reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);

			AutosuggestionWidgets suggestions = new AutosuggestionWidgets(reader);
			suggestions.enable();
			commands.setSuggestions(suggestions);

			// Mapeia teclas de atalho.
			KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");

			// Operações do programa

			keyMap.bind(new Reference("tailtip-toggle"), KeyMap.ctrl('t'));

			keyMap.bind((Widget) () -> {
				reader.callWidget(LineReader.CLEAR_SCREEN);
				try {
					showWelcomeBanner();
				} catch (IOException e) {
				}
				return true;
			}, KeyMap.ctrl('l'));

			keyMap.bind((Widget) () -> {
				reader.callWidget(LineReader.CLEAR_SCREEN);
				return true;
			}, KeyMap.ctrl('k'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("open ");
				reader.callWidget(LineReader.COMPLETE_WORD);
				return true;
			}, KeyMap.ctrl('o'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("open --new ");
				return true;
			}, KeyMap.ctrl('n'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("close");
				reader.callWidget(LineReader.ACCEPT_LINE);
				return true;
			}, KeyMap.ctrl('x'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("usage");
				reader.callWidget(LineReader.ACCEPT_LINE);
				return true;
			}, KeyMap.ctrl('y'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("keybindings");
				reader.callWidget(LineReader.ACCEPT_LINE);
				return true;
			}, KeyMap.ctrl('b'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("exit");
				reader.callWidget(LineReader.ACCEPT_LINE);
				return true;
			}, KeyMap.ctrl('q'));

			// Operações sobre os dados.

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("info");
				reader.callWidget(LineReader.ACCEPT_LINE);
				return true;
			}, KeyMap.alt('n'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("import ");
				return true;
			}, KeyMap.alt('m'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("create");
				reader.callWidget(LineReader.ACCEPT_LINE);
				return true;
			}, KeyMap.alt('c'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("read --method ");
				reader.callWidget(LineReader.COMPLETE_WORD);
				return true;
			}, KeyMap.alt('r'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("update --field ");
				reader.callWidget(LineReader.COMPLETE_WORD);
				return true;
			}, KeyMap.alt('u'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("delete ");
				return true;
			}, KeyMap.alt('d'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("play ");
				return true;
			}, KeyMap.alt('p'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("sort");
				reader.callWidget(LineReader.ACCEPT_LINE);
				return true;
			}, KeyMap.alt('s'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("index --");
				reader.callWidget(LineReader.COMPLETE_WORD);
				return true;
			}, KeyMap.alt('i'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("compress --method=HUFFMAN");
				reader.callWidget(LineReader.ACCEPT_LINE);
				return true;
			}, KeyMap.alt('h'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("compress --method=LZW");
				reader.callWidget(LineReader.ACCEPT_LINE);
				return true;
			}, KeyMap.alt('l'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("compress --delete --method ");
				reader.callWidget(LineReader.COMPLETE_WORD);
				return true;
			}, KeyMap.alt('a'));

			keyMap.bind((Widget) () -> {
				reader.getBuffer().clear();
				reader.getBuffer().write("decompress --open ");
				reader.callWidget(LineReader.COMPLETE_WORD);
				return true;
			}, KeyMap.alt('x'));

			// Se o terminal for grande o bastante para exibir o banner, mas menor que 45
			// linhas, desabilita tailtips por padrão. Além disso, se a altura for menor que
			// 25
			// linhas, omite tanto o banner quanto as tailtips.
			if (((terminal.getWidth() < App.MIN_TERMINAL_WIDTH || terminal.getHeight() < App.MIN_TERMINAL_HEIGHT)
					^ terminal.getHeight() < 45) || terminal.getHeight() < 25)
				tailtip.disable();
			else
				tailtip.enable();

			// Exibe o banner de boas-vindas ao iniciar o programa.
			showWelcomeBanner();

			// Abre um arquivo se passado pela linha de comando.
			if (args.length > 0) {
				commands.info("Carregando arquivo recebido na linha de comando: " + args[0]);
				commands.setDb(args[0]);
			}

			// Avisa que os parâmetros adicionais, se presentes, serão ignorados.
			if (args.length > 1) {
				StringBuilder ignoredParams = new StringBuilder("Os seguintes parâmetros adicionais serão ignorados:");
				for (int i = 1; i < args.length; ++i)
					ignoredParams.append(' ').append(args[i]);
				commands.warn(ignoredParams.toString());
			}

			// Loop principal de execução do CLI.
			String line;
			while (true) {
				try {
					systemRegistry.cleanUp();
					line = reader.readLine(commands.prompt, commands.rightPrompt, (MaskingCallback) null, null);

					// Ignora linhas em branco.
					if (line.isBlank())
						continue;

					// Verifica se o comando é desconhecido.
					String command = parser.getCommand(line);
					if (!systemRegistry.hasCommand(command)) {
						if (command.isBlank() || command.matches(".*[^a-zA-Z0-9].*"))
							commands.error("Sintaxe incorreta.");
						else
							commands.error(
									ansi().render(String.format("Comando desconhecido: @|cyan,bold,underline %s|@",
											command)).toString());

						commands.hint(ansi().render(
								"Use o comando @|magenta,bold usage|@ ou pressione @|magenta Ctrl-Y|@ para obter ajuda.")
								.toString());
						continue;
					}

					// No Windows, caminhos lidos podem usar `\` como separador.
					// Aqui, ajustamos para a forma cross-platform
					if (App.OS.contains("win"))
						line = line.replaceAll("\\\\", "/");

					systemRegistry.execute(line);
				} catch (UserInterruptException | EndOfFileException e) {
					break;
				} catch (Exception e) {
					systemRegistry.trace(e);
				}
			}

			// Tenta fechar o arquivo de dados.
			if (commands.db != null)
				commands.db.close();

			// Desabilita widgets do terminal.
			tailtip.disable();
			suggestions.disable();

			// Informa sobre o fim da execução.
			commands.info("Programa finalizado");
			if (!this.welcomeBannerShown) {
				commands.hint("A janela do seu terminal é muito pequena, então você não viu nossa banner!");
				commands.hint("Tente executar em uma janela maior na próxima vez, se quiser apreciar ;)");
			}
			commands.out.flush();
		} catch (Exception t) {
			t.printStackTrace();
		} finally {
			// Exibe o cursor novamente, caso tenha se tornado invisível.
			terminal.puts(Capability.cursor_visible);
			terminal.puts(Capability.cursor_normal);
			// Remove o suporte ao console ao encerrar.
			AnsiConsole.systemUninstall();
			System.exit(0);
		}
	}

	/**
	 * Exibe um banner de boas-vindas no início do programa.
	 *
	 * <p>
	 * Este método monta e exibe um banner estilizado em duas partes: uma parte
	 * à esquerda com uma arte em ANSI e uma parte à direita com a descrição
	 * do programa, incluindo o nome do banco de dados e os autores do projeto.
	 * </p>
	 *
	 * <p>
	 * A arte ANSI e as informações de descrição são coloridas para proporcionar
	 * uma
	 * apresentação visual atraente no terminal.
	 * </p>
	 *
	 * @throws IOException Se ocorrer um erro ao ler o arquivo de arte ANSI.
	 */
	public void showWelcomeBanner() throws IOException {
		// Não exibe o banner se o terminal for muito pequeno.
		if (terminal.getWidth() < App.MIN_TERMINAL_WIDTH || terminal.getHeight() < App.MIN_TERMINAL_HEIGHT)
			return;

		// Se um banner não foi exibido, escolhe dentre os dois primeiros.
		final int bannerNum = App.RANDOM.nextInt(this.welcomeBannerShown ? 4 : 2);

		// Lê a arte ANSI do diretório de recursos.
		String[] bannerLeft;
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("banner" + bannerNum + ".ans");
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(inputStreamReader)) {
			bannerLeft = reader.lines().toArray(String[]::new);
		}

		// Computa versão e revisão atual do programa.
		String ver = new App().getVersion();
		int revision;
		try {
			revision = Integer.parseInt(ver.replaceAll(".*\\.r", ""));
		} catch (NumberFormatException e) {
			revision = -1;
		}

		List<String> txt = new ArrayList<>();
		txt.add("     ");
		txt.add("     Banco de Dados");
		txt.add("     de Faixas Musicais");
		txt.add("     em formato binário");
		txt.add("     ");
		txt.add("     " + ver.replaceFirst("^v", "Versão ").replaceAll("\\.r\\d+$", ""));
		txt.add("     " + (revision > 0 ? revision + "ª revisão" : ""));
		txt.add("     ");
		txt.add("     \033]8;;https://www.apache.org/licenses/LICENSE-2.0.html\033\\Copyright © 2025\033]8;;\033\\");
		txt.add("     \033]8;;https://github.com/lucca-pellegrini\033\\Lucca Pellegrini\033]8;;\033\\");
		txt.add("     \033]8;;https://github.com/Pedro0826\033\\Pedro Vitor Andrade\033]8;;\033\\");
		txt.add("     ");
		txt.add("     Algoritmos e Estruturas de Dados III");
		txt.add("     Engenharia de Computação");
		txt.add("     PUC Minas");

		// Cria a descrição do programa à direita.
		String[] bannerRight = txt.toArray(String[]::new);

		// Aplica a formatação de cor nas linhas de descrição.
		for (int i = 1; i <= 3; ++i)
			bannerRight[i] = ansi().fgBrightYellow().bold().a(bannerRight[i]).toString();
		for (int i = 5; i <= 6; ++i)
			bannerRight[i] = ansi().fgYellow().a(bannerRight[i]).toString();
		bannerRight[8] = ansi().fgBlue().bold().a(bannerRight[8]).toString();
		for (int i = 9; i <= 10; ++i)
			bannerRight[i] = ansi().fgBrightBlue().bold().a(bannerRight[i]).toString();
		for (int i = 12; i <= 14; ++i)
			bannerRight[i] = ansi().fgBlue().a(bannerRight[i]).toString();

		// Combina a arte ANSI com a descrição do programa.
		for (int i = 0; i < bannerRight.length; ++i)
			bannerLeft[i + 1] += bannerRight[i];

		int maxCursorOffset = 0;

		// Exibe o banner completo no terminal.
		for (String s : bannerLeft) {
			terminal.writer().println(s);
			maxCursorOffset = Math.max(maxCursorOffset, s.replaceAll("\u001B\\[[;\\d]*m", "").length() - 42);
		}

		// Exibe a linha separadora.
		StringBuilder sep = new StringBuilder(ansi().fgBrightMagenta().toString());
		for (int i = 0; i < maxCursorOffset; ++i)
			sep.append("━");
		sep.append(ansi().reset().a('\n'));
		terminal.writer().println(sep.toString());

		// Salva que a banner foi exibida.
		this.welcomeBannerShown = true;
	}

	/**
	 * Classe que implementa um iterador para completar nomes de arquivos.
	 */
	static class FileCompleter implements Iterable<String> {

		/**
		 * Obtém o diretório a partir de um caminho fornecido.
		 *
		 * @param d Caminho do diretório.
		 * @return O diretório correspondente.
		 */
		public static File getDir(String d) {
			File f = new File(d);
			if (f.isDirectory())
				return f;
			String sep = File.separator;
			String pfx = (d.startsWith(sep) || d.startsWith("." + sep)) ? d.substring(0, d.indexOf(sep) + 1) : "";
			d = d.substring(pfx.length());
			int lastSlash = d.lastIndexOf(sep);
			if (lastSlash >= 0) {
				f = new File(pfx + d.substring(0, lastSlash));
				if (f.isDirectory())
					return f;
			}
			f = new File(pfx);
			if (f.isDirectory())
				return f;
			return new File(".");
		}

		/**
		 * Retorna um iterador para os candidatos de autocompletar.
		 *
		 * @return Um iterador de strings.
		 */
		@Override
		public Iterator<String> iterator() {
			ParsedLine line = ModifiedPicocliJLineCompleter.parsedLineThreadLocal.get();
			String userInput = line == null ? "" : line.words().get(line.wordIndex());
			File dir = getDir(userInput);
			List<String> candidates = new LinkedList<>();
			String left, middle;
			if (userInput.equals(".") || dir.getPath().equals(".") && !userInput.startsWith("." + File.separator)) {
				left = "";
				middle = "";
			} else {
				left = dir.getPath();
				middle = dir.getPath().endsWith(File.separator) ? "" : File.separator;
			}
			for (File file : dir.listFiles()) {
				candidates.add(left + middle + file.getName() + (file.isDirectory() ? File.separator : ""));
			}
			return candidates.iterator();
		}
	}

	/**
	 * Classe modificada para completar comandos usando Picocli.
	 */
	private static class ModifiedPicocliJLineCompleter extends PicocliJLineCompleter {

		/**
		 * Armazena a linha analisada atual.
		 */
		public static ThreadLocal<ParsedLine> parsedLineThreadLocal = new ThreadLocal<>();

		/**
		 * Especificação sobrescrita do comando.
		 */
		private final CommandSpec shadowedSpec;

		/**
		 * Construtor que inicializa o completer com a especificação de comando.
		 *
		 * @param spec Especificação do comando.
		 */
		public ModifiedPicocliJLineCompleter(CommandSpec spec) {
			super(spec);
			this.shadowedSpec = spec;
		}

		/**
		 * Completa os comandos com base na entrada do usuário.
		 *
		 * @param reader     Leitor de linha.
		 * @param line       Linha analisada.
		 * @param candidates Lista de candidatos para autocompletar.
		 */
		@Override
		public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
			parsedLineThreadLocal.set(line);
			try {
				String[] words = new String[line.words().size()];
				words = line.words().toArray(words);
				List<CharSequence> cs = new ArrayList<>();
				AutoComplete.complete(shadowedSpec, words, line.wordIndex(), 0, line.cursor(), cs);
				for (CharSequence c : cs) {
					String s = (String) c;
					if (s.endsWith(File.separator) && new File(s).isDirectory()) {
						candidates.add(new Candidate(s, s, null, null, null, null, false));
					} else
						candidates.add(new Candidate(s));
				}
			} finally {
				parsedLineThreadLocal.remove();
			}
		}
	}
}
