/**
 * 密码强度规则（注册与重置密码共用）。
 *
 * 与后端 `PasswordPolicy.java` 保持一致：长度 6-20 位，必须同时包含字母和数字，不含空白字符。
 * 此处仅供表单即时反馈，**服务端校验才是权威**。
 */

export const PASSWORD_MIN_LENGTH = 6
export const PASSWORD_MAX_LENGTH = 20

export const PASSWORD_MESSAGE = '密码需6-20位，且同时包含字母和数字'

/** 与后端 RegisterDTO / UpdatePasswordDTO 的 @Pattern 完全一致 */
export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)\S{6,20}$/

/** 密码是否满足强度要求 */
export function isValidPassword(password) {
  return PASSWORD_PATTERN.test(password || '')
}
