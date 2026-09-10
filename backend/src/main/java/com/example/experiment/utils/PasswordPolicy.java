package com.example.experiment.utils;

import java.util.regex.Pattern;

/**
 * 密码强度规则（注册与重置密码共用）。
 *
 * <p>规则：长度 6-20 位，必须同时包含字母和数字，且不含空白字符。
 *
 * <p>前端在 {@code frontend/src/constants/password-rule.js} 有一份等价实现，仅用于即时反馈； <b>服务端校验才是权威</b>，前端校验不可信。
 */
public final class PasswordPolicy {

  /** 长度下限 */
  public static final int MIN_LENGTH = 6;

  /** 长度上限 */
  public static final int MAX_LENGTH = 20;

  /** 统一提示文案，前后端保持一致 */
  public static final String MESSAGE = "密码需6-20位，且同时包含字母和数字";

  /**
   * 至少一个字母 + 至少一个数字 + 无空白 + 长度 6-20。
   *
   * <p>用 {@code \S} 直接排除空白字符，无需额外检查。
   */
  private static final Pattern PATTERN =
      Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)\\S{" + MIN_LENGTH + "," + MAX_LENGTH + "}$");

  private PasswordPolicy() {}

  /** 密码是否满足强度要求；null 或空串返回 false */
  public static boolean isValid(String password) {
    return password != null && PATTERN.matcher(password).matches();
  }
}
