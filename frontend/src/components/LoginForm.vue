登录表单组件，右侧表单区域，支持验证码、登录按钮和注册跳转。

<template>
    <div class="login-card">
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent="handleLogin">
            <el-form-item prop="username">
                <el-input v-model="form.username" placeholder="用户名" size="large" />
            </el-form-item>
            <el-form-item prop="password">
                <el-input v-model="form.password" type="password" show-password placeholder="密码" size="large" />
            </el-form-item>

            <VerifyCode @updateCode="getCode" />

            <el-form-item>
                <el-button type="primary" size="large" class="login-btn" @click="handleLogin" :loading="loading">
                    登 录
                </el-button>
            </el-form-item>
        </el-form>

        <div class="register" @click="goRegister">
            还没有账号？点击注册
        </div>
    </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import VerifyCode from './VerifyCode.vue'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
    username: '',
    password: ''
})

const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const getCode = (code) => { console.log('验证码:', code) }

async function handleLogin() {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
    loading.value = true
    try {
        const res = await authStore.login({
            username: form.username,
            password: form.password
        })
        console.log('登录成功', res)
        if (authStore.isStudent) {
            router.push('/experiment')
        } else {
            router.push('/admin')
        }
    } catch (err) {
        alert('登录失败：' + (err.response?.data?.message || err.message))
    } finally {
        loading.value = false
    }
}

function goRegister() {
    router.push('/register')
}
</script>

<style scoped>
.login-card {
    width: 420px;
    padding: 40px;
    background: #4c4d50;
    border-radius: 5px;
}

.login-btn {
    width: 100%;
}

.register {
    text-align: center;
    color: #4da6ff;
    cursor: pointer;
}

/* 覆盖 Element Plus 深色主题样式 */
:deep(.el-input__wrapper) {
    background: #3a3b3e !important;
    box-shadow: none !important;
    border: 1px solid #555 !important;
    border-radius: 4px !important;
}

:deep(.el-input__wrapper:hover) {
    border-color: #2196f3 !important;
}

:deep(.el-input__wrapper.is-focus) {
    border-color: #2196f3 !important;
    box-shadow: 0 0 0 1px #2196f3 inset !important;
}

:deep(.el-input__inner) {
    color: #fff !important;
}

:deep(.el-input__inner::placeholder) {
    color: #999 !important;
}

:deep(.el-form-item) {
    margin-bottom: 18px;
}

:deep(.el-form-item__error) {
    position: static;
    padding-top: 4px;
}
</style>