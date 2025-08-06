package com.example.authapp;

import java.util.Scanner;
import java.awt.Desktop;
import java.net.URI;

public class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Auth Console App.");
        System.out.print("Type 'login' to authenticate: ");

        String input = scanner.nextLine();
        if ("login".equalsIgnoreCase(input.trim())) {
            try {
                String loginUrl = "http://localhost:4200/login"; // Cambiar por la URL real
                System.out.println("Opening browser to: " + loginUrl);
                Desktop.getDesktop().browse(new URI(loginUrl));
            } catch (Exception e) {
                System.err.println("Failed to open browser: " + e.getMessage());
            }
        } else {
            System.out.println("Unknown command.");
        }
        scanner.close();
    }
}
