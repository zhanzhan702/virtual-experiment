<template>
  <div class="ticket-paper">
    <h2 class="ticket-title">配电第二种工作票</h2>

    <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        @change="handleUserOperation"
    >
      <div class="ticket-grid-row">
        <el-form-item label="国网福建 (____) 供电公司" prop="company">
          <el-input v-model="formData.company" placeholder="请输入（键盘输入）" @input="handleUserOperation" />
        </el-form-item>
        <el-form-item label="工作班组" prop="team">
          <el-select v-model="formData.team" placeholder="请选择">
            <el-option label="修试班" value="修试班" />
            <el-option label="装表班" value="装表班" />
            <el-option label="运维班" value="运维班" />
          </el-select>
        </el-form-item>
      </div>

      <div class="ticket-grid-row triple">
        <el-form-item label="工作负责人" prop="leader">
          <el-select v-model="formData.leader" placeholder="请选择">
            <el-option label="张亮" value="张亮" />
            <el-option label="王五" value="王五" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作班人员 1" prop="member1">
          <el-input v-model="formData.member1" placeholder="请输入（键盘输入）" @input="handleUserOperation" />
        </el-form-item>
        <el-form-item label="工作班人员 2" prop="member2">
          <el-select v-model="formData.member2" placeholder="请选择">
            <el-option label="张三" value="张三" />
            <el-option label="赵六" value="赵六" />
          </el-select>
        </el-form-item>
      </div>

      <el-divider content-position="left">7、注意事项（安全措施）</el-divider>
      <el-form-item label="请选择安全措施：" prop="safetyMeasures">
        <el-checkbox-group v-model="formData.safetyMeasures">
          <el-checkbox label="A" value="A">A、防止电压回路短路或接地</el-checkbox>
          <el-checkbox label="B" value="B">B、严禁电流互感器二次回路开路</el-checkbox>
          <el-checkbox label="C" value="C">C、与带电设备保持足够安全距离</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-divider content-position="left">10、危险点分析及防范措施</el-divider>

      <el-form-item label="走错工作间隔的防范措施：" prop="dangerInterval">
        <el-checkbox-group v-model="formData.dangerInterval">
          <el-checkbox label="1" value="1">工作负责人对工作班成员应进行安全教育...</el-checkbox>
          <el-checkbox label="2" value="2">检查是否悬挂标示牌。</el-checkbox>
          <el-checkbox label="3" value="3">核对工作任务单与现场信息是否一致</el-checkbox>
          <el-checkbox label="4" value="4">检查接入电源的电线有无破损，接入电源应固定牢靠。</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <el-form-item label="短路或接地的防范措施：" prop="dangerShortCircuit">
        <el-checkbox-group v-model="formData.dangerShortCircuit">
          <el-checkbox label="A" value="A">A、检查接入电源的电线有无破损...</el-checkbox>
          <el-checkbox label="B" value="B">B、使用合格的工器具，螺丝刀除刀口以外...</el-checkbox>
          <el-checkbox label="C" value="C">C、防止操作时相间或相对地短路...</el-checkbox>
          <el-checkbox label="D" value="D">D、移动电源盘，应带漏电保护器。</el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <div class="submit-zone">
        <el-button type="warning" size="large" @click="validateAndSubmit">提交</el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'

const emit = defineEmits(['submit-ticket'])

// 表单响应式数据
const formData = reactive({
  company: '',
  team: '',
  leader: '',
  member1: '',
  member2: '',
  safetyMeasures: [],
  dangerInterval: [],
  dangerShortCircuit: []
})

// 统计埋点数据
const stats = reactive({
  duration_seconds: 0,
  operation_count: 0,
  error_count: 0
})

let timer = null

onMounted(() => {
  timer = setInterval(() => { stats.duration_seconds++ }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const handleUserOperation = () => { stats.operation_count++ }

// 严格的表单校验规则（完全匹配操作手册的标准答案）
const formRules = {
  company: [
    { required: true, message: '请输入单位', trigger: 'blur' },
    { validator: (rule, value, callback) => value === '福州' ? callback() : callback(new Error('单位错误')) }
  ],
  team: [
    { required: true, message: '请选择班组', trigger: 'change' },
    { validator: (rule, value, callback) => value === '装表班' ? callback() : callback(new Error('班组选择错误')) }
  ],
  leader: [
    { required: true, message: '请选择工作负责人', trigger: 'change' },
    { validator: (rule, value, callback) => value === '张亮' ? callback() : callback(new Error('负责人选择错误')) }
  ],
  member1: [
    { required: true, message: '请输入工作班人员1', trigger: 'blur' },
    { validator: (rule, value, callback) => value === '李四' ? callback() : callback(new Error('人员输入错误')) }
  ],
  member2: [
    { required: true, message: '请选择工作班人员2', trigger: 'change' },
    { validator: (rule, value, callback) => value === '张三' ? callback() : callback(new Error('人员选择错误')) }
  ],
  safetyMeasures: [
    { type: 'array', required: true, message: '请选择注意事项', trigger: 'change' },
    { validator: (rule, value, callback) => value.length === 3 ? callback() : callback(new Error('需全选三项')) }
  ],
  dangerInterval: [
    { type: 'array', required: true, message: '请选择防范措施', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        // 排除第4项（检查电源电线破损）
        if (value.length === 3 && !value.includes('4')) callback()
        else callback(new Error('选择错误，请排除无关项'))
      }
    }
  ],
  dangerShortCircuit: [
    { type: 'array', required: true, message: '请选择防范措施', trigger: 'change' },
    { validator: (rule, value, callback) => value.length === 4 ? callback() : callback(new Error('需全选四项')) }
  ]
}

const formRef = ref(null)

const validateAndSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    // 校验通过，向父组件抛出数据
    emit('submit-ticket', {
      success: true,
      data: JSON.parse(JSON.stringify(formData)),
      stats: { ...stats }
    })
  } catch (error) {
    // 校验失败，错误计数加一并通知父组件
    stats.error_count++
    emit('submit-ticket', { success: false, errorCount: stats.error_count })
  }
}
</script>

<style scoped>
.ticket-paper {
  width: 850px;
  background-color: #ffffff;
  border: 2px solid #b3c0d1;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  padding: 30px;
  box-sizing: border-box;
}

.ticket-title {
  text-align: center;
  color: #303133;
  margin-bottom: 25px;
  letter-spacing: 2px;
}

.ticket-grid-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.ticket-grid-row.triple {
  grid-template-columns: 1fr 1fr 1fr;
}

.submit-zone {
  margin-top: 30px;
  text-align: right;
}

/* 覆盖 Element Plus 默认样式，使其更贴近工控表单风格 */
:deep(.el-checkbox) {
  display: flex;
  margin-bottom: 10px;
  white-space: normal;
}
:deep(.el-checkbox__label) {
  line-height: 1.5;
}
</style>