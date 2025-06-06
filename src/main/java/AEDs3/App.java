package AEDs3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Classe principal do programa que inicia a interface de linha de comando.
 * <p>
 * A classe {@link App} contém o método {@link #main(String[])} que é o ponto de
 * entrada
 * do aplicativo. Ao ser executada, ela cria uma nova instância da classe
 * {@link CommandLineInterface} com os argumentos fornecidos.
 */
public class App {
	protected static final String APP_NAME = "TrackDB";
	protected static final String HOME_PROPERTY = "user.home";

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

	/**
	 * Obtém o caminho de um recurso do aplicativo.
	 * <p>
	 * Este método determina o diretório apropriado para armazenar arquivos de
	 * recursos do aplicativo com base no sistema operacional. Ele cria o diretório
	 * e o arquivo se eles não existirem e tenta definir o arquivo como oculto no
	 * Windows.
	 *
	 * @param fileName Nome do arquivo de recurso.
	 * @return Caminho completo do arquivo de recurso.
	 * @throws FileNotFoundException Se o arquivo não puder ser criado.
	 */
	protected static String getAppResourcePath(String fileName) throws FileNotFoundException {
		File directory = null;
		String os = System.getProperty("os.name").toLowerCase();

		try {
			if (os.contains("win")) {
				// Windows: Usar %APPDATA%
				String appData = System.getenv("APPDATA");
				if (appData != null && !appData.isEmpty()) {
					directory = new File(appData, APP_NAME);
				}
			} else if (os.contains("mac")) {
				// macOS: Usar Library/Application Support
				directory = new File(System.getProperty(HOME_PROPERTY), "Library/Application Support/TrackDB");
			} else {
				// Linux e outros sistemas Unix-like: Usar XDG_DATA_HOME ou .local/share
				String xdgDataHome = System.getenv("XDG_DATA_HOME");
				if (xdgDataHome != null && !xdgDataHome.isEmpty()) {
					directory = new File(xdgDataHome, APP_NAME);
				} else {
					directory = new File(System.getProperty(HOME_PROPERTY), ".local/share/TrackDB");
				}
			}

			// Criar o diretório se ele não existir
			if (directory != null && !directory.exists()) {
				directory.mkdirs();
			}

			// Criar o arquivo de histórico
			File fileBaseName = new File(directory, fileName);
			if (!(fileBaseName.exists() || fileBaseName.createNewFile()))
				throw new FileNotFoundException();

			return fileBaseName.getPath();
		} catch (Exception e) {
			// Alternativa: usar o diretório home do usuário
			File fallbackFile = new File(System.getProperty(HOME_PROPERTY), "." + fileName);
			try {
				if (!(fallbackFile.exists() || fallbackFile.createNewFile()))
					throw new FileNotFoundException();

				// Tentar definir o arquivo como oculto no Windows
				if (os.contains("win")) {
					try {
						// Usar comando específico do Windows para ocultar o arquivo
						ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c",
								"attrib +h \"" + fallbackFile.getAbsolutePath() + "\"");
						pb.start();
					} catch (IOException ioException) {
						// Ignorar falhas ao definir atributos do arquivo
					}
				}
			} catch (IOException ioException) {
				throw new FileNotFoundException();
			}
			return fallbackFile.getPath();
		}
	}
}
