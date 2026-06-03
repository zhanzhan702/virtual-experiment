<template>
  <div>
    <h1>管理后台</h1>
    <p v-if="authStore.roles.length">
      当前角色：{{ authStore.roles.join(' / ') }}
    </p>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<style scoped>

</style>
