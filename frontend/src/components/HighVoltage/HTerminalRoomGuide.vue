<!-- 终端小室接线指导面板 —— 按参考图复刻（端子 13-48，上下两排） -->
<template>
  <div class="terminal-guide-panel">
    <div class="replica">
      <!-- 上排：遥控轮次 / 遥信（39-48） -->
      <div class="tg-row">
        <div v-for="g in groups.top" :key="g.label + g.nums[0]" class="tg-grp" :style="{ '--tg-n': g.nums.length }">
          <!-- 顶部符号行（恒定占位：遥控组填常开/常闭，其余留白 → 数字/横线跨组对齐） -->
          <div class="tg-syms">
            <span v-for="(_, i) in g.nums" :key="i" class="tg-sym">
              {{ g.symPos === 'top' ? (g.syms[i] || ' ') : ' ' }}
            </span>
          </div>
          <div class="tg-nums">
            <span v-for="n in g.nums" :key="n" class="tg-num">{{ n }}</span>
          </div>
          <!-- 端子排连接线：每数字一竖线、底部横线相连 -->
          <svg class="tg-connect" :viewBox="'0 0 ' + g.nums.length * 10 + ' 12'" preserveAspectRatio="none"
            aria-hidden="true">
            <line v-for="(_, i) in g.nums" :key="'v' + i" :x1="i * 10 + 5" :x2="i * 10 + 5" y1="0" y2="9" />
            <line :x1="5" :x2="(g.nums.length - 1) * 10 + 5" y1="9" y2="9" />
          </svg>
          <!-- 底部±（仅信号组：±紧贴横线，下方小字贴±）；无±组不渲染，标签大字+远距 -->
          <div v-if="g.symPos === 'bottom'" class="tg-syms tg-syms-bottom">
            <span v-for="(_, i) in g.nums" :key="i" class="tg-sym">{{ g.syms[i] || ' ' }}</span>
          </div>
          <div class="tg-label" :class="{ 'tg-label-big': g.symPos !== 'bottom' }">{{ g.label }}</div>
        </div>
      </div>

      <div class="tg-sep" />

      <!-- 下排：不用 / 告警 / 脉冲 / RS485 / 门接点（13-38） -->
      <div class="tg-row">
        <div v-for="g in groups.bottom" :key="g.label + g.nums[0]" class="tg-grp" :style="{ '--tg-n': g.nums.length }">
          <div class="tg-syms">
            <span v-for="(_, i) in g.nums" :key="i" class="tg-sym">
              {{ g.symPos === 'top' ? (g.syms[i] || ' ') : ' ' }}
            </span>
          </div>
          <div class="tg-nums">
            <span v-for="n in g.nums" :key="n" class="tg-num">{{ n }}</span>
          </div>
          <svg class="tg-connect" :viewBox="'0 0 ' + g.nums.length * 10 + ' 12'" preserveAspectRatio="none"
            aria-hidden="true">
            <line v-for="(_, i) in g.nums" :key="'v' + i" :x1="i * 10 + 5" :x2="i * 10 + 5" y1="0" y2="9" />
            <line :x1="5" :x2="(g.nums.length - 1) * 10 + 5" y1="9" y2="9" />
          </svg>
          <div v-if="g.symPos === 'bottom'" class="tg-syms tg-syms-bottom">
            <span v-for="(_, i) in g.nums" :key="i" class="tg-sym">{{ g.syms[i] || ' ' }}</span>
          </div>
          <div class="tg-label" :class="{ 'tg-label-big': g.symPos !== 'bottom' }">{{ g.label }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 端子分组（按参考图）；syms 为每孔符号，symPos 控制符号在数字上/下方，'' 为空白占位
const groups = {
  // 上排（39-48）：遥控轮次 ×2（符号在上）+ 遥信 ×2（符号在下）
  top: [
    { nums: [39, 40, 41], syms: ['常开', '', '常闭'], symPos: 'top', label: '遥控轮次1' },
    { nums: [42, 43, 44], syms: ['常开', '', '常闭'], symPos: 'top', label: '遥控轮次2' },
    { nums: [45, 46], syms: ['+', '-'], symPos: 'bottom', label: '遥信1' },
    { nums: [47, 48], syms: ['+', '-'], symPos: 'bottom', label: '遥信2' }
  ],
  // 下排（13-38）：不用 ×2 + 告警（无符号）+ 脉冲输入 ×4 + 脉冲输出 ×2 + RS485 ×2 + 门接点（均 ± 在下）
  bottom: [
    // 无符号组（不用/告警）：symPos='none'，顶部符号行仍恒定占位保证数字/横线对齐，但底部±行不渲染
    { nums: [13, 14, 15], syms: ['', '', ''], symPos: 'none', label: '不用' },
    { nums: [16, 17, 18], syms: ['', '', ''], symPos: 'none', label: '不用' },
    { nums: [19, 20], syms: ['', ''], symPos: 'none', label: '告警' },
    { nums: [21, 22], syms: ['+', '-'], symPos: 'bottom', label: '脉冲输入1' },
    { nums: [23, 24], syms: ['+', '-'], symPos: 'bottom', label: '脉冲输入2' },
    { nums: [25, 26], syms: ['+', '-'], symPos: 'bottom', label: '脉冲输入3' },
    { nums: [27, 28], syms: ['+', '-'], symPos: 'bottom', label: '脉冲输入4' },
    { nums: [29, 30], syms: ['+', '-'], symPos: 'bottom', label: '有功脉冲输出' },
    { nums: [31, 32], syms: ['+', '-'], symPos: 'bottom', label: '无功脉冲输出' },
    { nums: [33, 34], syms: ['+', '-'], symPos: 'bottom', label: 'RS485-1' },
    { nums: [35, 36], syms: ['+', '-'], symPos: 'bottom', label: 'RS485-2' },
    { nums: [37, 38], syms: ['+', '-'], symPos: 'bottom', label: '门接点' }
  ]
}
</script>

<style scoped>
/* ─────────────────────────────────────────────
   行尺寸集中管理（cqw = 面板宽 1%，全部相对宽度缩放）
   参考图：横向长条，上排（39-48）+ 下排（13-38）
   ───────────────────────────────────────────── */
.terminal-guide-panel {
  --tg-slot: 2.4cqw;
  /* 单端子槽宽（细长端子排）；横线 width=slot×数字数 随数字间距拉长 */
  --tg-sym-h: 1.1cqw;
  /* 符号行高（顶部+底部统一，保证数字/横线/标签跨组对齐） */
  --tg-num-fs: 1.3cqw;
  /* 数字字号 */
  --tg-sym-fs: 1.3cqw;
  /* 顶部符号（常开/常闭）字号 */
  --tg-bottom-sym-fs: 1.5cqw;
  /* ±号字号（底部，单独控制，较小于常开/常闭） */

  position: relative;
  z-index: 10;
  width: 100%;
  container-type: inline-size;
  background: rgba(171, 168, 158, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 1.2cqw;
  padding: 0.3cqw 0.5cqw;
  backdrop-filter: blur(2px);
  pointer-events: none;
  box-sizing: border-box;
  overflow: hidden;
}

.replica {
  position: relative;
  width: 100%;
  background: #9ba89b;
  border-radius: 0.8cqw;
  padding: 0.35cqw 0.4cqw;
  color: #f2f2f2;
  display: flex;
  flex-direction: column;
  gap: 0.3cqw;
  box-sizing: border-box;
}

/* 上下排分隔线 */
.tg-sep {
  width: 100%;
  height: 0.5cqw;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 0.3cqw;
}

.tg-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35cqw 2.6cqw;
  align-items: flex-start;
}

/* 单个端子组：宽度=数字行宽（slot×数字数），标签/符号居中可溢出但**不撑宽组**，
   保证多组长标签不会把下排挤到换行，数字间距可独立调大 */
.tg-grp {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15cqw;
  width: calc(var(--tg-slot) * var(--tg-n));
}

/* 顶部符号行（常开/常闭）恒定占位 → 数字/横线跨组对齐；与数字等宽 */
.tg-syms {
  display: flex;
  min-height: var(--tg-sym-h);
}

/* 底部符号（±）在横线下方：紧贴横线（负上边距让+顶贴住横线，与样例一致）；
   有±组标签小字、紧贴±下方 */
.tg-syms-bottom {
  margin-top: -0.5cqw;
  margin-bottom: 0.08cqw;
  height: var(--tg-sym-h);
  overflow: hidden;
}

.tg-sym {
  width: var(--tg-slot);
  text-align: center;
  line-height: var(--tg-sym-h);
  white-space: nowrap;
}

/* 常开/常闭（顶部符号）字号 */
.tg-syms:not(.tg-syms-bottom)>.tg-sym {
  font-size: var(--tg-sym-fs);
}

/* ±号（底部符号）字号，独立控制、较小于常开/常闭 */
.tg-syms-bottom>.tg-sym {
  font-size: var(--tg-bottom-sym-fs);
}

/* 数字行 */
.tg-nums {
  display: flex;
}

.tg-num {
  width: var(--tg-slot);
  text-align: center;
  font-size: var(--tg-num-fs);
  font-weight: 700;
  line-height: var(--tg-num-fs);
}

/* 端子排连接线（每数字一竖线 + 底部横线），宽度=数字行宽（slot×数字数），
   不受长标签撑宽 → 竖线恰好正对每个数字中心 */
.tg-connect {
  width: calc(var(--tg-slot) * var(--tg-n));
  height: 1.15cqw;
  display: block;
  stroke: rgba(255, 255, 255, 0.9);
  stroke-width: 1;
  fill: none;
}

/* 标签：有±组用小字（含"有功脉冲输出"等长标签，避免溢出到相邻组）、紧贴±下方 */
.tg-label {
  font-size: 1.2cqw;
  margin-top: 0.02cqw;
  line-height: 1.15;
  white-space: nowrap;
}

/* 无±组标签：字体更大（参照图），与上方横线间隔适度（不过大） */
.tg-label-big {
  font-size: 1.7cqw;
}
</style>
