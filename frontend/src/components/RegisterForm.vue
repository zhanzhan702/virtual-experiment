<template>
    <div class="register-card">
        <h2 class="title">学生注册</h2>

        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @submit.prevent="handleRegister">
            <el-form-item prop="username">
                <el-input v-model="form.username" placeholder="用户名" size="large" />
            </el-form-item>
            <el-form-item prop="password">
                <el-input v-model="form.password" type="password" show-password placeholder="密码" size="large" />
            </el-form-item>
            <el-form-item prop="confirmPassword">
                <el-input v-model="form.confirmPassword" type="password" show-password placeholder="确认密码"
                    size="large" />
            </el-form-item>
            <el-form-item prop="name">
                <el-input v-model="form.name" placeholder="姓名" size="large" />
            </el-form-item>
            <el-form-item prop="phone">
                <el-input v-model="form.phone" placeholder="手机号" size="large" />
            </el-form-item>
            <el-form-item prop="email">
                <el-input v-model="form.email" placeholder="邮箱" size="large" />
            </el-form-item>
            <el-form-item prop="studentNo">
                <el-input v-model="form.studentNo" placeholder="学号" size="large" />
            </el-form-item>
            <el-form-item prop="orgId">
                <el-input v-model="form.orgId" placeholder="所属班级 ID" size="large" />
            </el-form-item>
            <el-form-item>
                <el-button type="success" size="large" class="register-btn" @click="handleRegister" :loading="loading">
                    注 册
                </el-button>
            </el-form-item>
        </el-form>

        <div class="login-link" @click="goLogin">
            已有账号？立即登录
        </div>
    </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
    username: '',
    password: '',
    confirmPassword: '',
    name: '',
    phone: '',
    email: '',
    studentNo: '',
    orgId: ''
})

const validateConfirmPassword = (_rule, value, callback) => {
    if (!value) {
        callback(new Error('请再次输入密码'))
    } else if (value !== form.password) {
        callback(new Error('两次输入的密码不一致'))
    } else {
        callback()
    }
}

const rules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码至少6位', trigger: 'blur' }
    ],
    confirmPassword: [
        { validator: validateConfirmPassword, trigger: 'blur' }
    ],
    name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

async function handleRegister() {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
    loading.value = true
    try {
        await authStore.register(form)
        alert('注册成功，请登录')
        router.push('/')
    } catch (err) {
        alert('注册失败：' + (err.response?.data?.message || err.message))
    } finally {
        loading.value = false
    }
}

function goLogin() {
    router.push('/')
}
</script>

<style scoped>
.register-card {
    width: 420px;
    padding: 40px;
    background: #4c4d50;
    border-radius: 5px;
}

.title {
    text-align: center;
    color: #fff;
    margin-bottom: 24px;
    font-size: 22px;
}

.register-btn {
    width: 100%;
}

.login-link {
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
