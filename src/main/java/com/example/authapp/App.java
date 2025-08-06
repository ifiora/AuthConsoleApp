package com.example.authapp;

import java.io.*;
import java.net.*;
import java.awt.Desktop;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class App {
  private static final String CHALLENGE_FILE = "challenge.txt";

  public static void main(String[] args) {

    System.out.println("== Auth Desktop App ==");

    System.out.println("Args recibidos:");
    for (String arg : args) {
      System.out.println(arg);
    }

    if (args.length == 0) {
      // Sin argumentos → iniciar login
      startLoginFlow();
    } else {
      // Con argumentos → buscar code=... y completar login
      for (String arg : args) {
        if (arg.startsWith("jewel://code=")) {
          String code = arg.substring("jewel://code=".length());
          try {
            completeLoginFlow(code);
          } catch (Exception e) {
            System.err.println("Error while completing login: " + e.getMessage());
          }
        }
      }
    }

    System.out.println("Presioná Enter para cerrar...");
    try {
      System.in.read();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void startLoginFlow() {
    try {
      String verifier = PkceUtil.generateCodeVerifier();
      String challenge = PkceUtil.generateCodeChallenge(verifier);

      // Guardar el verifier en un archivo temporal
      Files.writeString(Path.of(CHALLENGE_FILE), verifier);
      System.out.println("Generated challenge: " + challenge);
      System.out.println("Opening browser...");

      // Abrir navegador a Angular app
      String loginUrl = "http://localhost:4200/jewel-login/" + URLEncoder.encode(challenge, "UTF-8");
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(new URI(loginUrl));
      } else {
        System.out.println("Open this URL manually:\n" + loginUrl);
      }

    } catch (Exception e) {
      System.err.println("Error during login flow: " + e.getMessage());
    }
  }

  private static void completeLoginFlow(String code) throws Exception {
    // Leer el verifier previamente guardado

    String jarPath = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
    File file = new File(jarPath, CHALLENGE_FILE);

    String verifier = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

    System.out.println("Sending POST to backend...");

    // Crear cuerpo JSON
    String json = String.format("{\"code\":\"%s\",\"verifier\":\"%s\"}", code, verifier);

    // Enviar POST a backend
    URL url = new URL("http://localhost:3000/v1/auth/jewelAuthorize");
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestMethod("POST");
    con.setDoOutput(true);
    con.setRequestProperty("Content-Type", "application/json");

    try (OutputStream os = con.getOutputStream()) {
      byte[] input = json.getBytes("utf-8");
      os.write(input, 0, input.length);
    }

    int status = con.getResponseCode();
    System.out.println("Response status: " + status);

    if (status == 200) {
      try (BufferedReader br = new BufferedReader(
          new InputStreamReader(con.getInputStream(), "utf-8"))) {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
          response.append(line.trim());
        }
        System.out.println("Response body: " + response);
      }
    } else {
      System.out.println("Failed to authorize. HTTP status: " + status);
    }

    con.disconnect();
  }
}
