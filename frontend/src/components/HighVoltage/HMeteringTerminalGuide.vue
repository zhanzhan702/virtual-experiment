<!-- 计量小室终端编号提示面板 —— 按参考图 1:1 复刻 -->
<template>
  <div class="terminal-guide-panel">
    <div class="replica">
      <!-- 16 列端子编号 -->
      <div class="tg-grid">
        <div
          v-for="item in terminals"
          :key="item.num"
          class="tg-col"
          :class="{ 'has-arrow': item.arrow, 'has-group': item.group }"
        >
          <div class="tg-top">{{ item.top || '' }}</div>
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
          <div class="tg-label" :class="{ 'tg-label-empty': !item.label }">
            {{ item.label }}
          </div>
        </div>
        <!-- 485 接口分组：双端子连线汇聚到端口，端口标 485，下方标 接口N -->
        <div class="tg-conv-row">
          <svg viewBox="0 0 1000 60" preserveAspectRatio="none" aria-hidden="true">
            <!-- 接口1：端子 24(中心718.75) + 25(中心781.25) 汇聚到中点750 -->
            <!-- 竖线24 → 横线 → 竖线25（倒U形），再从中点竖线到端口 -->
            <path
              d="M718.75 2 L718.75 14 L781.25 14 L781.25 2"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
            />
            <line x1="750" y1="14" x2="750" y2="30" stroke="currentColor" stroke-width="1.2" />
            <rect x="735" y="30" width="30" height="14" rx="2" fill="none" stroke="currentColor" stroke-width="1.2" />
            <text x="750" y="40" text-anchor="middle" class="tg-group-text" font-size="9">485</text>
            <text x="750" y="55" text-anchor="middle" class="tg-group-text">接口1</text>

            <!-- 接口2：端子 27(中心906.25) + 28(中心968.75) 汇聚到中点937.5 -->
            <path
              d="M906.25 2 L906.25 14 L968.75 14 L968.75 2"
              fill="none"
              stroke="currentColor"
              stroke-width="1.2"
            />
            <line x1="937.5" y1="14" x2="937.5" y2="30" stroke="currentColor" stroke-width="1.2" />
            <rect x="922.5" y="30" width="30" height="14" rx="2" fill="none" stroke="currentColor" stroke-width="1.2" />
            <text x="937.5" y="40" text-anchor="middle" class="tg-group-text" font-size="9">485</text>
            <text x="937.5" y="55" text-anchor="middle" class="tg-group-text">接口2</text>
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
.terminal-guide-panel {
  position: relative;
  z-index: 10;
  width: 133%;
  background: rgba(171, 168, 158, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 8px;
  padding: 6px 8px 4px;
  backdrop-filter: blur(2px);
  pointer-events: none;
  box-sizing: border-box;
}

.replica {
  position: relative;
  background: #9ba89b;
  border-radius: 4px;
  padding: 10px 8px 6px;
  color: #f2f2f2;
  overflow: hidden;
}

.tg-grid {
  display: grid;
  grid-template-columns: repeat(16, 1fr);
  gap: 4px;
  position: relative;
  padding-bottom: 24px;
}

.tg-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.tg-top {
  height: 14px;
  line-height: 14px;
  font-size: 10px;
  color: #f2f2f2;
  text-align: center;
  white-space: nowrap;
  margin-bottom: 2px;
}

.tg-box {
  width: 65%;
  aspect-ratio: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(216, 221, 212, 0.6);
  border: 1px solid rgba(90, 99, 90, 0.8);
  border-radius: 2px;
  color: #1a1a1a;
  font-size: clamp(13px, 2.2vw, 19px);
  font-weight: 700;
  box-sizing: border-box;
}

.tg-arrow {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-top: 2px;
  color: #f2f2f2;
  pointer-events: none;
}

.tg-line {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-top: 2px;
  color: #f2f2f2;
  pointer-events: none;
}

.tg-label {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-top: 24px;
  font-size: clamp(10px, 1.3vw, 13px);
  line-height: 1.25;
  color: #f2f2f2;
  writing-mode: vertical-rl;
  text-orientation: upright;
  letter-spacing: 0.5px;
  white-space: nowrap;
  min-height: 56px;
  pointer-events: none;
}

.tg-label-empty {
  min-height: 0;
}

.tg-conv-row {
  grid-column: 1 / -1;
  width: 100%;
  height: 60px;
  margin-top: 0;
  color: #f2f2f2;
  pointer-events: none;
}

.tg-conv-row svg {
  width: 100%;
  height: 100%;
  display: block;
}

.tg-group-text {
  fill: currentColor;
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.5px;
}
</style>
