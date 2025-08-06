package com.example.authapp;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PkceUtil {

  public static String generateCodeVerifier() {
    byte[] code = new byte[32];
    new SecureRandom().nextBytes(code);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(code);
  }

  public static String generateCodeChallenge(String codeVerifier) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] digest = md.digest(codeVerifier.getBytes("US-ASCII"));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
  }
}
