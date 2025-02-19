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

			if (acao == 1) {
				// codigo do create
			} else if (acao == 2) {
				// codigo do read
			} else if (acao == 3) {
				// codigo do update
			} else if (acao == 4) {
				// codigo do delete
			} else if (acao == 0) {
				System.out.println("Programa finalizado!");
			} else {
				System.out.println("Tente outro número.");
			}
		}

		sc.close();
	}
}
