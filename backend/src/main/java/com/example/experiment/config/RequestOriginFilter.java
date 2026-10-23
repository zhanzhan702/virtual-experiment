package com.example.experiment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Cookie 自动随请求发送，所有写接口（包括登录）都必须校验可信来源以防止 CSRF。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestOriginFilter extends OncePerRequestFilter {

  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
  private final List<String> allowedOrigins;

  public RequestOriginFilter(
      @Value("${auth.allowed-origins:http://localhost:5173}") List<String> allowedOrigins) {
    this.allowedOrigins =
        allowedOrigins.stream()
            .map(String::trim)
            .map(
                value -> {
                  String origin = parseOrigin(value, false);
                  if (origin == null) throw new IllegalArgumentException("非法可信来源: " + value);
                  return origin;
                })
            .distinct()
            .toList();
  }

  public List<String> getAllowedOrigins() {
    return allowedOrigins;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    // 部分 Servlet 调用不提供 servletPath，此时使用完整请求路径。
    if (path.isEmpty()) path = request.getRequestURI();
    return !(path.equals("/api") || path.startsWith("/api/"))
        || SAFE_METHODS.contains(request.getMethod());
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String originHeader = request.getHeader("Origin");
    String origin =
        originHeader != null
            ? parseOrigin(originHeader, false)
            : parseOrigin(request.getHeader("Referer"), true);
    if (origin == null || !allowedOrigins.contains(origin)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write("{\"message\":\"请求来源不可信\"}");
      return;
    }
    filterChain.doFilter(request, response);
  }

  /** 按协议、主机与有效端口匹配，不能用前缀匹配（可信域名的后缀可能是恶意域名）。 */
  private static String parseOrigin(String value, boolean referer) {
    if (value == null) return null;
    try {
      URI uri = URI.create(value).parseServerAuthority();
      String scheme = uri.getScheme();
      if (scheme == null || uri.getHost() == null || uri.getRawUserInfo() != null) return null;
      scheme = scheme.toLowerCase(Locale.ROOT);
      if (!scheme.equals("http") && !scheme.equals("https")) return null;
      if (!referer
          && ((uri.getRawPath() != null && !uri.getRawPath().isEmpty())
              || uri.getRawQuery() != null
              || uri.getRawFragment() != null)) return null;
      int port = uri.getPort();
      if (port > 65535) return null;
      String suffix =
          port == -1
                  || (scheme.equals("http") && port == 80)
                  || (scheme.equals("https") && port == 443)
              ? ""
              : ":" + port;
      return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + suffix;
    } catch (Exception e) {
      return null;
    }
  }
}
