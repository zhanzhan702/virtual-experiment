<template>
  <div class="ticket-board">
    <div class="board-heading">
      <img class="work-ticket-sign" :src="Images.workTicketSign" alt="填写工作票" />
      <div class="ticket-tabs" role="tablist" aria-label="工作票类型">
        <button
          v-for="ticket in LOW_TICKETS"
          :id="`tab-${ticket.id}`"
          :key="ticket.id"
          type="button"
          role="tab"
          :aria-selected="data.activeTicket === ticket.id"
          :aria-controls="`paper-${ticket.id}`"
          :class="{ active: data.activeTicket === ticket.id }"
          @click="handleSelectTicket(ticket.id)"
        >
          {{ ticket.label }}
        </button>
      </div>
    </div>
    <div ref="commitRef" class="work-ticket-commit">
      <img class="commit-sign" :src="Images.workTicketCommit" alt="" draggable="false" />
      <img
        ref="ropeRef"
        class="commit-rope"
        :src="Images.workTicketRope"
        alt=""
        draggable="false"
        @load="handleSyncFocus"
      />
      <button
        class="commit-hit-area"
        type="button"
        :disabled="disabled"
        :aria-label="completed ? '已完成' : busy ? '处理中' : '提交'"
        :title="completed ? '已完成' : busy ? '处理中' : '提交'"
        @click.stop="handleSubmit"
      />
      <button
        class="scroll-focus"
        :class="{ dragging: focusDragging, ready: focusReady }"
        :style="{ top: focusTop + 'px' }"
        type="button"
        role="scrollbar"
        aria-label="工作票滚动条"
        aria-orientation="vertical"
        :aria-controls="`paper-${data.activeTicket}`"
        :aria-valuenow="Math.round(focusRatio * 100)"
        :aria-valuemin="0"
        :aria-valuemax="100"
        @pointerdown="handleFocusDown"
        @pointermove="handleFocusMove"
        @pointerup="handleFocusEnd"
        @pointercancel="handleFocusEnd"
        @lostpointercapture="handleFocusEnd"
        @keydown="handleFocusKeydown"
      >
        <img :src="Images.scrollFocus" alt="" draggable="false" />
      </button>
    </div>
    <div class="paper-frame">
      <div class="paper-notice" role="status">
        {{ completed ? '两张工作票已完成' : '注：请填写红色下划线部分，两张工作票均需完成' }}
      </div>
      <div
        v-for="ticket in LOW_TICKETS"
        v-show="data.activeTicket === ticket.id"
        :id="`paper-${ticket.id}`"
        :key="ticket.id"
        :ref="element => setPaperRef(ticket.id, element)"
        class="paper-scroll"
        role="tabpanel"
        :aria-labelledby="`tab-${ticket.id}`"
        @scroll="handleScroll(ticket.id, $event)"
      >
        <el-form
          :model="data.tickets[ticket.id]"
          :disabled="disabled || data.activeTicket !== ticket.id"
          class="ticket-paper"
          @submit.prevent
        >
          <header class="paper-header">
            <div class="company-heading">
              国网福建
              <span
                :data-field="'company'"
                class="company-field"
                :class="{ invalid: hasError(ticket.id, 'company') }"
              >
                <el-input
                  v-model="data.tickets[ticket.id].company"
                  :aria-label="`${ticket.label}单位`"
                  @change="handleOperation"
                />
              </span>
              供电公司
            </div>
            <div class="paper-title">配电第二种工作票</div>
            <div class="ticket-number">
              编号：<span class="fixed-line">{{ ticket.ticketNo }}</span>
            </div>
          </header>
          <div class="team-row">
            <span>1、部门：<span class="fixed-line">营销部</span></span>
            <label
              >工作班组：<span data-field="team"
                ><LTicketAnswer
                  v-model="data.tickets[ticket.id].team"
                  :options="ticket.options.team"
                  :label="`${ticket.label}工作班组`"
                  :disabled="disabled || data.activeTicket !== ticket.id"
                  :invalid="hasError(ticket.id, 'team')"
                  @update:model-value="handleOperation" /></span
            ></label>
            <label
              >工作负责人：<span data-field="leader"
                ><LTicketAnswer
                  v-model="data.tickets[ticket.id].leader"
                  :options="ticket.options.leader"
                  :label="`${ticket.label}工作负责人`"
                  :disabled="disabled || data.activeTicket !== ticket.id"
                  :invalid="hasError(ticket.id, 'leader')"
                  @update:model-value="handleOperation" /></span
            ></label>
          </div>
          <section>
            <p>2、工作班人员（不包括工作负责人）</p>
            <div class="member-row">
              <span
                data-field="member1"
                class="member-field"
                :class="{ invalid: hasError(ticket.id, 'member1') }"
              >
                <el-input
                  v-model="data.tickets[ticket.id].member1"
                  :aria-label="`${ticket.label}第一位工作班人员`"
                  @change="handleOperation"
                />
              </span>
              <span>、</span>
              <span data-field="member2" class="member-field"
                ><LTicketAnswer
                  v-model="data.tickets[ticket.id].member2"
                  :options="ticket.options.member2"
                  :label="`${ticket.label}第二位工作班人员`"
                  :disabled="disabled || data.activeTicket !== ticket.id"
                  :invalid="hasError(ticket.id, 'member2')"
                  @update:model-value="handleOperation"
              /></span>
              <span>共 <span class="fixed-line">2</span> 人</span>
            </div>
          </section>
          <section>
            <p>3、工作的变配电站名称及设备双重名称</p>
            <div class="fixed-line block-line">{{ ticket.location }}</div>
          </section>
          <section>
            <p>4、工作任务</p>
            <table class="task-table">
              <thead>
                <tr>
                  <th>工作地点或地段</th>
                  <th>工作内容</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <span
                      v-for="line in ticket.taskLocationLines"
                      :key="line"
                      class="task-location-line"
                      >{{ line }}</span
                    >
                  </td>
                  <td>{{ ticket.task }}</td>
                </tr>
              </tbody>
            </table>
          </section>
          <section>
            <p>5、计划工作时间</p>
            <p class="indented date-row">
              <span>自</span><LTicketDate :value="data.tickets[ticket.id].planStart" />
            </p>
            <p class="indented date-row">
              <span>至</span><LTicketDate :value="data.tickets[ticket.id].planEnd" />
            </p>
          </section>
          <section>
            <p>6、工作条件（停电或不停电，或邻近及保留带电设备名称）</p>
            <div class="fixed-line block-line">不停电</div>
          </section>
          <section>
            <p>7、注意事项（安全措施）</p>
            <div data-field="safetyMeasures" class="measure-field">
              <LTicketAnswer
                v-model="data.tickets[ticket.id].safetyMeasures"
                :options="ticket.options.safetyMeasures"
                :label="`${ticket.label}注意事项`"
                :multiple="true"
                :disabled="disabled || data.activeTicket !== ticket.id"
                :invalid="hasError(ticket.id, 'safetyMeasures')"
                @update:model-value="handleOperation"
              />
            </div>
            <p class="indented">工作票签发人签名 <span class="fixed-line signature">李强</span></p>
            <p class="indented date-row">
              <span>签发日期</span><LTicketDate :value="data.tickets[ticket.id].signDate" />
            </p>
          </section>
          <section>
            <p>8、补充安全措施（工作许可人填写）</p>
            <div class="fixed-line block-line">无</div>
          </section>
          <section>
            <p>9、确认本工作票1-8项</p>
            <p class="indented date-row"><span>许可工作时间</span><LTicketDate /></p>
            <p class="indented">工作负责人签名 <span class="blank signature" /></p>
            <p class="indented">
              工作许可人签名 <span class="blank signature" />、<span class="blank signature" />
            </p>
          </section>
          <section>
            <p>10、本次工作危险点分析及防范措施（由工作负责人填写）</p>
            <table class="task-table danger-table">
              <colgroup>
                <col class="number-column" />
                <col class="type-column" />
                <col class="danger-column" />
                <col />
              </colgroup>
              <thead>
                <tr>
                  <th>序号</th>
                  <th>防范类型</th>
                  <th>危险点</th>
                  <th>预防控制措施</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td rowspan="2">1</td>
                  <td rowspan="2">人身触电<br />与伤害</td>
                  <td>走错<br />工作间隔</td>
                  <td data-field="dangerInterval">
                    <LTicketAnswer
                      v-model="data.tickets[ticket.id].dangerInterval"
                      :options="ticket.options.dangerInterval"
                      :label="`${ticket.label}走错工作间隔防范措施`"
                      :multiple="true"
                      :disabled="disabled || data.activeTicket !== ticket.id"
                      :invalid="hasError(ticket.id, 'dangerInterval')"
                      @update:model-value="handleOperation"
                    />
                  </td>
                </tr>
                <tr>
                  <td>短路或<br />接地</td>
                  <td data-field="dangerShortCircuit">
                    <LTicketAnswer
                      v-model="data.tickets[ticket.id].dangerShortCircuit"
                      :options="ticket.options.dangerShortCircuit"
                      :label="`${ticket.label}短路或接地防范措施`"
                      :multiple="true"
                      :disabled="disabled || data.activeTicket !== ticket.id"
                      :invalid="hasError(ticket.id, 'dangerShortCircuit')"
                      @update:model-value="handleOperation"
                    />
                  </td>
                </tr>
              </tbody>
            </table>
          </section>
          <section>
            <p>11、确认工作负责人布置的任务和本施工项目安全措施</p>
            <p class="indented">
              工作班人员签名
              <span class="fixed-line signature"
                >{{ data.tickets[ticket.id].member1 || '　　' }}、{{
                  data.tickets[ticket.id].member2 || '　　'
                }}</span
              >
            </p>
          </section>
          <section>
            <p>12、工作票延期</p>
            <p class="indented date-row">
              <span>有效期延长到</span><LTicketDate :slashed="true" />
            </p>
            <p class="indented">工作负责人签名 <span class="fixed-line signature">／</span></p>
            <p class="indented date-row"><LTicketDate :slashed="true" /></p>
            <p class="indented">工作许可人签名 <span class="fixed-line signature">／、／</span></p>
            <p class="indented date-row"><LTicketDate :slashed="true" /></p>
          </section>
          <section>
            <p>13、工作票终结</p>
            <p class="indented">
              <span class="date-row"
                ><span>全部工作于</span><LTicketDate /><span>结束，</span></span
              >
              工作人员已全部撤离，材料工具已清理完毕。
            </p>
            <p class="indented">工作负责人签名 <span class="blank signature" /></p>
            <p class="indented date-row"><LTicketDate /></p>
            <p class="indented">
              工作许可人签名 <span class="blank signature" />、<span class="blank signature" />
            </p>
            <p class="indented date-row"><LTicketDate /></p>
          </section>
          <p>备注：　无</p>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import Images from '@/constants/images'
import LTicketAnswer from '@/components/LowVoltage/LTicketAnswer.vue'
import LTicketDate from '@/components/LowVoltage/LTicketDate.vue'
import { LOW_TICKETS, createLowTicketData, validateLowTickets } from '@/constants/low-work-ticket'

const props = defineProps({
  playerName: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  paused: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
  completed: { type: Boolean, default: false }
})
const emit = defineEmits(['submit-ticket'])
const data = reactive(createLowTicketData(props.playerName))
const stats = reactive({ duration_seconds: 0, operation_count: 0, error_count: 0 })
const errors = ref([])
const paperRefs = {}
let initialDuration = 0
let timer
const commitRef = ref(null)
const ropeRef = ref(null)
const focusTop = ref(0)
const focusRatio = ref(0)
const focusReady = ref(false)
const focusDragging = ref(false)
let resizeObserver
let dragStartY = 0
let dragStartRatio = 0
// 复用高压素材的几何：绳高 431，滚轮从绳顶下 6 到下 388 的位置运动。
const FOCUS_START_RATIO = 6 / 431
const FOCUS_END_RATIO = 388 / 431

function handleSyncFocus() {
  const paper = paperRefs[data.activeTicket]
  const rope = ropeRef.value
  if (!paper || !rope?.clientHeight) return
  const max = paper.scrollHeight - paper.clientHeight
  focusRatio.value = max > 0 ? Math.min(1, Math.max(0, paper.scrollTop / max)) : 0
  focusTop.value =
    rope.offsetTop +
    rope.clientHeight *
      (FOCUS_START_RATIO + focusRatio.value * (FOCUS_END_RATIO - FOCUS_START_RATIO))
  focusReady.value = true
}
function handleFocusDown(event) {
  if (event.button !== 0) return
  event.preventDefault()
  event.currentTarget.focus({ preventScroll: true })
  event.currentTarget.setPointerCapture(event.pointerId)
  focusDragging.value = true
  dragStartY = event.clientY
  dragStartRatio = focusRatio.value
}
function handleFocusMove(event) {
  if (!focusDragging.value) return
  const paper = paperRefs[data.activeTicket]
  const travel = ropeRef.value.clientHeight * (FOCUS_END_RATIO - FOCUS_START_RATIO)
  if (!paper || travel <= 0) return
  const ratio = Math.min(1, Math.max(0, dragStartRatio + (event.clientY - dragStartY) / travel))
  paper.scrollTop = ratio * (paper.scrollHeight - paper.clientHeight)
  handleSyncFocus()
}
function handleFocusEnd() {
  focusDragging.value = false
}
function handleFocusKeydown(event) {
  const paper = paperRefs[data.activeTicket]
  if (!paper) return
  const positions = {
    ArrowDown: paper.scrollTop + 40,
    ArrowUp: paper.scrollTop - 40,
    PageDown: paper.scrollTop + paper.clientHeight,
    PageUp: paper.scrollTop - paper.clientHeight,
    Home: 0,
    End: paper.scrollHeight
  }
  if (!(event.key in positions)) return
  event.preventDefault()
  paper.scrollTop = positions[event.key]
  handleSyncFocus()
}

function setPaperRef(id, element) {
  paperRefs[id] = element
}
function hasError(ticket, field) {
  return errors.value.some(error => error.ticket === ticket && error.field === field)
}
function handleOperation() {
  stats.operation_count++
}
function handleScroll(id, event) {
  data.scrollPositions[id] = event.target.scrollTop
  if (id === data.activeTicket) handleSyncFocus()
}
async function handleSelectTicket(id) {
  data.activeTicket = id
  await nextTick()
  if (paperRefs[id]) paperRefs[id].scrollTop = data.scrollPositions[id] || 0
  handleSyncFocus()
}
async function handleSubmit() {
  if (props.disabled) return
  errors.value = validateLowTickets(data)
  if (errors.value.length) {
    stats.error_count++
    const first = errors.value[0]
    await handleSelectTicket(first.ticket)
    const field = paperRefs[first.ticket]?.querySelector(`[data-field="${first.field}"]`)
    field?.scrollIntoView({ block: 'center' })
    field?.querySelector('input, button')?.focus({ preventScroll: true })
    emit('submit-ticket', {
      success: false,
      errors: [...errors.value],
      errorCount: stats.error_count
    })
    return
  }
  emit('submit-ticket', { success: true, data: getSnapshot(), stats: { ...stats } })
}

/** 恢复数据不触发操作统计，并在 DOM 更新后恢复各票滚动位置。 */
async function restore(snapshot) {
  for (const ticket of LOW_TICKETS) {
    const saved = snapshot.tickets?.[ticket.id]
    if (!saved) continue
    const form = data.tickets[ticket.id]
    for (const key of Object.keys(form)) {
      if (Array.isArray(form[key]) && Array.isArray(saved[key])) form[key] = [...saved[key]]
      else if (typeof form[key] === 'string' && typeof saved[key] === 'string')
        form[key] = saved[key]
    }
  }
  for (const key of Object.keys(stats)) {
    stats[key] = Math.max(0, Number(snapshot.stats?.[key]) || 0)
  }
  initialDuration = stats.duration_seconds
  for (const ticket of LOW_TICKETS) {
    data.scrollPositions[ticket.id] = Math.max(
      0,
      Number(snapshot.scrollPositions?.[ticket.id]) || 0
    )
  }
  await handleSelectTicket(
    LOW_TICKETS.some(t => t.id === snapshot.activeTicket) ? snapshot.activeTicket : 'concentrator'
  )
}
/** 返回与当前界面一致的完整双票快照。 */
function getSnapshot() {
  return JSON.parse(JSON.stringify({ ...data, stats: { ...stats } }))
}
function getSessionSeconds() {
  return stats.duration_seconds - initialDuration
}

onMounted(() => {
  resizeObserver = new ResizeObserver(handleSyncFocus)
  resizeObserver.observe(commitRef.value)
  for (const paper of Object.values(paperRefs)) {
    resizeObserver.observe(paper)
    if (paper.firstElementChild) resizeObserver.observe(paper.firstElementChild)
  }
  handleSyncFocus()
  timer = setInterval(() => {
    if (!props.paused) stats.duration_seconds++
  }, 1000)
})
onUnmounted(() => {
  clearInterval(timer)
  resizeObserver?.disconnect()
})
defineExpose({ getSnapshot, restore, getSessionSeconds })
</script>

<style scoped>
.ticket-board {
  width: min(76vw, 1150px);
  position: relative;
  color: #123f68;
}
.board-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 7% 0 4%;
  gap: 2%;
}
.work-ticket-sign {
  width: clamp(130px, 28%, 320px);
  height: auto;
  display: block;
  margin-bottom: -8px;
  position: relative;
  z-index: 2;
}
.ticket-tabs {
  display: flex;
  gap: 4px;
}
.ticket-tabs button {
  color: #008c83;
  border: 5px solid #078c83;
  border-bottom: 0;
  border-radius: 25px 25px 0 0;
  padding: 0.35em 0.6em;
  background: #dcece8;
  font-family: inherit;
  font-size: clamp(18px, 2.1vw, 32px);
  font-weight: 900;
  cursor: pointer;
}
.ticket-tabs button.active {
  background: white;
  transform: translateY(6px);
  padding-bottom: calc(0.35em + 6px);
  position: relative;
  z-index: 2;
}
.paper-frame {
  border: clamp(6px, 0.7vw, 12px) solid #008d85;
  border-radius: 28px;
  background: #fff;
  box-shadow:
    0 0 0 4px #0b5a60,
    inset 0 0 0 3px #80d3c9,
    0 18px 25px #163f6533;
  overflow: hidden;
  padding: 0 1.4% 1%;
}
.paper-notice {
  color: #f22;
  font-size: clamp(13px, 1.4vw, 21px);
  font-weight: bold;
  padding: 0.6em 0.2em 0.3em;
  min-height: 2em;
}
.paper-scroll {
  height: 61vh;
  overflow-y: auto;
  overscroll-behavior: contain;
  container-type: inline-size;
}
/* 与 fix 分支高压一致：隐藏原生滚动条，由绳上的滚轮控制正文。 */
.paper-scroll {
  scrollbar-width: none;
}
.paper-scroll::-webkit-scrollbar {
  display: none;
}
/* 复用高压提交牌/金属钩、绳与滚轮素材；内部横向坐标沿用 140px 设计稿。 */
.work-ticket-commit {
  position: absolute;
  top: 7%;
  right: -2.4%;
  z-index: 11;
  width: clamp(72px, 12.2%, 140px);
  height: 78%;
  container-type: inline-size;
  pointer-events: none;
}
.commit-sign {
  position: absolute;
  left: 22.857%;
  top: 2.857cqw;
  width: 77.143%;
  height: auto;
  user-select: none;
}
.commit-rope {
  position: absolute;
  left: 44.286%;
  top: 81.429cqw;
  bottom: 2.7%;
  height: calc(97.3% - 81.429cqw);
  width: auto;
  user-select: none;
}
.commit-hit-area {
  position: absolute;
  left: 32.857%;
  top: 11.429cqw;
  width: 57.143%;
  height: 51.429cqw;
  border: 0;
  padding: 0;
  background: transparent;
  pointer-events: auto;
  cursor: pointer;
}
.commit-hit-area:disabled {
  cursor: default;
}
.commit-hit-area:focus-visible {
  outline: 3px solid #078c83;
  border-radius: 8px;
}
.scroll-focus {
  position: absolute;
  left: 40.714%;
  width: 20.357%;
  border: 0;
  padding: 0;
  background: transparent;
  cursor: grab;
  pointer-events: auto;
  touch-action: none;
  visibility: hidden;
  user-select: none;
}
.scroll-focus img {
  display: block;
  width: 100%;
  height: auto;
}
.scroll-focus.ready {
  visibility: visible;
}
.scroll-focus.dragging {
  cursor: grabbing;
}
.scroll-focus:focus-visible {
  outline: 2px solid #078c83;
}
.ticket-paper {
  padding: 0.2em 3% 1.4em;
  /* 字号依据纸张容器缩放，保持不同窗口下的表单比例。 */
  font-size: clamp(14px, 3.1cqw, 32px);
  line-height: 1.65;
  font-family: 'Microsoft YaHei', 'Noto Sans CJK SC', sans-serif;
  color: #123f68;
}
.paper-header {
  margin-bottom: 0.35em;
}
.company-heading {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 0.15em;
  font-size: 1.35em;
  line-height: 1.3;
  white-space: nowrap;
}
.company-field {
  display: inline-flex;
  width: 20%;
  border-bottom: 1.5px solid #ff454f;
}
.paper-title {
  text-align: center;
  font-size: 1.12em;
  line-height: 1.4;
  margin: 0.45em 0 0.3em;
}
.ticket-number {
  text-align: right;
  line-height: 1.5;
}
.fixed-line,
.blank {
  display: inline-block;
  border-bottom: 1px solid #315e83;
  min-width: 2em;
  padding: 0 0.25em;
  line-height: 1.3;
}
.ticket-number .fixed-line {
  width: 23%;
  min-width: 5em;
  text-align: center;
}
.team-row {
  display: grid;
  grid-template-columns: 27fr 34fr 39fr;
  column-gap: 0.65em;
  align-items: end;
  margin: 0.35em 0;
}
.team-row > span {
  white-space: nowrap;
  line-height: 1.3;
}
.team-row label {
  display: flex;
  align-items: flex-end;
  line-height: 1.3;
  min-width: 0;
  white-space: nowrap;
}
.team-row label > span {
  display: flex;
  min-width: 0;
  flex: 1;
}
section {
  margin: 0.45em 0;
}
p {
  margin: 0.35em 0;
}
.member-row {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 0.25em;
  padding-left: 1em;
}
.member-field {
  width: 20%;
  min-width: 4em;
  display: inline-flex;
  height: 1.3em;
  align-items: flex-end;
}
.team-row :deep(.answer-line),
.member-row :deep(.answer-line) {
  min-height: 1.3em;
  height: 1.3em;
  padding: 0 0.15em;
  line-height: 1.3;
}
.member-field:has(.el-input) {
  border-bottom: 1.5px solid #ff454f;
}
.member-row > span:last-child {
  margin-left: 0.6em;
  white-space: nowrap;
}
.block-line {
  display: block;
  margin: 0.3em 0 0.25em 1em;
}
.indented {
  margin-left: 1.6em;
}
.signature {
  min-width: 5.5em;
  text-align: center;
}
.blank {
  height: 1.25em;
}
.date-row {
  display: flex;
  align-items: baseline;
  gap: 0.25em;
}
.date-row > span:not(.ticket-date) {
  flex-shrink: 0;
}
.date-row > .ticket-date {
  flex: 1;
}
.task-table {
  width: calc(100% - 1em);
  border-collapse: collapse;
  table-layout: fixed;
  margin: 0.25em 0 0.55em 1em;
}
.task-table th,
.task-table td {
  border: 1px solid #315e83;
  padding: 0.35em 0.5em;
  font-weight: normal;
  text-align: center;
  overflow-wrap: anywhere;
}
.task-table tbody td {
  padding-top: 0.65em;
  padding-bottom: 0.65em;
}
.task-location-line {
  display: block;
}
.measure-field {
  margin: 0.4em 0 0.4em 1.6em;
}
/* 安全措施保留原图的三条红线，选中后正文直接落在对应横线上。 */
.measure-field :deep(.answer-line) {
  min-height: 5.4em;
  line-height: 1.8;
  padding: 0;
  border-bottom: 0;
  background-image: repeating-linear-gradient(
    to bottom,
    transparent 0,
    transparent calc(1.8em - 1.5px),
    #ff454f calc(1.8em - 1.5px),
    #ff454f 1.8em
  );
}
.measure-field :deep(.answer-text > span) {
  min-height: 1.8em;
}
.danger-table {
  font-size: 1em;
}
.number-column {
  width: 10%;
}
.type-column {
  width: 18%;
}
.danger-column {
  width: 14%;
}
.danger-table tbody tr {
  height: 17em;
}
.danger-table td:last-child {
  text-align: left;
  padding: 0;
}
.danger-table :deep(.answer-line) {
  min-height: 17em;
  display: flex;
  align-items: flex-end;
  padding: 0.4em 0.5em;
}
.danger-table :deep(.answer-text) {
  width: 100%;
}
.invalid {
  background-color: #fff1ed;
}
:deep(.el-input) {
  font-size: inherit;
  line-height: inherit;
}
:deep(.el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  border: 0;
  padding: 0 0.2em;
}
:deep(.el-input__inner) {
  color: #123f68 !important;
  -webkit-text-fill-color: #123f68;
  font-family: inherit;
  font-size: inherit;
  height: 1.3em;
  line-height: 1.3;
  text-align: center;
}
.company-heading :deep(.el-input__inner) {
  font-size: inherit;
}
@container (max-width: 600px) {
  .team-row {
    grid-template-columns: 1fr;
    row-gap: 0.4em;
  }
  .team-row label {
    width: 100%;
  }
  .company-heading {
    font-size: 1.3em;
  }
  .danger-table {
    font-size: 0.9em;
  }
  .date-row {
    flex-wrap: wrap;
  }
  .date-row > .ticket-date {
    min-width: 12em;
  }
}
@media (max-width: 800px) {
  .ticket-board {
    width: 90vw;
  }
  .board-heading {
    padding: 0 4% 0 0;
  }
  .ticket-tabs button {
    font-size: clamp(14px, 3vw, 22px);
    border-width: 3px;
  }
  .paper-scroll {
    height: 64vh;
  }
}
@media (max-height: 600px) {
  .paper-scroll {
    height: 56vh;
  }
  .ticket-tabs button {
    font-size: 20px;
  }
}
</style>
