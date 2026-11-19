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
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  align-items: safe center;
  min-height: 100vh;
  padding: 48px 24px;
  box-sizing: border-box;
  background-color: #f3f8f7;
  background-image:
    radial-gradient(rgba(26, 179, 148, 0.09) 1px, transparent 1px),
    radial-gradient(900px 520px at 10% 6%, rgba(26, 179, 148, 0.16), transparent 62%),
    radial-gradient(820px 500px at 92% 94%, rgba(64, 158, 255, 0.14), transparent 62%),
    linear-gradient(135deg, #f8fcfb 0%, #f2f7f9 52%, #edf6f4 100%);
  background-size: 26px 26px, auto, auto, auto;
}

/* 背景装饰光斑 */
.register-page::before,
.register-page::after {
  content: '';
  position: fixed;
  border-radius: 50%;
  pointer-events: none;
}

.register-page::before {
  width: 460px;
  height: 460px;
  top: -170px;
  left: -140px;
  background: radial-gradient(circle, rgba(26, 179, 148, 0.2), transparent 66%);
}

.register-page::after {
  width: 420px;
  height: 420px;
  right: -150px;
  bottom: -160px;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.18), transparent 66%);
}
</style>
