<template>
  <div class="profile-view">
    <el-card shadow="never" class="profile-card">
      <template #header>
        <div class="card-header">
          <span class="title">修改密码</span>
        </div>
      </template>

      <p class="hint">修改成功后需要重新登录。新密码需 6-20 位，且同时包含字母和数字。</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>

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
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            确认修改
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changePassword } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { PASSWORD_PATTERN, PASSWORD_MESSAGE } from '@/constants/password-rule'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  oldPassword: '',
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

/** 前两项是格式校验，服务端是否接受原密码只有提交后才知道 */
const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: PASSWORD_PATTERN, message: PASSWORD_MESSAGE, trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await ElMessageBox.confirm('修改成功后需要重新登录，确定继续？', '请确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户取消
  }

  submitting.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword
    })

    ElMessage.success('密码已修改，请用新密码登录')
    // 强制重新登录：既符合常规预期，也让用户立即验证新密码可用
    authStore.clearAuth()
    router.push('/')
  } catch (err) {
    // 原密码错误、新旧相同等都由服务端判定，直接展示后端文案
    ElMessage.error(err.response?.data?.message || '修改失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.profile-view {
  display: flex;
  justify-content: center;
}

.profile-card {
  width: 520px;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius);
}

.card-header .title {
  font-size: 15px;
  font-weight: 600;
  color: var(--admin-text);
}

.hint {
  margin: 0 0 20px;
  color: var(--admin-text-muted);
  font-size: 13px;
  line-height: 1.6;
}
</style>
