<template>
  <div class="login-card">
    <div class="brand">
      <img :src="Images.mjuLogo" class="brand-logo" alt="校徽" />
      <h2 class="brand-title">用电信息采集实训互动系统</h2>
      <p class="brand-sub">欢迎登录</p>
    </div>

    <el-form
      :model="form"
      :rules="rules"
      ref="formRef"
      label-position="top"
      @submit.prevent="handleLogin"
    >
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名 / 学号" size="large" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="密码"
          size="large"
        />
      </el-form-item>

      <div class="code-item">
        <VerifyCode ref="verifyCodeRef" v-model="form.code" @updateCode="saveGeneratedCode" />
      </div>

      <el-form-item>
        <el-button
          type="primary"
          size="large"
          class="login-btn"
          @click="handleLogin"
          :loading="loading"
        >
          登 录
        </el-button>
      </el-form-item>
    </el-form>

    <div class="register" @click="goRegister">还没有账号？点击注册</div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import Images from '@/constants/images'
import VerifyCode from './VerifyCode.vue'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const verifyCodeRef = ref(null)
const loading = ref(false)
let generatedCode = ''

const form = reactive({
  username: '',
  password: '',
  code: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名或学号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function saveGeneratedCode(code) {
  generatedCode = code
}

async function handleLogin() {
  // 校验必填项
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 手动校验验证码（独立于 el-form-item，因为 VerifyCode 非 ElInput）
  const code = (form.code || '').trim()
  if (!code) {
    ElMessage.warning('请输入验证码')
    verifyCodeRef.value?.createCode()
    form.code = ''
    return
  }
  if (code.toUpperCase() !== generatedCode) {
    ElMessage.error('验证码错误，请重新输入')
    verifyCodeRef.value?.createCode()
    form.code = ''
    return
  }

  loading.value = true
  try {
    await authStore.login({
      username: form.username,
      password: form.password
    })
    if (authStore.isStudent) {
      router.push('/experiment')
    } else {
      router.push('/admin')
    }
  } catch (err) {
    ElMessage.error('登录失败：' + (err.response?.data?.message || err.message))
  } finally {
    loading.value = false
  }
}

function goRegister() {
  router.push('/register')
}
</script>

<style scoped>
/* 配色取自管理员后台主题：青绿 #1ab394 + 浅色卡片 */
.login-card {
  width: 420px;
  padding: 40px 40px 30px;
  background: #fff;
  border: 1px solid var(--admin-border, #ebeef5);
  border-radius: 12px;
  box-shadow: 0 14px 40px rgba(26, 179, 148, 0.12);

  /* 管理员后台主色（青绿），只在登录卡片内生效，不影响学生深色实验页 */
  --el-color-primary: #1ab394;
  --el-color-primary-light-3: #4fc4ab;
  --el-color-primary-light-5: #8dd9cb;
  --el-color-primary-light-7: #bce9e0;
  --el-color-primary-light-8: #d3f1eb;
  --el-color-primary-light-9: #e9f8f5;
  --el-color-primary-dark-2: #159c81;
}

.brand {
  text-align: center;
  margin-bottom: 26px;
}

.brand-logo {
  width: 56px;
  height: 56px;
  object-fit: contain;
  margin-bottom: 8px;
}

.brand-title {
  margin: 0;
  font-size: 19px;
  line-height: 1.5;
  font-weight: 600;
  letter-spacing: 1px;
  color: var(--admin-text, #303133);
}

.brand-sub {
  margin: 6px 0 0;
  font-size: 13px;
  letter-spacing: 4px;
  color: var(--admin-text-muted, #909399);
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 6px;
  border-radius: 8px;
}

.register {
  text-align: center;
  font-size: 14px;
  color: var(--admin-primary, #1ab394);
  cursor: pointer;
  transition: color 0.2s;
}

.register:hover {
  color: var(--admin-primary-hover, #159c81);
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

.code-item :deep(.el-form-item__content) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

:deep(.el-form-item__error) {
  position: static;
  padding-top: 4px;
}
</style>
