/**
 * 个人资料字段的校验规则。
 *
 * 与后端 `UpdateProfileDTO` 的注解保持一致 —— 此处仅供表单即时反馈，
 * **服务端校验才是权威**。
 *
 * 手机号与邮箱都<b>允许留空</b>：正则里的 `^$` 分支就是为此。后端写成
 * `^$|^1[3-9]\d{9}$` 也是同一个原因 —— 纯 `^1[3-9]\d{9}$` 会让「清空手机号」这个动作
 * 被自己的校验挡住，用户想删掉号码却删不掉。
 */

export const PHONE_PATTERN = /^$|^1[3-9]\d{9}$/
export const PHONE_MESSAGE = '手机号格式不正确'

export const EMAIL_PATTERN = /^$|^[^\s@]+@[^\s@]+\.[^\s@]+$/
export const EMAIL_MESSAGE = '邮箱格式不正确'

/** 与 users.gender（TINYINT）取值一致：0 未设置 / 1 男 / 2 女 */
export const GENDER_OPTIONS = [
  { value: '1', label: '男' },
  { value: '2', label: '女' },
  { value: '0', label: '未设置' }
]
