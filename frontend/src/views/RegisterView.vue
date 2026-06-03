/* 注册页面视图，显示注册表单。 */

<template>
  <div class="register-page">
    <RegisterForm />     
  </div>
</template>

<script setup>
import RegisterForm from '@/components/RegisterForm.vue'       //<!-- 注册表单组件 -->
</script>

<style scoped>
.register-page{
  display:flex;
  justify-content:center;
  align-items:center;
  min-height:100vh;
  background:#f5f5f5;
}
</style>