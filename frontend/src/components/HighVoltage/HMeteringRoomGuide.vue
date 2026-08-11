<!-- 计量小室终端编号提示面板 —— 按参考图 1:1 复刻 -->
<template>
  <div class="terminal-guide-panel">
    <div class="replica">
      <!-- 16 列端子编号 -->
      <div class="tg-grid">
        <div v-for="item in terminals" :key="item.num" class="tg-col">
          <div class="tg-top">{{ item.top || ' ' }}</div>
          <div class="tg-box">{{ item.num }}</div>
          <div v-if="item.arrow" class="tg-arrow">
            <svg viewBox="0 0 12 18" width="12" height="18">
              <path
                d="M6 1L6 13M2 10L6 15L10 10"
                stroke="currentColor"
                stroke-width="1.2"
                fill="none"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          <div v-else-if="item.line" class="tg-line">
            <svg viewBox="0 0 12 18" width="12" height="18">
              <path
                d="M6 1L6 15"
                stroke="currentColor"
                stroke-width="1.2"
                fill="none"
                stroke-linecap="round"
              />
            </svg>
          </div>
          <div class="tg-label">{{ item.label }}</div>
        </div>
        <!-- 485 接口分组：双端子连线汇聚到端口，端口标 485，下方标 接口N -->
        <div class="tg-conv-row">
          <svg viewBox="0 0 1000 60" preserveAspectRatio="none" aria-hidden="true">
            <!-- 接口1：端子 24(中心718.75) + 25(中心781.25) 汇聚到中点750 -->
            <!-- 竖线24 → 横线 → 竖线25（倒U形），再从中点竖线到端口 -->
            <path
              d="M718.75 0 L718.75 14 L781.25 14 L781.25 0"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
            />
            <line x1="750" y1="14" x2="750" y2="30" stroke="currentColor" stroke-width="1.2" />
            <!-- 485 端口框（width/height 加大即放大；x 保持中点居中，文字 y 随框微调） -->
            <rect
              x="725"
              y="30"
              width="50"
              height="25"
              rx="2"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
            />
            <text x="750" y="50" text-anchor="middle" class="tg-group-text" font-size="9">485</text>
            <text x="750" y="85" text-anchor="middle" class="tg-group-text">接口1</text>

            <!-- 接口2：端子 27(中心906.25) + 28(中心968.75) 汇聚到中点937.5 -->
            <path
              d="M906.25 0 L906.25 14 L968.75 14 L968.75 0"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
            />
            <line x1="937.5" y1="14" x2="937.5" y2="30" stroke="currentColor" stroke-width="1.2" />
            <rect
              x="912.5"
              y="30"
              width="50"
              height="25"
              rx="2"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
            />
            <text x="937.5" y="50" text-anchor="middle" class="tg-group-text" font-size="9">
              485
            </text>
            <text x="937.5" y="85" text-anchor="middle" class="tg-group-text">接口2</text>
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const terminals = [
  { num: '13', label: '跳闸常开', line: true },
  { num: '14', label: '跳闸公共', line: true },
  { num: '15', label: '跳闸常闭', line: true },
  { num: '16', label: '报警常开', top: '+', line: true },
  { num: '17', label: '报警公共', top: '-', line: true },
  { num: '18', label: '报警常闭', top: '+', line: true },
  { num: '19', label: '有功校表高', top: '+', line: true },
  { num: '20', label: '无功校表高', top: '-', line: true },
  { num: '21', label: '公共地', top: '+', arrow: true },
  { num: '22', label: '多功能口高', top: '-', line: true },
  { num: '23', label: '多功能口低', top: '+', line: true },
  { num: '24', label: '', top: 'A1' },
  { num: '25', label: '', top: 'B1' },
  { num: '26', label: '公共地', top: '-', arrow: true },
  { num: '27', label: '', top: 'A2' },
  { num: '28', label: '', top: 'B2' }
]
</script>

<style scoped>
/* ─────────────────────────────────────────────
   行尺寸集中管理（cqw = 面板宽 1%，全部相对宽度缩放）
   高度由各行求和自动撑开 → 长宽比例固定，改行高即改比例
   ───────────────────────────────────────────── */
.terminal-guide-panel {
  /* 顶部符号行 */
  --tg-top-h: 3cqw;
  --tg-top-fs: 2.5cqw;
  /* 编号框 */
  --tg-box-size: 4.5cqw;
  /* 边长（正方形宽=高） */
  --tg-box-fs: 2.5cqw;
  /* 箭头/线行 */
  --tg-arrow-h: 2.5cqw;
  /* 竖排名称 */
  --tg-label-h: 10cqw;
  --tg-label-fs: 2cqw;
  /* 485 接口行 */
  --tg-conv-h: 6cqw;
  --tg-conv-fs: 3cqw;
  /* 行间距 / 内衬 */
  --tg-gap: 0.5cqw;
  --tg-pad: 1cqw 1.5cqw 0.8cqw;
  --tg-pad-inner: 0.4cqw 1cqw 0;

  position: relative;
  z-index: 10;
  width: 100%;
  container-type: inline-size;
  background: rgba(171, 168, 158, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 1.5cqw;
  padding: var(--tg-pad);
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
  padding: var(--tg-pad-inner);
  color: #f2f2f2;
  overflow: hidden;
  box-sizing: border-box;
}

.tg-grid {
  display: grid;
  grid-template-columns: repeat(16, 1fr);
  /* 4 行：符号 / 编号框 / 箭头线 / 竖排名称（485 连线为绝对定位覆盖层，不占 grid 行） */
  grid-template-rows: var(--tg-top-h) var(--tg-box-size) var(--tg-arrow-h) var(--tg-label-h);
  gap: var(--tg-gap);
  position: relative;
}

.tg-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.tg-top {
  height: var(--tg-top-h);
  min-height: var(--tg-top-h);
  /* 空内容（13/14/15 无上标）也强制占位，保持各列同行 */
  line-height: var(--tg-top-h);
  font-size: var(--tg-top-fs);
  color: #f2f2f2;
  text-align: center;
  white-space: nowrap;
}

.tg-box {
  width: var(--tg-box-size);
  height: var(--tg-box-size);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(216, 221, 212, 0.6);
  border: 1px solid rgba(90, 99, 90, 0.8);
  border-radius: 0.3cqw;
  color: #1a1a1a;
  font-size: var(--tg-box-fs);
  font-weight: 700;
  box-sizing: border-box;
}

.tg-arrow,
.tg-line {
  height: var(--tg-arrow-h);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f2f2f2;
  pointer-events: none;
}

.tg-arrow svg,
.tg-line svg {
  width: 1.6cqw;
  height: 2.4cqw;
}

.tg-label {
  height: var(--tg-label-h);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--tg-label-fs);
  line-height: 1.25;
  color: #f2f2f2;
  writing-mode: vertical-rl;
  text-orientation: upright;
  letter-spacing: 0.08cqw;
  white-space: nowrap;
  pointer-events: none;
}

/* 485 连线特殊处理：脱离 grid 行，绝对定位——竖线起点对齐端子框底部（--tg-top-h+box+gap），
   高度由 --tg-conv-h 控制（viewBox 1000:60，宽度 100cqw 时 6cqw 为 1:1 不拉伸比例） */
.tg-conv-row {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(var(--tg-top-h) + var(--tg-box-size) + var(--tg-gap));
  height: var(--tg-conv-h);
  color: #f2f2f2;
  pointer-events: none;
  z-index: 1;
}

.tg-conv-row svg {
  width: 100%;
  height: 100%;
  display: block;
  /* 超出 svg 边界的内容（竖线/连线/框）不裁剪，向外显示 */
  overflow: visible;
}

.tg-group-text {
  fill: currentColor;
  font-size: var(--tg-conv-fs);
  font-weight: 500;
  letter-spacing: 0.08cqw;
}
</style>
