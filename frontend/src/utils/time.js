/**
 * 时间格式化工具 — 统一本地时间输出
 * 格式: yyyy-MM-dd HH:mm:ss（与后端 LocalDateTime 一致）
 */

const pad = n => String(n).padStart(2, '0')

/**
 * 将 Date 对象格式化为本地时间字符串
 * @param {Date} d
 * @returns {string} 如 "2026-07-17 14:30:00"
 */
export function formatLocalTime(d) {
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}
