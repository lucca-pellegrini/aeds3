package AEDs3;

import AEDs3.DataBase.Track;
import AEDs3.DataBase.TrackDB;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

//GPT foi usado para formatação e otimização do código.
public class Menu {
	static Scanner sc = new Scanner(System.in);

	static TrackDB db;

	public static void main(String[] args) throws Exception {
		int acao = 10;
		db = new TrackDB("tracks.db");

		System.out.println("\n\033[1;34m" +
            "========================================\n" +
            "      BEM-VINDO AO CRUD DE MÚSICAS!     \n" +
            "========================================\033[0m");

		while (acao != 0) {
			System.out.println("\n\033[1;36m" +
                "Escolha uma ação:\n" +
                "----------------------------------------\033[0m");
        System.out.println("\033[1;33m" + 
                "1. Criar uma nova música\n" +
                "2. Ler músicas cadastradas\n" +
                "3. Atualizar uma música\n" +
                "4. Deletar uma música\n" +
                "5. Ordenação externa\n" +
                "0. Sair do programa\033[0m");

        System.out.print("\n\033[1;32mDigite sua opção: \033[0m");
        acao = sc.nextInt();

			switch (acao) {
				case 1 -> create();
				//case 2 -> read();
				case 3 -> update();
				case 4 -> delete();
				case 5 -> System.err.println("\033[1;31mOrdenação externa não implementada\033[0m\"");
				case 0 -> System.out.println("\033[1;32mPrograma finalizado. Até logo!\033[0m");
				default -> System.out.println("\033[1;31mOpção inválida! Tente novamente.\033[0m");
			}
		}

		sc.close();
	}

	public static void create() throws Exception {
		int cont = 1;
		
		while (cont == 1) {
			sc.nextLine(); // Evita problemas com buffer
			Track nova = new Track();
			System.out.println("\n\033[1;34m=== Criando uma Nova Track ===\033[0m");
	
			LocalDate releaseDate = null;
			while (releaseDate == null) {
				System.out.print("Album Release Date (YYYY-MM-DD): ");
				String dataEntrada = sc.nextLine();
				try {
					releaseDate = LocalDate.parse(dataEntrada);
				} catch (DateTimeParseException e) {
					System.out.println("Formato inválido! Use YYYY-MM-DD.");
				}
			}
			nova.setAlbumReleaseDate(releaseDate);
	
			nova.setGenres(lerLista("Genres (separados por vírgula): "));
			nova.setTrackArtists(lerLista("Track Artists (separados por vírgula): "));
	
			System.out.print("Album Name: ");
			nova.setAlbumName(sc.nextLine());
	
			System.out.print("Album Type: ");
			nova.setAlbumType(sc.nextLine());
	
			System.out.print("Track Name: ");
			nova.setName(sc.nextLine());
	
			Boolean explicit = null;
			while (explicit == null) {
				System.out.print("Explicit (true/false): ");
				String input = sc.next().trim().toLowerCase();
				if (input.equals("true") || input.equals("false")) {
					explicit = Boolean.parseBoolean(input);
				} else {
					System.out.println("Entrada inválida! Digite 'true' ou 'false'.");
				}
			}
			nova.setExplicit(explicit);
			sc.nextLine(); 
	
			System.out.print("Track ID: ");
			char[] trackI = sc.nextLine().toCharArray();
			while (trackI.length != Track.getTrackIdNumChars()) {
				System.out.println("Tamanho do track ID incorreto!");
				System.out.print("Track ID: ");
				trackI = sc.nextLine().toCharArray();
			}
			nova.setTrackId(trackI);
	
			nova.setDanceability(lerFloat("Danceability: "));
			nova.setEnergy(lerFloat("Energy: "));
			nova.setLoudness(lerFloat("Loudness: "));
			nova.setTempo(lerFloat("Tempo: "));
			nova.setValence(lerFloat("Valence: "));
	
			nova.setId(lerInt("ID: "));
			nova.setKey(lerInt("Key: "));
			nova.setPopularity(lerInt("Popularity: "));
	
			db.create(nova);
	
			System.out.print("Digite 1 para adicionar outra track ou outro qualquer número para parar: ");
			cont = sc.nextInt();
		}
	}
	
	/*public static void read() throws Exception {
		System.out.println("Por qual característica você irá querer para fazer a leitura?");
		int acao3 = 20;
	
		while (acao3 != 0) {
			System.out.println("1. AlbumReleaseDate\n2. Genres\n3. TrackArtists\n4. AlbumName\n5. " +
					"AlbumType\n6. Name\n7. Explicit\n8. TrackId\n9. Danceability\n10. " +
					"Energy\n11. Loudness\n12. Tempo\n13. Valence\n14. ID\n15. Key\n16. " +
					"Popularity\n0. Sair");
			
			acao3 = sc.nextInt();
			if (acao3 == 0) break;
	
			Track track = null;
	
			switch (acao3) {
				case 1 -> {
					LocalDate releaseDate = null;
					while (releaseDate == null) {
						System.out.print("Album Release Date (YYYY-MM-DD): ");
						String dataEntrada = sc.nextLine();
						try {
							releaseDate = LocalDate.parse(dataEntrada);
						} catch (DateTimeParseException e) {
							System.out.println("Formato inválido! Use o formato YYYY-MM-DD.");
						}
					}
					track = db.read(TrackField.ALBUM_RELEASE_DATE, releaseDate);
				}
				case 2 -> track = db.read(TrackField.GENRES, lerLista("Genres (separados por vírgula):"));
				case 3 -> track = db.read(TrackField.ARTISTS, lerLista("Track Artists (separados por vírgula):"));
				case 4 -> {
					System.out.print("Album Name: ");
					String albumName = sc.nextLine();
					track = db.read(TrackField.ALBUM_NAME, albumName);
				}
				case 5 -> {
					System.out.print("Album Type: ");
					String albumType = sc.nextLine();
					track = db.read(TrackField.ALBUM_TYPE, albumType);
				}
				case 6 -> {
					System.out.print("Track Name: ");
					String trackName = sc.nextLine();
					track = db.read(TrackField.TRACK_NAME, trackName);
				}
				case 7 -> {
					Boolean explicit = null;
					while (explicit == null) {
						System.out.print("Explicit (true/false): ");
						String input = sc.next().trim().toLowerCase();
						if (input.equals("true") || input.equals("false")) {
							explicit = Boolean.parseBoolean(input);
						} else {
							System.out.println("Entrada inválida! Digite 'true' ou 'false'.");
						}
					}
					track = db.read(TrackField.EXPLICIT, explicit);
					sc.nextLine();
				}
				case 8 -> {
					char[] id = sc.nextLine().toCharArray();
					if (id.length == Track.getTrackIdNumChars()) {
						track = db.read(TrackField.TRACK_ID, id);
					} else {
						System.out.println("Tamanho do track ID incorreto!");
					}
				}
				case 9 -> track = db.read(TrackField.DANCEABILITY, lerFloat("Danceability:"));
				case 10 -> track = db.read(TrackField.ENERGY, lerFloat("Energy:"));
				case 11 -> track = db.read(TrackField.LOUDNESS, lerFloat("Loudness:"));
				case 12 -> track = db.read(TrackField.TEMPO, lerFloat("Tempo:"));
				case 13 -> track = db.read(TrackField.VALENCE, lerFloat("Valence:"));
				case 14 -> track = db.read(TrackField.ID, lerInt("ID:"));
				case 15 -> track = db.read(TrackField.KEY, lerInt("Key:"));
				case 16 -> track = db.read(TrackField.POPULARITY, lerInt("Popularity:"));
				default -> System.out.println("Opção inválida. Tente novamente.");
			}
	
			if (track != null) {
				System.out.println("Track encontrada: " + track);
			} else {
				System.out.println("Nenhuma track encontrada com o critério informado.");
			}
		}
	}*/

	public static void update() throws Exception {
		System.out.println("Qual ID você deseja atualizar?");
		int idUpdt = sc.nextInt();
		sc.nextLine(); // Consumir quebra de linha
	
		Track t = db.read(idUpdt);
	
		int acao3 = 20;
	
		while (acao3 != 0) {
			System.out.println("1. Album Release Date\n2. Genres\n3. Track Artists\n4. Album Name\n5. "
					+ "Album Type\n6. Name\n7. Explicit\n8. Track ID\n9. Danceability\n10. "
					+ "Energy\n11. Loudness\n12. Tempo\n13. Valence\n14. ID\n15. Key\n16. "
					+ "Popularity\n0. Sair");
			acao3 = sc.nextInt();
			sc.nextLine(); // Consumir quebra de linha
	
			switch (acao3) {
				case 1 -> {
					LocalDate releaseDate = null;
					while (releaseDate == null) {
						System.out.print("Album Release Date (YYYY-MM-DD): ");
						String dataEntrada = sc.nextLine();
						try {
							releaseDate = LocalDate.parse(dataEntrada);
						} catch (DateTimeParseException e) {
							System.out.println("Formato inválido! Use o formato YYYY-MM-DD.");
						}
					}
					t.setAlbumReleaseDate(releaseDate);
				}
				case 2 -> {
					t.setGenres(lerLista("Genres (separados por vírgula): "));
				}
				case 3 -> {
					t.setTrackArtists(lerLista("Track Artists (separados por vírgula): "));
				}
				case 4 -> {
					System.out.print("Album Name: ");
					t.setAlbumName(sc.nextLine());
				}
				case 5 -> {
					System.out.print("Album Type: ");
					t.setAlbumType(sc.nextLine());
				}
				case 6 -> {
					System.out.print("Track Name: ");
					t.setName(sc.nextLine());
				}
				case 7 -> {
					Boolean explicit = null;
					while (explicit == null) {
					System.out.print("Explicit (true/false): ");
					String input = sc.next().trim().toLowerCase();
					if (input.equals("true") || input.equals("false")) {
					explicit = Boolean.parseBoolean(input);
					} else {
					System.out.println("Entrada inválida! Digite 'true' ou 'false'.");
					}
					}
					t.setExplicit(explicit);
					sc.nextLine();
				}
				case 8 -> {
					System.out.print("Track ID: ");
					char[] trackId = sc.nextLine().toCharArray();
					if (trackId.length == Track.getTrackIdNumChars()) {
						t.setTrackId(trackId);
					} else {
						System.out.println("Tamanho do track ID incorreto!");
					}
				}
				case 9 -> {
					t.setDanceability(lerFloat("Danceability: "));
				}
				case 10 -> {
					t.setEnergy(lerFloat("Energy: "));
				}
				case 11 -> {
					t.setLoudness(lerFloat("Loudness: "));
				}
				case 12 -> {
					t.setTempo(lerFloat("Tempo: "));
				}
				case 13 -> {
					t.setValence(lerFloat("Valence: "));
				}
				case 14 -> {
					t.setId(lerInt("ID: "));
				}
				case 15 -> {
					t.setKey(lerInt("Key: "));
				}
				case 16 -> {
					t.setPopularity(lerInt("Popularity: "));
				}
				case 0 -> System.out.println("Saindo da atualização...");
				default -> System.out.println("Tente outro número.");
			}
		}
		
		// Atualiza os dados no banco de dados apenas uma vez, no final
		db.update(idUpdt, t);
	}

	public static void delete() throws Exception{
		
		int acao4 = 20;
		
		while (acao4 != 0) {
			System.out.println("Qual ID voce deseja deletar? Digite 0 para parar o delete.");
			acao4 = sc.nextInt();
			
			db.delete(acao4);
		}
	}

private static List<String> lerLista(String mensagem) {
    System.out.print(mensagem);
    String entrada = sc.nextLine();
    return Arrays.stream(entrada.split(","))
                 .map(String::trim)
                 .collect(Collectors.toList());
}

private static Float lerFloat(String mensagem) {
    Float valor = null;
    while (valor == null) {
        try {
            System.out.print(mensagem);
            valor = sc.nextFloat();
        } catch (InputMismatchException e) {
            System.out.println("Entrada inválida! Digite um número válido.");
            sc.nextLine(); 
        }
    }
    sc.nextLine(); 
    return valor;
}

private static Integer lerInt(String mensagem) {
    Integer valor = null;
    while (valor == null) {
        try {
            System.out.print(mensagem);
            valor = sc.nextInt();
        } catch (InputMismatchException e) {
            System.out.println("Entrada inválida! Digite um número válido.");
            sc.nextLine(); 
        }
    }
    sc.nextLine(); 
    return valor;
}

}
