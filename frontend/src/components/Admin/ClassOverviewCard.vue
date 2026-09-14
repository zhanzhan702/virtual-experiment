<template>
  <div v-loading="loading" class="class-overview">
    <!-- 第一排：人数与三项完成率 -->
    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">班级人数</div>
        <div class="stat-value">{{ data?.studentCount ?? '—' }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">高压完成率</div>
        <div class="stat-value">{{ rateText(data?.highDoneRate) }}</div>
        <div class="stat-sub">{{ data?.highDone ?? 0 }} / {{ data?.studentCount ?? 0 }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">低压完成率</div>
        <div class="stat-value">{{ rateText(data?.lowDoneRate) }}</div>
        <div class="stat-sub">{{ data?.lowDone ?? 0 }} / {{ data?.studentCount ?? 0 }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">高、低压均完成</div>
        <div class="stat-value">{{ rateText(data?.overallDoneRate) }}</div>
      </div>
    </div>

    <!-- 第二排：分数。口径与班级汇总表的平均分一致（高、低压分混在一起统计） -->
    <div class="stat-row">
      <div class="stat">
        <div class="stat-label">平均分</div>
        <div class="stat-value">{{ scoreText(data?.avgScore) }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">最高分</div>
        <div class="stat-value">{{ scoreText(data?.maxScore) }}</div>
      </div>
      <div class="stat">
        <div class="stat-label">最低分</div>
        <div class="stat-value">{{ scoreText(data?.minScore) }}</div>
      </div>
    </div>

    <!-- 分数段分布：纯 CSS 横条，不引图表库 -->
    <div class="section">
      <div class="section-title">分数段分布</div>

      <template v-if="hasScores">
        <div v-for="bucket in data.buckets" :key="bucket.label" class="bucket">
          <span class="bucket-label">{{ bucket.label }}</span>
          <div class="bucket-bar">
            <div class="bucket-fill" :style="{ width: barWidth(bucket) }" />
          </div>
          <span class="bucket-count">{{ bucket.count }}（{{ bucket.ratio ?? 0 }}%）</span>
        </div>
      </template>

      <p v-else class="empty-tip">暂无有效分数</p>
    </div>

    <!-- 未完成名单 -->
    <div v-if="unfinished.length" class="section">
      <div class="section-title">未完成（{{ unfinished.length }} 人）</div>
      <div class="unfinished-list">
        <el-tag
          v-for="item in unfinished"
          :key="item.userId"
          type="warning"
          size="small"
          effect="plain"
        >
          {{ item.name }}<span class="missing">{{ missingText(item) }}</span>
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** ClassOverviewVO；未加载时为 null */
  data: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

const unfinished = computed(() => props.data?.unfinished || [])

/** 有分数才画分布条 —— 全班无人完成时画五根空条只会让人以为「全是 0 分」 */
const hasScores = computed(() => (props.data?.buckets || []).some(b => (b.count || 0) > 0))

/** 完成率为 null 表示班级里没人（后端刻意区分「0%」与「不适用」） */
function rateText(rate) {
  return rate == null ? '—' : `${rate}%`
}

function scoreText(score) {
  return score == null ? '—' : score
}

/**
 * 条长按「最大档」归一，而不是按百分比。
 * 用百分比的话，一个 60 人的班若某档只有 2 人（3%）条子短到看不见，档间差异分辨不出来。
 */
function barWidth(bucket) {
  const counts = (props.data?.buckets || []).map(b => b.count || 0)
  const max = Math.max(...counts, 1)
  return `${Math.round(((bucket.count || 0) / max) * 100)}%`
}

function missingText(item) {
  if (item.missingHigh && item.missingLow) return '（高压、低压均未完成）'
  return item.missingHigh ? '（缺高压）' : '（缺低压）'
}
</script>

<style scoped>
.class-overview {
  padding-bottom: 4px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--admin-border);
}

.stat-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.stat {
  flex: 1 1 120px;
  min-width: 0;
  padding: 10px 14px;
  background: var(--admin-table-head-bg);
  border-radius: var(--admin-radius);
}

.stat-label {
  color: var(--admin-text-muted);
  font-size: 12px;
}

.stat-value {
  margin-top: 4px;
  color: var(--admin-text);
  font-size: 20px;
  font-weight: 600;
  line-height: 1.2;
}

.stat-sub {
  margin-top: 2px;
  color: var(--admin-text-muted);
  font-size: 12px;
}

.section {
  margin-top: 16px;
}

.section-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--admin-text);
}

.bucket {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 22px;
}

.bucket-label {
  flex: 0 0 96px;
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.bucket-bar {
  flex: 1;
  min-width: 0;
  height: 10px;
  background: var(--admin-table-head-bg);
  border-radius: 5px;
  overflow: hidden;
}

.bucket-fill {
  height: 100%;
  background: var(--admin-primary);
  border-radius: 5px;
  transition: width 0.3s;
}

.bucket-count {
  flex: 0 0 72px;
  text-align: right;
  color: var(--admin-text-muted);
  font-size: 12px;
}

.unfinished-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 96px;
  overflow-y: auto;
}

.missing {
  opacity: 0.75;
}

.empty-tip {
  margin: 0;
  color: var(--admin-text-muted);
  font-size: 12px;
}
</style>
