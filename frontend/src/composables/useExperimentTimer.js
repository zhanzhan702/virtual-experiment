/**
 * 实验全局计时器 composable
 *
 * 数据来源（不新建时间变量，复用已有来源）：
 *   1. 后端 GET /{experimentId}/duration → 已提交步骤累计秒数
 *   2. 当前步骤页的 stats.duration_seconds（通过参数传入 ref）
 *
 * 用法：
 *   import { useExperimentTimer } from '@/composables/useExperimentTimer'
 *   const { formatted } = useExperimentTimer(experimentIdRef, currentStepSecondsRef)
 */
import { ref, computed, onMounted } from 'vue'
import { getTotalDuration } from '@/api/experiment'

export function useExperimentTimer(experimentId, currentStepSeconds) {
  // 已提交步骤累计秒数（来自后端）
  const accumulatedSeconds = ref(0)
  const loading = ref(false)

  async function fetchAccumulated() {
    if (!experimentId.value) return
    loading.value = true
    try {
      const res = await getTotalDuration(experimentId.value)
      // 后端返回 { durationSeconds: number }
      accumulatedSeconds.value = Number(res?.durationSeconds ?? 0)
    } catch (_) {
      accumulatedSeconds.value = 0
    } finally {
      loading.value = false
    }
  }

  onMounted(fetchAccumulated)

  // 总秒数 = 已提交累计 + 当前步骤实时
  const totalSeconds = computed(
    () => accumulatedSeconds.value + (Number(currentStepSeconds.value) || 0)
  )

  // 格式化 HH:MM:SS
  const formatted = computed(() => {
    const s = totalSeconds.value
    const h = Math.floor(s / 3600)
    const m = Math.floor((s % 3600) / 60)
    const sec = s % 60
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
  })

  return { totalSeconds, formatted, loading, refresh: fetchAccumulated }
}
