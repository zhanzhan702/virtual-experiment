package com.example.experiment.utils;

import java.util.Collections;
import java.util.List;

/**
 * 请求级用户上下文。由 {@link com.example.experiment.config.AuthInterceptor} 在 preHandle 写入。
 *
 * <p><b>必须在请求结束时调用 {@link #clear()}。</b> Tomcat 复用线程处理请求， 若不清理会导致下一个请求读到上一个用户的身份（线程池串号）。
 * AuthInterceptor 已在 afterCompletion 中统一清理。
 */
public final class UserContext {

  private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
  private static final ThreadLocal<List<String>> ROLES = new ThreadLocal<>();
  private static final ThreadLocal<Integer> MAX_LEVEL = new ThreadLocal<>();

  private UserContext() {}

  public static void set(String userId, List<String> roles, int maxLevel) {
    USER_ID.set(userId);
    ROLES.set(roles == null ? Collections.emptyList() : roles);
    MAX_LEVEL.set(maxLevel);
  }

  /** 当前登录用户 ID；未登录时为 null（正常流程下拦截器已挡掉） */
  public static String getUserId() {
    return USER_ID.get();
  }

  public static List<String> getRoles() {
    List<String> roles = ROLES.get();
    return roles == null ? Collections.emptyList() : roles;
  }

  /** 当前用户的最高角色层级；未登录时为 0 */
  public static int getMaxLevel() {
    Integer level = MAX_LEVEL.get();
    return level == null ? 0 : level;
  }

  public static void clear() {
    USER_ID.remove();
    ROLES.remove();
    MAX_LEVEL.remove();
  }
}
