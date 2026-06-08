<template>
  <div class="ticket-paper">
    <!-- 表头 -->
    <div class="paper-header">
      <span class="paper-label">国网福建</span>
      <span class="inline-input">
        <el-input v-model="formData.company" placeholder="______" size="default" @input="handleUserOperation" />
      </span>
      <span class="paper-label">供电公司</span>
      <span class="paper-right">配电第二种工作票</span>
    </div>
    <div class="paper-subheader">
      <span class="paper-label">编号：</span>
      <span class="inline-input short">
        <el-input v-model="formData.ticketNo" placeholder="1001" size="default" />
      </span>
    </div>

    <el-form ref="formRef" :model="formData" :rules="formRules" @change="handleUserOperation">
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

const formRules = {
  company: [
    { required: true, message: '请输入单位', trigger: 'blur' },
    { validator: (_r, v, cb) => v === '福州' ? cb() : cb(new Error('单位错误')) }
  ],
  team: [
    { required: true, message: '请选择班组', trigger: 'change' },
    { validator: (_r, v, cb) => v === '装表班' ? cb() : cb(new Error('班组选择错误')) }
  ],
  leader: [
    { required: true, message: '请选择工作负责人', trigger: 'change' },
    { validator: (_r, v, cb) => v === '张亮' ? cb() : cb(new Error('负责人选择错误')) }
  ],
  member1: [
    { required: true, message: '请输入工作班人员1', trigger: 'blur' },
    { validator: (_r, v, cb) => v === '李四' ? cb() : cb(new Error('人员输入错误')) }
  ],
  member2: [
    { required: true, message: '请选择工作班人员2', trigger: 'change' },
    { validator: (_r, v, cb) => v === '张三' ? cb() : cb(new Error('人员选择错误')) }
  ],
  safetyMeasures: [
    { type: 'array', required: true, message: '请选择注意事项', trigger: 'change' },
    { validator: (_r, v, cb) => v.length === 3 ? cb() : cb(new Error('需全选三项')) }
  ],
  dangerInterval: [
    { type: 'array', required: true, message: '请选择防范措施', trigger: 'change' },
    { validator: (_r, v, cb) => v.length === 3 && !v.includes('4') ? cb() : cb(new Error('选择错误，请排除无关项')) }
  ],
  dangerShortCircuit: [
    { type: 'array', required: true, message: '请选择防范措施', trigger: 'change' },
    { validator: (_r, v, cb) => v.length === 4 ? cb() : cb(new Error('需全选四项')) }
  ]
}

const formRef = ref(null)

const validateAndSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    emit('submit-ticket', { success: true, data: JSON.parse(JSON.stringify(formData)), stats: { ...stats } })
  } catch (error) {
    stats.error_count++
    emit('submit-ticket', { success: false, errorCount: stats.error_count })
  }
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
</style>