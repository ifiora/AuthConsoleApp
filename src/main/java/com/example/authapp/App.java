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
        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier);
        String loginUrl = "http://localhost:4200/login" +
            "?response_type=code" +
            "&client_id=desktop-client" +
            "&redirect_uri=http://localhost:12345/callback" +
            "&code_challenge=" + codeChallenge +
            "&code_challenge_method=S256";

        System.out.println("Opening browser to login...");
        Desktop.getDesktop().browse(new URI(loginUrl));

        // Guardar codeVerifier para luego intercambiar el token
        System.out.println("Generated code_verifier (keep it safe!): " + codeVerifier);

      } catch (Exception e) {
        System.err.println("Failed to open browser: " + e.getMessage());
      }
    } else {
      System.out.println("Unknown command.");
    }

    scanner.close();
  }
}
