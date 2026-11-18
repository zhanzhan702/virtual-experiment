<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ExperimentLogout from '@/components/ExperimentLogout.vue'

const route = useRoute()
// 仅在实验场景页展示退出登录入口（由路由 meta.experimentScene 标记）
const isExperimentScene = computed(() => route.meta?.experimentScene === true)
</script>

<template>
  <router-view />
  <ExperimentLogout v-if="isExperimentScene" />
</template>
