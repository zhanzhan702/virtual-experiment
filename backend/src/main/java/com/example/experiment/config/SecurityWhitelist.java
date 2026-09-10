package com.example.experiment.config;

import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 免鉴权路径白名单。
 *
 * <p>刻意采用精确匹配而非 {@code /api/auth/**} 通配：后者会把 {@code /api/auth/me} 也放行， 导致该接口拿不到
 * userId。白名单只应包含真正无需登录的接口。
 */
@Component
public class SecurityWhitelist {

  private static final Set<String> PUBLIC_PATHS = Set.of("/api/auth/login", "/api/auth/register");

  /** 路径是否无需登录即可访问 */
  public boolean isPublic(String path) {
    if (path == null) {
      return false;
    }
    // 容忍末尾斜杠（/api/auth/login/）
    String normalized = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    return PUBLIC_PATHS.contains(normalized);
  }
}
