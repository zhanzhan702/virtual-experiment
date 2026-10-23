package com.example.experiment.config;

import com.example.experiment.utils.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** 统一登录 Cookie 的签发、读取与清除属性。 */
@Component
public class AuthCookie {

  public static final String NAME = "auth_token";
  private final boolean secure;

  public AuthCookie(@Value("${auth.cookie-secure:false}") boolean secure) {
    this.secure = secure;
  }

  public String issue(String token) {
    return build(token, JwtUtils.TOKEN_TTL);
  }

  public String clear() {
    return build("", Duration.ZERO);
  }

  public String read(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return null;
    for (Cookie cookie : cookies) {
      if (NAME.equals(cookie.getName())) return cookie.getValue();
    }
    return null;
  }

  private String build(String token, Duration maxAge) {
    return ResponseCookie.from(NAME, token)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/api")
        .maxAge(maxAge)
        .build()
        .toString();
  }
}
