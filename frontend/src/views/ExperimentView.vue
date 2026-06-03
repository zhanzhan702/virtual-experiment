<template>
  <div >
    <h1>用电信息采集互动仿真系统</h1>
    <p>欢迎你，{{ authStore.user?.name || '同学' }}！</p>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

</script>

<style scoped>
</style>
