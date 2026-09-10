<template>
  <el-dialog
    v-model="visible"
    title="修改密码"
    width="440px"
    :close-on-click-modal="false"
    @closed="resetForm"
  >
    <p v-if="targetUser" class="target-tip">
      目标用户：<strong>{{ targetUser.name || targetUser.username }}</strong>
    </p>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          show-password
          placeholder="6-20位，含字母和数字"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入新密码"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resetUserPassword } from '@/api/admin-user'
import { PASSWORD_PATTERN, PASSWORD_MESSAGE } from '@/constants/password-rule'

const visible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const targetUser = ref(null)

const form = reactive({
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (_rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: PASSWORD_PATTERN, message: PASSWORD_MESSAGE, trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

/** 打开弹窗；由父组件传入目标用户 */
function open(user) {
  targetUser.value = user
  visible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  form.newPassword = ''
  form.confirmPassword = ''
  targetUser.value = null
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const name = targetUser.value?.name || targetUser.value?.username

  // 对他人密码的改动不可逆，提交前二次确认（不显示明文密码）
  try {
    await ElMessageBox.confirm(`确定重置「${name}」的密码？此操作不可撤销。`, '请确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  submitting.value = true
  try {
    await resetUserPassword(targetUser.value.id, {
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword
    })
    ElMessage.success('密码修改成功')
    visible.value = false
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '修改失败')
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>

<style scoped>
.target-tip {
  margin: 0 0 16px;
  color: #606266;
}
</style>
