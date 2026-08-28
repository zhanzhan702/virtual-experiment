<template>
  <div class="ticket-paper">
    <el-form ref="formRef" :model="formData" class="paper-form">
      <!-- 表头 -->
      <div class="paper-header">
        <span class="paper-title">国网福建</span>
        <span class="inline-input">
          <el-input v-model="formData.company" placeholder="" size="default" />
        </span>
        <span class="paper-title">供电公司</span>
        <span class="paper-right">配电第二种工作票</span>
      </div>
      <div class="paper-subheader">
        <span class="paper-label">编号：</span>
        <span class="inline-input short">
          <el-input v-model="formData.ticketNo" placeholder="" size="default" />
        </span>
      </div>

      <!-- 第一行：部门 + 工作班组 + 工作负责人（还原工作票） -->
      <div class="form-line-row triple">
        <div class="form-line">
          <span class="line-label">1、部门：</span>
          <span class="static-val">营销部</span>
        </div>
        <div class="form-line">
          <span class="line-label">工作班组：</span>
          <span class="inline-input">
            <el-select v-model="formData.team" placeholder="" size="default">
              <el-option label="修试班" value="修试班" />
              <el-option label="装表班" value="装表班" />
              <el-option label="运维班" value="运维班" />
            </el-select>
          </span>
        </div>
        <div class="form-line">
          <span class="line-label">工作负责人：</span>
          <span class="inline-input">
            <el-select v-model="formData.leader" placeholder="" size="default">
              <el-option label="张亮" value="张亮" />
              <el-option label="王五" value="王五" />
              <el-option label="张三" value="张三" />
              <el-option label="赵六" value="赵六" />
            </el-select>
          </span>
        </div>
      </div>

      <!-- 第二行：工作班人员（不包括工作负责人）：共2人 -->
      <div class="form-line-row triple">
        <div class="form-noline">
          <span class="line-label">2、工作班人员（不包括工作负责人）</span>
        </div>

        <div class="form-line">
          <span class="inline-input">
            <el-input v-model="formData.member1" placeholder="" size="default" />
          </span>
        </div>
        <div class="form-line">
          <span class="line-label">、</span>
          <span class="inline-input">
            <el-select v-model="formData.member2" placeholder="" size="default">
              <el-option label="张三" value="张三" />
              <el-option label="赵六" value="赵六" />
              <el-option label="张亮" value="张亮" />
              <el-option label="王五" value="王五" />
            </el-select>
          </span>
        </div>

        <div class="form-line">
          <span class="line-label">共</span>
          <span class="static-val">2</span>
          <span class="line-label">人</span>
        </div>
      </div>

      <!-- 第3点 -->
      <div>
        <div class="form-noline">
          <span class="line-label"> 3、工作的变配电站名称及设备双重名称 </span>
        </div>
        <div class="static-val">福州市台江区鳌峰路雪花水泥厂配电室计量柜</div>
      </div>

      <!-- 第4点 -->
      <div class="form-noline section-brk">4、工作任务</div>

      <table class="task-table">
        <thead>
          <tr>
            <th style="width: 70%">工作地点或地段</th>
            <th>工作内容</th>
          </tr>
        </thead>

        <tbody>
          <tr>
            <td>福州市台江区鳌峰路雪花水泥厂配电室计量柜</td>
            <td>专变终端安装与调试</td>
          </tr>

          <tr>
            <td>福州市台江区鳌峰路雪花水泥厂配电室计量柜</td>
            <td>三相电能表安装</td>
          </tr>
        </tbody>
      </table>

      <!-- 第5点 -->
      <div>
        <span class="line-label">5、计划工作时间：</span>
        <span
          >自 <span class="static-val">{{ planStart }}</span> 至
          <span class="static-val">{{ planEnd }}</span></span
        >
      </div>

      <!-- 第6点 -->
      <div class="form-noline">
        <div class="line-label">6、工作条件（停电或不停电，或邻近及保留带电设备名称）：</div>

        <div class="static-val">不停电</div>
      </div>

      <!-- 7、安全措施 -->
      <div class="form-noline section-brk">7、注意事项（安全措施）</div>
      <div class="form-line">
        <span class="inline-input full">
          <el-select v-model="formData.safetyMeasures" multiple placeholder="" size="default">
            <el-option label="A、防止电压回路短路或接地" value="A" />
            <el-option label="B、严禁电流互感器二次回路开路" value="B" />
            <el-option label="C、与带电设备保持足够安全距离" value="C" />
          </el-select>
        </span>
      </div>
      <div class="form-line">
        <span class="line-label">工作票签发人签名：</span>
        <span class="static-val">李强</span>
      </div>
      <div class="form-line">
        <span class="line-label">签发日期：</span>
        <span class="static-val">{{ signDate }}</span>
      </div>

      <!-- 第8点 -->
      <div class="form-line">
        <span class="line-label"> 8、补充安全措施（工作许可人填写）</span>
      </div>
      <div class="static-val">无</div>

      <!-- 第9点 -->
      <div class="form-noline section-brk">9、确认本工作票1-8项</div>

      <div class="form-line">
        <span class="line-label">许可工作时间：</span>
        <span v-if="finalize" class="static-val">{{ endTimeText }}</span>
        <span v-else class="blank" />
      </div>

      <div class="form-line">
        <span class="line-label">工作负责人签名：</span>
        <span v-if="finalize" class="static-val">{{ finalizeLeaderSign }}</span>
        <span v-else class="blank" />
      </div>

      <div class="form-line">
        <span class="line-label">工作许可人签名：</span>
        <span v-if="finalize" class="static-val">{{ finalizePermitterSign }}</span>
        <span v-else class="blank" />
      </div>

      <!-- 10、危险点分析（表格样式，还原工作票：单元格内保留防范措施选择） -->
      <div class="form-noline section-brk">10、本工作危险点分析及防范措施（工作负责人填写）</div>
      <table class="task-table danger-table">
        <thead>
          <tr>
            <th style="width: 8%">序号</th>
            <th style="width: 18%">防范类型</th>
            <th style="width: 18%">危险点</th>
            <th>预防控制措施</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td class="center" rowspan="2">1</td>
            <td class="center" rowspan="2">人身触电<br />与伤害</td>
            <td class="center">走错工作间隔</td>
            <td>
              <span class="inline-input full">
                <el-select v-model="formData.dangerInterval" multiple placeholder="" size="default">
                  <el-option label="A、负责人对班成员进行安全教育" value="A" />
                  <el-option label="B、检查是否悬挂标示牌" value="B" />
                  <el-option label="C、核对工作任务单与现场信息一致" value="C" />
                  <el-option label="D、检查接入电源电线有无破损" value="D" />
                </el-select>
              </span>
            </td>
          </tr>
          <tr>
            <td class="center">短路或接地</td>
            <td>
              <span class="inline-input full">
                <el-select
                  v-model="formData.dangerShortCircuit"
                  multiple
                  placeholder=""
                  size="default"
                >
                  <el-option label="A、检查接入电源电线有无破损" value="A" />
                  <el-option label="B、使用合格工器具，螺丝刀除刀口外应绝缘" value="B" />
                  <el-option label="C、防止操作时相间或相对地短路" value="C" />
                  <el-option label="D、移动电源盘应带漏电保护器" value="D" />
                </el-select>
              </span>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 第11点 -->
      <div class="form-noline section-brk">11、确认工作负责人布置的任务和本施工项目安全措施</div>

      <div class="form-line">
        <span class="line-label">工作班人员签名</span>
        <span v-if="finalize" class="static-val">{{ finalizeMembersSign }}</span>
        <span v-else class="blank wide" />
      </div>

      <!-- 第12点 -->
      <div class="form-noline section-brk">12、工作票延期</div>

      <div class="form-line">
        <span class="line-label">有效期延长到</span>
        <span v-if="finalize" class="static-val">无</span>
        <span v-else class="blank date" />
      </div>

      <div class="form-line">
        <span class="line-label">工作负责人签名</span>
        <span v-if="finalize" class="static-val">{{ finalizeLeaderSign }}</span>
        <span v-else class="blank" />
      </div>

      <div class="form-line">
        <span class="line-label">工作许可人签名：</span>
        <span v-if="finalize" class="static-val">{{ finalizePermitterSign }}</span>
        <span v-else class="blank" />
      </div>

      <!-- 第13点 -->
      <div class="form-noline section-brk">13、工作票终结</div>

      <div class="form-line">
        <span>
          <template v-if="finalize">
            全部工作于{{ endTimeText }}结束，工作人员已全部撤离，材料工具已清理完毕。
          </template>
          <template v-else>
            全部工作于<span class="blank date" />结束，工作人员已全部撤离，材料工具已清理完毕。
          </template>
        </span>
      </div>

      <div class="form-line">
        <span class="line-label">工作负责人签名：</span>
        <span>
          <template v-if="finalize">{{ formData.leader || '张亮' }}　　{{ endTimeText }}</template>
          <template v-else><span class="blank" /></template>
        </span>
      </div>

      <div class="form-line">
        <span class="line-label">工作许可人签名：</span>
        <span>
          <template v-if="finalize">王伟、王五　　{{ endTimeText }}</template>
          <template v-else><span class="blank" /></template>
        </span>
      </div>

      <div class="form-line">
        <span class="line-label">备注：</span>
        <span>无</span>
      </div>

      <div class="submit-zone">
        <el-button type="warning" size="large" @click="validateAndSubmit">提交</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  /** 步骤24 办理工作终结模式：第13点工作票终结显示补全内容 */
  finalize: { type: Boolean, default: false }
})
const emit = defineEmits(['submit-ticket'])

const formData = reactive({
  company: '',
  ticketNo: '',
  team: '',
  leader: '',
  member1: '',
  member2: '',
  safetyMeasures: [],
  dangerInterval: [],
  dangerShortCircuit: []
})

// ─── 默认演示值（新增工作票/终结工作票共用） ───
const FINAL_DEFAULTS = {
  company: '福州',
  ticketNo: '1001',
  team: '装表班',
  leader: '张亮',
  member1: '李四',
  member2: '张三',
  safetyMeasures: ['A', 'B', 'C'],
  dangerInterval: ['A', 'B', 'C'],
  dangerShortCircuit: ['A', 'B', 'C', 'D']
}

// ─── 第7点 签发日期（当前日期 12时0分，贴近工作票样式） ───
const signDate = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}年${String(d.getMonth() + 1).padStart(2, '0')}月${String(d.getDate()).padStart(2, '0')}日 12时0分`
})

// ─── 步骤24 终结工作票补全（时间用当前真实时间） ───
const endTimeText = computed(() => {
  if (!props.finalize) return ''
  const d = new Date()
  return `${d.getFullYear()}年${String(d.getMonth() + 1).padStart(2, '0')}月${String(d.getDate()).padStart(2, '0')}日${String(d.getHours()).padStart(2, '0')}时${String(d.getMinutes()).padStart(2, '0')}分`
})
// 终结补全各签名显示值（参考工作票）
const finalizeLeaderSign = computed(() => formData.leader || FINAL_DEFAULTS.leader)
const finalizePermitterSign = '王伟、王五'
const finalizeMembersSign = '李四、张三'

// ─── 第5点 计划工作时间：当前时间 至 当前+3小时 ───
function fmtDT(d) {
  return `${d.getFullYear()}年${String(d.getMonth() + 1).padStart(2, '0')}月${String(d.getDate()).padStart(2, '0')}日 ${String(d.getHours()).padStart(2, '0')}时${String(d.getMinutes()).padStart(2, '0')}分`
}
const planStart = computed(() => fmtDT(new Date()))
const planEnd = computed(() => {
  const d = new Date()
  d.setHours(d.getHours() + 3)
  return fmtDT(d)
})

const stats = reactive({
  duration_seconds: 0,
  operation_count: 0,
  error_count: 0
})

let timer = null
onMounted(() => {
  timer = setInterval(() => {
    stats.duration_seconds++
  }, 1000)
  // 步骤24 终结工作票：进入即把表单填为完整状态（下拉/输入预选，避免红色下划线空白）
  // 编号/第一个姓名与首次填写一致（从存档恢复，若非空则不覆盖）
  if (props.finalize) {
    Object.keys(FINAL_DEFAULTS).forEach(k => {
      const v = FINAL_DEFAULTS[k]
      if (Array.isArray(v)) {
        if (!formData[k] || !formData[k].length) formData[k] = [...v]
      } else if (!formData[k]) {
        formData[k] = v
      }
    })
  }
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// 监听所有表单字段变更，自动统计操作次数
watch(
  formData,
  () => {
    stats.operation_count++
  },
  { deep: true }
)

/** 手动校验 — 只在提交时调用，不在填表过程中显示任何错误 */
const manualValidate = () => {
  const errors = {}
  if (!formData.company) errors.company = '请输入单位'
  else if (formData.company !== '福州') errors.company = '单位错误'
  if (!formData.ticketNo) errors.ticketNo = '请输入编号'
  if (!formData.team) errors.team = '请选择班组'
  else if (formData.team !== '装表班') errors.team = '班组选择错误'
  if (!formData.leader) errors.leader = '请选择工作负责人'
  else if (formData.leader !== '张亮') errors.leader = '负责人选择错误'
  if (!formData.member2) errors.member2 = '请选择工作班人员2'
  else if (formData.member2 !== '张三') errors.member2 = '人员选择错误'
  if (!formData.safetyMeasures || !formData.safetyMeasures.length)
    errors.safetyMeasures = '请选择注意事项'
  else if (formData.safetyMeasures.length !== 3) errors.safetyMeasures = '需全选三项'
  if (!formData.dangerInterval || !formData.dangerInterval.length)
    errors.dangerInterval = '请选择防范措施'
  else if (formData.dangerInterval.length !== 3 || formData.dangerInterval.includes('4'))
    errors.dangerInterval = '选择错误，请排除无关项'
  if (!formData.dangerShortCircuit || !formData.dangerShortCircuit.length)
    errors.dangerShortCircuit = '请选择防范措施'
  else if (formData.dangerShortCircuit.length !== 4) errors.dangerShortCircuit = '需全选四项'
  return errors
}

const formRef = ref(null)

const validateAndSubmit = async () => {
  const errors = manualValidate()
  if (Object.keys(errors).length > 0) {
    stats.error_count++
    emit('submit-ticket', { success: false, errors, errorCount: stats.error_count })
    return
  }
  emit('submit-ticket', {
    success: true,
    data: { ticketNo: formData.ticketNo, member1: formData.member1 },
    stats: { ...stats }
  })
}

defineExpose({ formData, stats })
</script>

<style scoped>
.ticket-paper {
  width: 850px;
  background-color: #fffef8;
  border: 2px solid #333;
  border-radius: 4px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  padding: 30px 36px;
  box-sizing: border-box;
  font-family: 'SimSun', '宋体', serif;
  color: #222;
}

.paper-header {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.paper-subheader {
  margin-bottom: 18px;
  padding-bottom: 8px;
  border-bottom: 1px solid #333;
}

.paper-right {
  margin-left: auto;
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 2px;
}

.paper-label,
.line-label {
  font-size: 14px;
  white-space: nowrap;
}

/* 静态填充值（部门/人数等无需输入的文字，与页面宋体统一） */
.static-val {
  border-bottom: 1px solid #333;
  font-family: 'SimSun', '宋体', serif;
  font-size: 14px;
  color: #222;
}

/* 填空下划线：span + 黑色细下划线（替代字符下划线 ____） */
.blank {
  display: inline-block;
  border-bottom: 1px solid #333;
  min-width: 130px;
  height: 1.4em;
}

.blank.wide {
  min-width: 260px;
  /* 工作班人员签名等长填空 */
}

.blank.date {
  min-width: 560px;
  /* 日期长串 */
}

.paper-title {
  font-size: 22px;
  font-weight: bold;
  letter-spacing: 2px;
}

/* 国网福建中间输入框放大 */
.paper-header .inline-input {
  font-size: 22px;
  min-width: 140px;
  border-bottom: 1.5px solid #ff0000;
}

.paper-header .inline-input :deep(.el-input__inner) {
  font-size: 22px !important;
  font-weight: bold;
  text-align: center;
}

/* 横线行 — 模拟纸上填空 */
.form-line {
  display: flex;
  align-items: center;
  padding: 6px 0;

  margin-bottom: 2px;
}

.form-noline {
  align-items: center;
  padding: 6px 0;
  margin-bottom: 2px;
}

/* 分节标题行：普通宋体，仅上方留白区分各节（无下划线、不加粗） */
.section-brk {
  margin-top: 14px;
  font-family: 'SimSun', '宋体', serif;
}

.form-line-row {
  display: flex;
  gap: 12px;
}

.form-line-row.triple .form-line {
  flex: 1;
}

.inline-input {
  display: inline-flex;
  min-width: 120px;
  margin: 0 4px;
  border-bottom: 1.5px solid #ff0000;
}

.inline-input.short {
  width: 100px;
}

.inline-input.full {
  flex: 1;
  min-width: 200px;
}

/* ===== 输入框：只保留红色下划线 ===== */
:deep(.inline-input .el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  border-radius: 0 !important;
  padding: 0 4px !important;
}

:deep(.inline-input .el-input__inner) {
  font-size: 14px;
  color: #222;
  font-family: 'SimSun', '宋体', serif;
}

:deep(.inline-input .el-input__inner::placeholder) {
  color: #ccc;
  font-style: italic;
}

/* ===== 下拉选择框：隐藏外框，只保留红色下划线（与输入框风格统一）===== */
:deep(.inline-input .el-select__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  border-radius: 0 !important;
  padding: 0 4px !important;
}

/* 隐藏下拉箭头图标 */
:deep(.inline-input .el-select__suffix) {
  display: none !important;
}

/* 多选标签背景与纸张同色 */
:deep(.inline-input .el-select__tags) {
  background: transparent !important;
}

:deep(.inline-input .el-select .el-tag) {
  background: #fffef8 !important;
  border: none !important;
  border-radius: 0 !important;
  font-family: 'SimSun', '宋体', serif;
  font-size: 14px;
  color: #222;
  margin: 1px 2px;
  padding: 0 2px !important;
}

/* 选中后的显示文字与正文统一 */
:deep(.inline-input .el-select__selected-item) {
  font-family: 'SimSun', '宋体', serif;
  font-size: 14px;
  color: #222;
}

:deep(.inline-input .el-select__input) {
  font-family: 'SimSun', '宋体', serif;
  font-size: 14px;
  color: #222;
}

.submit-zone {
  margin-top: 24px;
  text-align: right;
}

/* 内联 form-item — 保持纸张横线布局，校验时显示错误 */
:deep(.paper-form) {
  width: 100%;
}

:deep(.inline-item) {
  display: inline-flex !important;
  margin-bottom: 0 !important;
  position: relative;
}

:deep(.inline-item .el-form-item__content) {
  margin-left: 0 !important;
}

:deep(.inline-item .el-form-item__error) {
  position: absolute;
  top: calc(100% + 2px);
  left: 0;
  font-size: 11px;
  line-height: 1.3;
  color: #f56c6c;
  white-space: nowrap;
  background: rgba(255, 255, 255, 0.95);
  padding: 1px 4px;
  border-radius: 2px;
  z-index: 10;
}

.task-table {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0 14px 0;
  font-size: 14px;
  font-family: 'SimSun', '宋体', serif;
}

.task-table th,
.task-table td {
  border: 1px solid #333;
  padding: 8px 10px;
  text-align: left;
  vertical-align: middle;
}

.task-table th {
  background: #f5f5f5;
  font-weight: bold;
}

/* 危险点分析表格：居中列、单元格内下拉撑满 */
.danger-table td.center {
  text-align: center;
}

.danger-table .inline-input {
  display: flex;
  width: 100%;
  min-width: 0;
}

.danger-table .inline-input :deep(.el-select) {
  width: 100%;
}
</style>
