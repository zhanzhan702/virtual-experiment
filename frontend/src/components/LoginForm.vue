//登录表单组件，右侧表单区域，支持身份选择、验证码、登录按钮和注册跳转。

<template>
  <div class="login-card">
    <select v-model="role" class="input">
      <option value="user">用户</option>
      <option value="admin">管理员</option>
    </select>

    <input v-model="username" class="input" placeholder="用户名" />
    <input type="password" v-model="password" class="input" placeholder="密码" />

    <VerifyCode @updateCode="getCode" />

    <button class="login-btn">登录</button>

    <div class="register" @click="goRegister">
      还没有账号？点击注册
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import VerifyCode from './VerifyCode.vue'

const router = useRouter()
const role = ref('user')
const username = ref('')
const password = ref('')

const getCode = (code) => { console.log('验证码:', code) }

function goRegister() { router.push('/register') }
</script>

<style scoped>
.login-card{
  width:420px;
  padding:40px;
  background:#4c4d50;
  border-radius:5px;
}
.input{
  width:100%;
  height:50px;
  margin-bottom:20px;
  padding-left:15px;
  font-size:18px;
}
.login-btn{
  width:100%;
  height:50px;
  background:#2196f3;
  color:white;
  border:none;
  cursor:pointer;
  font-size:18px;
}
.register{
  text-align:center;
  color:#4da6ff;
  margin-top:40px;
  cursor:pointer;
}
</style>