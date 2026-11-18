/* 注册页面视图，显示注册表单。 */

<template>
  <div class="register-page">
    <RegisterForm />
  </div>
</template>

<script setup>
import RegisterForm from '@/components/RegisterForm.vue' //<!-- 注册表单组件 -->
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 40px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 15% 20%, #e8f7f4 0%, transparent 45%),
    radial-gradient(circle at 85% 80%, #eef7f5 0%, transparent 42%),
    #f5f7f9;
}
</style>
