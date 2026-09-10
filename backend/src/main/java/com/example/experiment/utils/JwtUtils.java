package com.example.experiment.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;

public class JwtUtils {

  private static final String SECRET = "virtual-experiment-platform-jwt-secret-key-2026";
  private static final long EXPIRATION = 24 * 60 * 60 * 1000; // 24小时
  private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

  /**
   * 生成 token。
   *
   * <p>maxLevel 为签发时刻的角色层级快照（见 {@link com.example.experiment.config.Role}）， 之后每个请求直接读 token
   * 而无需查库。代价：token 有效期内（24 小时）调整了角色，对方仍持旧权限。 后期实现角色管理时，在角色变更接口中引入 token_version 即可。
   */
  public static String generateToken(String userId, List<String> roles, int maxLevel) {
    return Jwts.builder()
        .subject(userId)
        .claim("roles", roles)
        .claim("maxLevel", maxLevel)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
        .signWith(KEY)
        .compact();
  }

  /** 从 token 中获取 userId */
  public static String getUserId(String token) {
    return parseClaims(token).getSubject();
  }

  /** 从 token 中获取角色列表；老 token 无该 claim 时返回空列表 */
  @SuppressWarnings("unchecked")
  public static List<String> getRoles(String token) {
    Object roles = parseClaims(token).get("roles");
    return roles instanceof List ? (List<String>) roles : List.of();
  }

  /** 从 token 中获取角色层级；老 token 无该 claim 时返回 0（视为无权限） */
  public static int getMaxLevel(String token) {
    Object level = parseClaims(token).get("maxLevel");
    return level instanceof Number ? ((Number) level).intValue() : 0;
  }

  /** 验证 token 是否有效 */
  public static boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private static Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload();
  }
}
