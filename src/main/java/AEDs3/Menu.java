package AEDs3;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Scanner;

public class Menu {
	static Scanner sc = new Scanner(System.in);

	public static void main(String[] args) {
		int acao = 10;

		System.out.println(
				"Bem vindo ao menu do CRUD!!! Digite o seguintes números para a ação que desejar:");

		while (acao != 0) {
			System.out.println(
					"1. Create\n2. Read\n3. Update\n4. Delete\n5. Ordenação\n0. Parar o programa.");
			acao = sc.nextInt();

			switch (acao) {
				case 1 -> create();
				case 2 -> read();
				case 3 -> update();
				case 4 -> delete();
				case 5 -> System.err.println("Ordenação externa não implementado");
				case 0 -> System.out.println("Programa finalizado");
				default -> System.out.println("Tente outro número.");
			}
		}

		sc.close();
	}

	public static void create() {
		int acao1 = 20;
		System.out.println("Deseja criar um objeto por qual característica?");

		while (acao1 != 0) {
			System.out.println("1. AlbumReleaseDate\n2. Genres\n3. TrackArtists\n4. AlbumName\n5. "
					+ "AlbumType\n6. Name\n7. Explicit\n8. TrackId\n9. Danceability\n10. "
					+ "Energy\n11. Loudness\n12. Tempo\n13. Valence\n14. ID\n15. Key\n16. "
					+ "Popularity\n0. Sair");
			acao1 = sc.nextInt();

			switch (acao1) {
				case 1 -> {
					System.out.print("Album Release Date (YYYY-MM-DD): ");
					// track = LocalDate.parse(sc.nextLine());
					break;
				}
				case 2 -> {
					System.out.print("Genres (separados por vírgula): ");
					// track = Arrays.asList(sc.nextLine().split(", "));
					break;
				}
				case 3 -> {
					System.out.print("Track Artists (separados por vírgula): ");
					// track = Arrays.asList(sc.nextLine().split(", "));
					break;
				}
				case 4 -> {
					System.out.print("Album Name: ");
					// track = sc.nextLine();
					break;
				}
				case 5 -> {
					System.out.print("Album Type: ");
					// track = sc.nextLine();
					break;
				}
				case 6 -> {
					System.out.print("Track Name: ");
					// track = sc.nextLine();
					break;
				}
				case 7 -> {
					System.out.print("Explicit (true/false): ");
					// track = sc.nextBoolean();
					break;
				}
				case 8 -> {
					System.out.print("Track ID: ");
					// track = sc.nextLine().toCharArray();
					break;
				}
				case 9 -> {
					System.out.print("Danceability: ");
					// track =sc.nextFloat();
					break;
				}
				case 10 -> {
					System.out.print("Energy: ");
					// track =sc.nextFloat();
					break;
				}
				case 11 -> {
					System.out.print("Loudness: ");
					// track =sc.nextFloat();
					break;
				}
				case 12 -> {
					System.out.print("Tempo: ");
					// track =sc.nextFloat();
					break;
				}
				case 13 -> {
					System.out.print("Valence: ");
					// track =sc.nextFloat();
					break;
				}
				case 14 -> {
					System.out.print("ID: ");
					// track = sc.nextInt();
					break;
				}
				case 15 -> {
					System.out.print("Key: ");
					// track = sc.nextInt()
					break;
				}
				case 16 -> {
					System.out.print("Popularity: ");
					// track = sc.nextInt()
					break;
				}
				default -> System.out.println("Tente outro número.");
			}
		}
	}

	public static void read() {
		System.err.println("Read não implementado");
	}

	public static void update() {
		System.err.println("Update não implementado");
	}

	public static void delete() {
		int acao4 = 20;
		System.out.println("Deseja deletar um objeto por qual característica?");

		while (acao4 != 0) {
			System.out.println("1. AlbumReleaseDate\n2. Genres\n3. TrackArtists\n4. AlbumName\n5. "
					+ "AlbumType\n6. Name\n7. Explicit\n8. TrackId\n9. Danceability\n10. "
					+ "Energy\n11. Loudness\n12. Tempo\n13. Valence\n14. ID\n15. Key\n16. "
					+ "Popularity\n0. Sair");
			acao4 = sc.nextInt();

			switch (acao4) {
				case 1 -> {
					System.out.print("Album Release Date (YYYY-MM-DD): ");
					// track = LocalDate.parse(sc.nextLine());
					break;
				}
				case 2 -> {
					System.out.print("Genres (separados por vírgula): ");
					// track = Arrays.asList(sc.nextLine().split(", "));
					break;
				}
				case 3 -> {
					System.out.print("Track Artists (separados por vírgula): ");
					// track = Arrays.asList(sc.nextLine().split(", "));
					break;
				}
				case 4 -> {
					System.out.print("Album Name: ");
					// track = sc.nextLine();
					break;
				}
				case 5 -> {
					System.out.print("Album Type: ");
					// track = sc.nextLine();
					break;
				}
				case 6 -> {
					System.out.print("Track Name: ");
					// track = sc.nextLine();
					break;
				}
				case 7 -> {
					System.out.print("Explicit (true/false): ");
					// track = sc.nextBoolean();
					break;
				}
				case 8 -> {
					System.out.print("Track ID: ");
					// track = sc.nextLine().toCharArray();
					break;
				}
				case 9 -> {
					System.out.print("Danceability: ");
					// track =sc.nextFloat();
					break;
				}
				case 10 -> {
					System.out.print("Energy: ");
					// track =sc.nextFloat();
					break;
				}
				case 11 -> {
					System.out.print("Loudness: ");
					// track =sc.nextFloat();
					break;
				}
				case 12 -> {
					System.out.print("Tempo: ");
					// track =sc.nextFloat();
					break;
				}
				case 13 -> {
					System.out.print("Valence: ");
					// track =sc.nextFloat();
					break;
				}
				case 14 -> {
					System.out.print("ID: ");
					// track = sc.nextInt();
					break;
				}
				case 15 -> {
					System.out.print("Key: ");
					// track = sc.nextInt()
					break;
				}
				case 16 -> {
					System.out.print("Popularity: ");
					// track = sc.nextInt()
					break;
				}
				default -> System.out.println("Tente outro número.");
			}
		}
	}
}
