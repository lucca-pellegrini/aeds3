package com.verticordia.AEDs3;

import java.util.Scanner;

public class Menu {
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		int acao = 10;

		System.out.println("Bem vindo ao menu do CRUD!!! Digite o seguintes números para a ação que desejar:");

		while (acao != 0) {
			System.out.println("1. Create\n2. Read\n3. Update\n4. Delete\n0. Parar o programa.");
			acao = sc.nextInt();

			switch (acao) {
				case 1 -> System.err.println("Create não implementado");
				case 2 -> System.err.println("Read não implementado");
				case 3 -> System.err.println("Update não implementado");
				case 4 -> System.err.println("Delete não implementado");
				case 0 -> System.out.println("Programa finalizado");
				default -> System.out.println("Tente outro número.");
			}
		}

		sc.close();
	}
}
