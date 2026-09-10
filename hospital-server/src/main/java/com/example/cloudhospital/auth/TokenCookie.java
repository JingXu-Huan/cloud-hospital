package com.example.cloudhospital.auth;

import jakarta.servlet.http.HttpServletResponse;

final class TokenCookie {
  static final String NAME = "cloud-hospital-token";
  private static final int MAX_AGE_SECONDS = 30 * 60;

  private TokenCookie() {}

  static void write(HttpServletResponse response, String token) {
    response.setHeader("X-Auth-Token", token);
    response.addHeader(
        "Set-Cookie",
        NAME
            + "="
            + token
            + "; Max-Age="
            + MAX_AGE_SECONDS
            + "; Path=/api/v1; HttpOnly; SameSite=Lax");
  }
}
