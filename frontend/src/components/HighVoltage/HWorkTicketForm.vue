<template>
  <div class="ticket-paper">
    <el-form ref="formRef" :model="formData" @change="handleUserOperation" class="paper-form">
    <!-- 表头 -->
    <div class="paper-header">
      <span class="paper-title">国网福建</span>
        <span class="inline-input">
          <el-input v-model="formData.company" placeholder="______" size="default" @input="handleUserOperation" />
        </span>
      <span class="paper-title">供电公司</span>
      <span class="paper-right">配电第二种工作票</span>
    </div>
    <div class="paper-subheader">
      <span class="paper-label">编号：</span>
      <span class="inline-input short">
        <el-input v-model="formData.ticketNo" placeholder="1001" size="default" />
      </span>
    </div>

      <!-- 第一行：班组 -->
      <div class="form-line">
        <span class="line-label">1、工作班组：</span>
          <span class="inline-input">
            <el-select v-model="formData.team" placeholder="请选择" size="default">
              <el-option label="修试班" value="修试班" />
            <el-option label="装表班" value="装表班" />
            <el-option label="运维班" value="运维班" />
          </el-select>
            </span>
      </div>

      <!-- 第二行：负责人 + 班人员 -->
      <div class="form-line-row triple">
        <div class="form-line">
          <span class="line-label">2、工作负责人：</span>
            <span class="inline-input">
              <el-select v-model="formData.leader" placeholder="请选择" size="default">
                <el-option label="张亮" value="张亮" />
                <el-option label="王五" value="王五" />
              </el-select>
            </span>
        </div>
        <div class="form-line">
          <span class="line-label">工作班人员1：</span>
            <span class="inline-input">
              <el-input v-model="formData.member1" placeholder="________" size="default" @input="handleUserOperation" />
            </span>
        </div>
        <div class="form-line">
          <span class="line-label">工作班人员2：</span>
            <span class="inline-input">
              <el-select v-model="formData.member2" placeholder="请选择" size="default">
                <el-option label="张三" value="张三" />
                <el-option label="赵六" value="赵六" />
              </el-select>
            </span>
        </div>
      </div>

      <!-- 第3点 -->
  <div class="form-line">
    <span class="line-label">
      3、工作的变配电站名称及设备双重名称：
    </span>
    <span>
      福州市台江区鳌峰路雪花水泥厂配电室计量柜
    </span>
  </div>

  <!-- 第4点 -->
<div class="section-title">4、工作任务</div>

<table class="task-table">
  <thead>
    <tr>
      <th style="width: 70%;">工作地点或地段</th>
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
<div class="form-line">
  <span class="line-label">5、计划工作时间：</span>
  <span>
    {{ new Date().getFullYear() }}年
    {{ String(new Date().getMonth() + 1).padStart(2,'0') }}月
    {{ String(new Date().getDate()).padStart(2,'0') }}日
    {{ String(new Date().getHours()).padStart(2,'0') }}时
    {{ String(new Date().getMinutes()).padStart(2,'0') }}分
    至__________________
  </span>
</div>

<!-- 第6点 -->
<div class="form-line">
  <span class="line-label">
    6、工作条件（停电或不停电，或邻近及保留带电设备名称）：
  </span>
  <span>不停电</span>
</div>


      <!-- 7、安全措施 -->
      <div class="section-title">7、注意事项（安全措施）</div>
      <div class="form-line">
        <span class="line-label">请选择：</span>
          <span class="inline-input full">
            <el-select v-model="formData.safetyMeasures" multiple placeholder="请选择安全措施" size="default">
              <el-option label="A、防止电压回路短路或接地" value="A" />
              <el-option label="B、严禁电流互感器二次回路开路" value="B" />
              <el-option label="C、与带电设备保持足够安全距离" value="C" />
            </el-select>
          </span>
      </div>
      <div class="form-line">
      <span class="line-label">工作票签发人签名：</span>
      <span>李强</span>
      </div>

      <!-- 第8点 -->
      <div class="form-line">
      <span class="line-label">
          8、补充安全措施（工作许可人填写）：
      </span>
      <span>无</span>
      </div>

      <!-- 第9点 -->
<div class="section-title">9、确认本工作票1-8项</div>

<div class="form-line">
  <span class="line-label">许可工作时间：</span>
  <span>__________________</span>
</div>

<div class="form-line">
  <span class="line-label">工作负责人签名：</span>
  <span>__________________</span>
</div>

<div class="form-line">
  <span class="line-label">工作许可人签名：</span>
  <span>__________________</span>
</div>



      <!-- 10、危险点分析 -->
      <div class="section-title">10、危险点分析及防范措施</div>

      <div class="form-line">
        <span class="line-label">走错工作间隔：</span>
          <span class="inline-input full">
            <el-select v-model="formData.dangerInterval" multiple placeholder="请选择（排除无关项）" size="default">
              <el-option label="1、负责人对班成员进行安全教育" value="1" />
              <el-option label="2、检查是否悬挂标示牌" value="2" />
              <el-option label="3、核对工作任务单与现场信息一致" value="3" />
              <el-option label="4、检查接入电源电线有无破损" value="4" />
            </el-select>
          </span>
      </div>

      <div class="form-line">
        <span class="line-label">短路或接地：</span>
          <span class="inline-input full">
            <el-select v-model="formData.dangerShortCircuit" multiple placeholder="请选择（全选）" size="default">
              <el-option label="A、检查接入电源电线有无破损" value="A" />
              <el-option label="B、使用合格工器具，螺丝刀除刀口外应绝缘" value="B" />
              <el-option label="C、防止操作时相间或相对地短路" value="C" />
              <el-option label="D、移动电源盘应带漏电保护器" value="D" />
            </el-select>
          </span>
      </div>


      <!-- 第11点 -->
<div class="section-title">
  11、确认工作负责人布置的任务和本施工项目安全措施
</div>

<div class="form-line">
  <span class="line-label">工作班人员签名：</span>
  <span>________________________________________________</span>
</div>

<!-- 第12点 -->
<div class="section-title">12、工作票延期</div>

<div class="form-line">
  <span class="line-label">有效期延长到：</span>
  <span>______年______月______日______时______分</span>
</div>

<div class="form-line">
  <span class="line-label">工作负责人签名：</span>
  <span>__________________</span>
</div>

<div class="form-line">
  <span class="line-label">工作许可人签名：</span>
  <span>__________________</span>
</div>

<!-- 第13点 -->
<div class="section-title">13、工作票终结</div>

<div class="form-line">
  <span>
    全部工作于______年______月______日______时______分结束，
    工作人员已全部撤离，材料工具已清理完毕。
  </span>
</div>

<div class="form-line">
  <span class="line-label">工作负责人签名：</span>
  <span>__________________</span>
</div>

<div class="form-line">
  <span class="line-label">工作许可人签名：</span>
  <span>__________________</span>
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
import { ref, reactive, onMounted, onUnmounted } from 'vue'

const emit = defineEmits(['submit-ticket'])

const formData = reactive({
  company: '',
  ticketNo: '1001',
  team: '',
  leader: '',
  member1: '',
  member2: '',
  safetyMeasures: [],
  dangerInterval: [],
  dangerShortCircuit: []
})

const stats = reactive({
  duration_seconds: 0,
  operation_count: 0,
  error_count: 0
})

let timer = null
onMounted(() => { timer = setInterval(() => { stats.duration_seconds++ }, 1000) })
onUnmounted(() => { if (timer) clearInterval(timer) })

const handleUserOperation = () => { stats.operation_count++ }

/** 手动校验 — 只在提交时调用，不在填表过程中显示任何错误 */
const manualValidate = () => {
  const errors = {}
  if (!formData.company) errors.company = '请输入单位'
  else if (formData.company !== '福州') errors.company = '单位错误'
  if (!formData.team) errors.team = '请选择班组'
  else if (formData.team !== '装表班') errors.team = '班组选择错误'
  if (!formData.leader) errors.leader = '请选择工作负责人'
  else if (formData.leader !== '张亮') errors.leader = '负责人选择错误'
  if (!formData.member1) errors.member1 = '请输入工作班人员1'
  else if (formData.member1 !== '李四') errors.member1 = '人员输入错误'
  if (!formData.member2) errors.member2 = '请选择工作班人员2'
  else if (formData.member2 !== '张三') errors.member2 = '人员选择错误'
  if (!formData.safetyMeasures || !formData.safetyMeasures.length) errors.safetyMeasures = '请选择注意事项'
  else if (formData.safetyMeasures.length !== 3) errors.safetyMeasures = '需全选三项'
  if (!formData.dangerInterval || !formData.dangerInterval.length) errors.dangerInterval = '请选择防范措施'
  else if (formData.dangerInterval.length !== 3 || formData.dangerInterval.includes('4')) errors.dangerInterval = '选择错误，请排除无关项'
  if (!formData.dangerShortCircuit || !formData.dangerShortCircuit.length) errors.dangerShortCircuit = '请选择防范措施'
  else if (formData.dangerShortCircuit.length !== 4) errors.dangerShortCircuit = '需全选四项'
  return errors
}

// 以下 formRef 和 formRules 仅保留结构兼容
const formRef = ref(null)
const formRules = {}

const validateAndSubmit = async () => {
  const errors = manualValidate()
  if (Object.keys(errors).length > 0) {
    stats.error_count++
    emit('submit-ticket', { success: false, errors, errorCount: stats.error_count })
    return
  }
  emit('submit-ticket', { success: true, data: JSON.parse(JSON.stringify(formData)), stats: { ...stats } })
}
</script>

<style scoped>
.ticket-paper {
  width: 850px;
  background-color: #fffef8;
  border: 2px solid #333;
  border-radius: 4px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
  padding: 30px 36px;
  box-sizing: border-box;
  font-family: "SimSun", "宋体", serif;
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
.paper-label, .line-label {
  font-size: 14px;
  white-space: nowrap;
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
}

.paper-header .inline-input :deep(.el-input__inner) {
  font-size: 22px !important;
  font-weight: bold;
  text-align: center;
}

/* 横线行 — 模拟纸上填空 */
.form-line {
  display: flex;
  align-items: baseline;
  padding: 6px 0;
  border-bottom: 1px dashed #ccc;
  margin-bottom: 2px;
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
}
.inline-input.short {
  width: 100px;
}
.inline-input.full {
  flex: 1;
  min-width: 200px;
}

.section-title {
  font-size: 15px;
  font-weight: bold;
  margin: 16px 0 8px;
  padding-bottom: 4px;
  border-bottom: 1px solid #333;
}

/* 下划线风格 — 去除 Element 输入框边框 */
:deep(.inline-input .el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  border-bottom: 1px solid #000 !important;
  border-radius: 0 !important;
  padding: 0 4px !important;
}
:deep(.inline-input .el-input__inner) {
  font-size: 14px;
  color: #00f;
  font-family: "SimSun", "宋体", serif;
}
:deep(.inline-input .el-input__inner::placeholder) {
  color: #ccc;
  font-style: italic;
}
:deep(.inline-input .el-select .el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  border-bottom: 1px solid #000 !important;
  border-radius: 0 !important;
  padding: 0 4px !important;
}
:deep(.inline-input .el-select .el-select__tags) {
  background: transparent;
}
:deep(.inline-input .el-tag) {
  background: #e8f0fe;
  border-radius: 2px;
  font-size: 12px;
  margin: 1px 2px;
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
  background: rgba(255,255,255,0.95);
  padding: 1px 4px;
  border-radius: 2px;
  z-index: 10;
}

.task-table {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0 14px 0;
  font-size: 14px;
  font-family: "SimSun", "宋体", serif;
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
</style>