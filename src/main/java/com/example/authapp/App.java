package com.example.authapp;

import HttpConnection.AsyncHttpClient;
import HttpConnection.HttpClient;
import HttpConnection.HttpResponseHandler;
import HttpConnection.RequestParams;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.net.*;
import java.awt.Desktop;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static spark.Spark.*;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;

public class App {
  private static final String CHALLENGE_FILE = "out\\artifacts\\AuthConsoleApp_jar\\challenge.txt";

  private static final String URL_LOGIN = "https://eeg-portal.soplo.com.ar/jewel-login/";
  private static final String URL_AUTHORIZATION = "https://eeg-portal.soplo.com.ar/v1/auth/jewelAuthorize";

  public static void main(String[] args) {
    // We make the SparkJava logger less verbose...
    Log.setLog(new StdErrLog());
    Log.getRootLogger().setDebugEnabled(false);

    if (args.length == 0) {
      System.out.println("No arguments. Initializing HTTP server and login flow...");

      startHttpServer();
      launchLoginFlow();
    } else {
      System.out.println("Received arguments:");
      for (String arg : args) {
        System.out.println("Argument: " + arg);
      }

      String code = getCodeFromArgs(args[0]);
      completeLoginFlow(code);
    }
  }

  private static void startHttpServer() {
    // We use SparkJava (not to be confused with Apache Spark) to set up a local server
    port(3000);
    // Not to be confused with the doPostRequest() custom method we have, which uses the other library
    post("/v1/auth/authorizeJewel", (req, res) -> {
      String code = req.queryParams("code");

      System.out.println("Code received from website: " + code);

      // Simulates relaunching the program with the code as an argument
      try {
        String jarPath = new java.io.File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        new ProcessBuilder("java", "-jar", jarPath, "jewel://code=" + code).start();
      } catch (Exception e) {
        e.printStackTrace();
      }

      return "OK";
    });

    System.out.println("HTTP server listening on http://localhost:3000");
  }

  private static void launchLoginFlow() {
    try {
      String verifier = PkceUtil.generateCodeVerifier();
      System.out.println("Verifier: " + verifier);
      String challengeString = PkceUtil.generateCodeChallenge(verifier);

      // Saves the verifier on a temporary file
      Path challengeFilePath = Path.of(CHALLENGE_FILE);
      Files.writeString(challengeFilePath, challengeString);
      System.out.println("Generated challenge: " + challengeString);

      System.out.println("Opening browser...");
      String loginUrl = URL_LOGIN + URLEncoder.encode(challengeString, StandardCharsets.UTF_8);
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(new URI(loginUrl));
      } else {
        System.out.println("Open this URL manually:\n" + loginUrl);
      }

    } catch (Exception e) {
      System.err.println("Error when launching login flow: " + e.getMessage());
    }
  }

  private static void completeLoginFlow(String code) {
    try {
      File jarFile = new File(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      File jarPath = jarFile.getParentFile();

      // We now read the challenge from the file
      File challengeFile = new File(jarPath, "challenge.txt");
      byte[] bytes = Files.readAllBytes(challengeFile.toPath());
      String challengeString = new String(bytes, StandardCharsets.UTF_8);

      System.out.println("Sending code and challenge to the website:");
      System.out.println("Code: " + code);
      System.out.println("Challenge: " + challengeString);

      doPostRequest(challengeString);
    } catch (Exception e) {
      System.err.println("Error while completing login flow: " + e.getMessage());
    }
  }

  private static void doPostRequest(String challenge) {
    RequestParams params = new RequestParams();
    HttpClient httpClient = new AsyncHttpClient();
    HttpResponseHandler postHandler = new HttpResponseHandler () {
      @Override
      public void onSuccess(int statusCode, Map<String, List<String>> headers, byte[] content) {
        System.out.println("POST SUCCESS");

        String contentString = new String(content, StandardCharsets.UTF_8);
        System.out.println("Content = " + contentString);

        System.out.println("FINISHED!! You can close the program now.");
      }

      @Override
      public void onFailure(int statusCode, Map<String, List<String>> headers, byte[] content) {
        System.out.println("POST FAILURE. CODE: " + statusCode);

        String contentString = new String(content, StandardCharsets.UTF_8);
        System.out.println("Content = " + contentString);

        System.out.println("FINISHED!! You can close the program now.");
      }

      @Override
      public void onFailure(Throwable throwable) {
        System.out.println("POST - COULD NOT CONNECT TO THE SERVER");

        System.out.println("FINISHED!! You can close the program now.");
      }
    };

    params.put("challenge", challenge);

    httpClient.post(URL_AUTHORIZATION,
                    params,
                    postHandler);
  }

  private static String getCodeFromArgs(String rawArg) {
    // For example: jewel://code=abc123/ → abc123
    int idx = rawArg.indexOf("code=");
    if (idx != -1) {
      String temp = rawArg.substring(idx + 5);
      return temp.replace("/", "");
    }
    return "";
  }
}
