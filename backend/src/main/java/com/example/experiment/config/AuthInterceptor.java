package com.example.experiment.config;

import com.example.experiment.utils.JwtUtils;
import com.example.experiment.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器。
 *
 * <p>链路：解析 Bearer token → 校验 → 写入 {@link UserContext} → 校验 {@link RequireRole} → 放行。
 *
 * <p>放行路径在 {@link WebConfig} 中配置，仅 {@code /api/auth/login} 与 {@code /api/auth/register}。 注意不是整个
 * {@code /api/auth/**} —— {@code /api/auth/me} 需要经过本拦截器以取得 userId。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

  private final SecurityWhitelist whitelist;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // 放行预检请求（CORS 由 CorsFilter 处理，此处不拦截）
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    String path = request.getRequestURI();
    if (whitelist.isPublic(path)) {
      return true;
    }

    // 1. 解析 Authorization 头
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      return reject(response, HttpStatus.UNAUTHORIZED, "未登录");
    }

    String token = header.substring("Bearer ".length()).trim();
    if (token.isEmpty() || !JwtUtils.validateToken(token)) {
      return reject(response, HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
    }

    // 2. 写入请求上下文
    String userId = JwtUtils.getUserId(token);
    List<String> roles = JwtUtils.getRoles(token);
    int maxLevel = JwtUtils.getMaxLevel(token);
    UserContext.set(userId, roles, maxLevel);

    // 3. 校验角色要求（方法级优先，其次类级）
    if (handler instanceof HandlerMethod handlerMethod) {
      Role required = resolveRequiredRole(handlerMethod.getMethodAnnotation(RequireRole.class));
      if (required == null) {
        // 方法上没标注时，回退到 Controller 类上的标注
        required =
            resolveRequiredRole(handlerMethod.getBeanType().getAnnotation(RequireRole.class));
      }
      if (required != null && maxLevel < required.getLevel()) {
        return reject(response, HttpStatus.FORBIDDEN, "权限不足");
      }
    }

    return true;
  }

  /** 从注解中取出要求的角色；注解为 null 时返回 null */
  private Role resolveRequiredRole(RequireRole annotation) {
    return annotation == null ? null : annotation.value();
  }

  /**
   * 请求结束务必清理 ThreadLocal。
   *
   * <p>Tomcat 复用线程处理请求，不清理会让下一个请求读到上一个用户的身份（线程池串号）。 异常路径也会走到这里，因此清理是可靠的。
   */
  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    UserContext.clear();
  }

  /** 返回 JSON 错误响应；与 AuthController 的 {@code Map.of("message", ...)} 结构保持一致 */
  private boolean reject(HttpServletResponse response, HttpStatus status, String message)
      throws Exception {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write("{\"message\":\"" + message + "\"}");
    return false;
  }
}
