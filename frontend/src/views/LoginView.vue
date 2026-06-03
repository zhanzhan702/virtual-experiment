/* 登录页面视图，负责布局，包含左侧展示区域和右侧登录表单 */

<template>
  <div class="login-page">
    <LeftPreview />
    <LoginForm />
  </div>
</template>

<script setup>
import LeftPreview from '@/components/LeftPreview.vue'   // 左侧图片组件
import LoginForm from '@/components/LoginForm.vue'       // 登录表单组件
</script>

<style scoped>
.login-page{
  display:flex;
  justify-content:center;
  align-items:center;
  gap:120px;
  min-height:100vh;
  background:#181b22;
}
</style>