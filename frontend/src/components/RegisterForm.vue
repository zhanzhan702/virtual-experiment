<template>
  <div class="register-card">
    <h2 class="title">学生注册</h2>

    <input v-model="form.username" class="input" placeholder="用户名" />
    <input type="password" v-model="form.password" class="input" placeholder="密码" />
    <input v-model="form.name" class="input" placeholder="姓名" />
    <input v-model="form.phone" class="input" placeholder="手机号" />
    <input v-model="form.email" class="input" placeholder="邮箱" />
    <input v-model="form.studentNo" class="input" placeholder="学号" />

    <button class="register-btn" @click="handleRegister">注 册</button>

    <div class="login-link" @click="goLogin">
      已有账号？立即登录
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  name: '',
  phone: '',
  email: '',
  studentNo: '',
  orgId: ''
})

async function handleRegister() {
  if (!form.username || !form.password || !form.name) {
    alert('请填写必填项（用户名、密码、姓名）')
    return
  }
  try {
    await authStore.register(form)
    alert('注册成功，请登录')
    router.push('/')
  } catch (err) {
    alert('注册失败：' + (err.response?.data?.message || err.message))
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
.input {
  width: 100%;
  height: 50px;
  margin-bottom: 20px;
  padding-left: 15px;
  font-size: 18px;
  box-sizing: border-box;
}
.register-btn {
  width: 100%;
  height: 50px;
  background: #4caf50;
  color: white;
  border: none;
  cursor: pointer;
  font-size: 18px;
}
.login-link {
  text-align: center;
  color: #4da6ff;
  margin-top: 40px;
  cursor: pointer;
}
</style>
