package com.example.experiment.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class JwtUtils {

    private static final String SECRET = "virtual-experiment-platform-jwt-secret-key-2026";
    private static final long EXPIRATION = 24 * 60 * 60 * 1000; // 24小时
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /** 生成 token */
    public static String generateToken(String userId, List<String> roles) {
        return Jwts.builder()
                .subject(userId)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    /** 从 token 中获取 userId */
    public static String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /** 从 token 中获取角色列表 */
    @SuppressWarnings("unchecked")
    public static List<String> getRoles(String token) {
        return parseClaims(token).get("roles", List.class);
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
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
