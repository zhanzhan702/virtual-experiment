/**
 * 低压居民表箱外部场景的坐标与尺寸。
 *
 * ★★★ 本文件是位置/大小的唯一调整入口 —— 改这里即可，不需要动组件 ★★★
 *
 * 所有数值都是「相对场景图」的百分比：
 *   left / top   元素左上角在场景图中的位置
 *   width        元素宽度占场景图宽度的比例
 *   height       固定写 'auto'，按图片原始比例缩放
 *
 * 场景容器（.scene-group）的尺寸由内部背景图决定，所以改窗口大小时整体等比缩放，
 * 这里的百分比不需要跟着改。
 */

// ── 围栏：左右两半各自独立落位 ─────────────────────────────
export const LEFT_FENCE_STYLE = { left: '-12%', top: '58%', width: '56%', height: 'auto' }
export const RIGHT_FENCE_STYLE = { left: '56%', top: '63%', width: '46%', height: 'auto' }

// ── 两块标识牌：分开挂，各自独立的位置与大小 ────────────────
export const SIGN_DANGER_STYLE = { left: '3%', top: '40%', width: '10%', height: 'auto' }
export const SIGN_WORKING_STYLE = { left: '82%', top: '40%', width: '10%', height: 'auto' }

// ── 排插：叠在背景左上角（与高压一致，不画进背景图） ────────
// left 不能小于 14%：左侧工具栏是 fixed 定位、宽 10vw，场景宽 90vw，
// 工具栏会盖住场景最左侧约 11% 的宽度，排插落在那下面就点不到
export const POWER_SOCKET_STYLE = { left: '14%', top: '12%', width: '7%', height: 'auto' }

// ── 三步验电第二步：表箱门把热区 ───────────────────────────
// 只做命中判定；验电进行到第二步时会显示半透明高亮框，方便调坐标
export const DOOR_CHECK_ZONE = { left: '52%', top: '30%', width: '8%', height: '12%' }

// ── 点击判定区域（不显示，只决定「点哪里算点中」） ──────────
export const FENCE_HIT_ZONE = { left: '5%', top: '55%', width: '90%', height: '35%' }
export const SIGN_HIT_ZONE = { left: '30%', top: '8%', width: '40%', height: '45%' }
