<!-- 登录页面视图，负责布局，包含左侧展示区域和右侧登录表单 -->

<template>
  <div class="login-page">
    <LeftPreview />
    <LoginForm />
  </div>
</template>

<script setup>
import LeftPreview from '@/components/LeftPreview.vue' // 左侧图片组件
import LoginForm from '@/components/LoginForm.vue' // 登录表单组件
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: clamp(40px, 6vw, 100px);
  min-height: 100vh;
  padding: 40px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 15% 20%, #e8f7f4 0%, transparent 45%),
    radial-gradient(circle at 85% 80%, #eef7f5 0%, transparent 42%),
    #f5f7f9;
}

@media (max-width: 1100px) {
  .login-page {
    flex-direction: column;
    gap: 32px;
  }
}
</style>
