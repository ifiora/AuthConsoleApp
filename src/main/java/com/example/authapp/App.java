package com.example.authapp;

import HttpConnection.AsyncHttpClient;
import HttpConnection.HttpClient;
import HttpConnection.HttpResponseHandler;
import HttpConnection.RequestParams;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.*;
import java.awt.Desktop;
import java.nio.charset.StandardCharsets;
import static spark.Spark.*;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;

public class App {
  private static final String URL_APP = "https://eeg-portal.soplo.com.ar/";
  private static final String URL_LOGIN = URL_APP + "jewel-login/";
  private static final String URL_AUTHORIZATION = URL_APP + "v1/auth/jewelAuthorize";

  public static void main(String[] args) {
    // We make the SparkJava logger less verbose...
    Log.setLog(new StdErrLog());
    Log.getRootLogger().setDebugEnabled(false);

    System.out.println("Starting Application...");

    try {

      String verifier = PkceUtil.generateCodeVerifier();
      String challenge = PkceUtil.generateCodeChallenge(verifier);

      System.out.println("Verifier and challenge generated...");

      launchLoginFlow(challenge);
      startHttpServer(verifier);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private static void startHttpServer(String verifier) {
    port(49160);
    enableCors(URL_APP);

    post("/v1/auth/authorizeJewel", (req, res) -> {
      res.type("application/json");

      try {
        // Parseo del body como JSON
        String body = req.body();
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        if (!json.has("code") || json.get("code").isJsonNull()) {
          res.status(400);
          return new Gson().toJson(new ApiResponse<Boolean>(false, "Bad Request"));
        }

        String code = json.get("code").getAsString();
        System.out.println("Code received from website: " + code);

        // Tu lógica (si falla, cae al catch)
        doPostRequest(code, verifier);

        // OK → IResponse<boolean> con data=true
        res.status(200);
        return new Gson().toJson(new ApiResponse<Boolean>(true));

      } catch (Exception e) {
        e.printStackTrace();
        res.status(500);

        // Podés devolver info mínima del error o un objeto
        Map<String, Object> err = new HashMap<>();
        err.put("type", e.getClass().getSimpleName());
        err.put("message", e.getMessage());

        return new Gson().toJson(new ApiResponse<Boolean>(false, "Server Error", err));
      }
    });

    System.out.println("HTTP server listening on http://localhost:49160");
  }

  private static void enableCors(String allowedOriginRaw) {
    // Normalizo el origin: sin barra final
    String allowedOrigin = allowedOriginRaw.replaceAll("/+$", "");

    // Preflight (OPTIONS) — acá seteamos CORS
    options("/*", (req, res) -> {
      String reqHeaders = req.headers("Access-Control-Request-Headers");
      if (reqHeaders != null)
        res.header("Access-Control-Allow-Headers", reqHeaders);

      String reqMethod = req.headers("Access-Control-Request-Method");
      res.header("Access-Control-Allow-Methods",
          (reqMethod != null ? reqMethod : "GET,POST,PUT,DELETE") + ", OPTIONS");

      // IMPORTANTE: setear solo una vez y con el origin bien formado
      res.raw().setHeader("Access-Control-Allow-Origin", allowedOrigin);
      res.header("Vary", "Origin");

      // Si usás cookies/sesiones desde el browser, descomentá:
      // res.header("Access-Control-Allow-Credentials", "true");

      res.status(204);
      return "";
    });

    // Respuestas reales (no OPTIONS) — seteamos CORS acá
    before((req, res) -> {
      if (!"OPTIONS".equalsIgnoreCase(req.requestMethod())) {
        res.raw().setHeader("Access-Control-Allow-Origin", allowedOrigin);
        res.header("Vary", "Origin");
        // res.header("Access-Control-Allow-Credentials", "true"); // si usás cookies
        res.header("Access-Control-Expose-Headers", "Content-Type");
      }
    });
  }

  private static void launchLoginFlow(String challenge) {
    try {
      System.out.println("Opening browser...");
      String loginUrl = URL_LOGIN + URLEncoder.encode(challenge, StandardCharsets.UTF_8);
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(new URI(loginUrl));
      } else {
        System.out.println("Open this URL manually:\n" + loginUrl);
      }
    } catch (Exception e) {
      System.err.println("Error when launching login flow: " + e.getMessage());
    }
  }

  private static void doPostRequest(String code, String verifier) {
    try {
      URL u = new URL(URL_AUTHORIZATION);
      int port = (u.getPort() == -1 ? u.getDefaultPort() : u.getPort());
      System.out.printf("POST → %s (host=%s port=%d path=%s)%n", u, u.getHost(), port, u.getPath());
    } catch (Exception e) {
      logThrowable("Bad URL", e);
      return;
    }

    HttpClient httpClient = new AsyncHttpClient();
    httpClient.setHeader("Accept", "application/json");
    httpClient.setHeader("Content-Type", "application/x-www-form-urlencoded");

    RequestParams params = new RequestParams();
    params.put("code", code);
    params.put("verifier", verifier);

    HttpResponseHandler postHandler = new HttpResponseHandler() {
      @Override
      public void onSuccess(int status, Map<String, List<String>> h, byte[] c) {
        System.out.println("POST SUCCESS (" + status + ")");
        System.out.println("Content = " + (c != null ? new String(c, java.nio.charset.StandardCharsets.UTF_8) : ""));
        System.out.println("FINISHED!! You can close the program now.");
      }

      @Override
      public void onFailure(int status, Map<String, List<String>> h, byte[] c) {
        System.out.println("POST FAILURE. CODE: " + status);
        System.out.println("Content = " + (c != null ? new String(c, java.nio.charset.StandardCharsets.UTF_8) : ""));
        System.out.println("FINISHED!! You can close the program now.");
      }

      @Override
      public void onFailure(Throwable t) {
        logThrowable("HTTP POST failed", t); // acá verás el ConnectException si no hay server
        System.out.println("POST - COULD NOT CONNECT TO THE SERVER");
        System.out.println("FINISHED!! You can close the program now.");
      }
    };

    httpClient.post(URL_AUTHORIZATION, params, postHandler);
  }

  private static void logThrowable(String prefix, Throwable t) {
    System.err.println(prefix + " :: " + t.getClass().getName() + ": " + t.getMessage());

    // Cadena de causas
    int i = 0;
    for (Throwable c = t; c != null; c = c.getCause()) {
      System.err.printf("cause[%d]: %s: %s%n", i++, c.getClass().getName(), c.getMessage());
    }

    // Excepciones suprimidas (si hubiera)
    for (Throwable s : t.getSuppressed()) {
      System.err.println("suppressed: " + s.getClass().getName() + ": " + s.getMessage());
    }

    // Stacktrace completo
    t.printStackTrace(System.err);
  }
}
