<template>
  <div class="experiment-view">
    <h1>用电信息采集互动仿真系统</h1>
    <p>欢迎你，{{ authStore.user?.name || '同学' }}！</p>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

</script>

<style scoped>
.experiment-view{
    width: 100vw;
    height: 100vh;
    background-image:url(src/assets/images/ExperimentViewBackgroundImage.jpg) ;
    background-size: cover;          /* 让图片等比缩放铺满容器，多余部分裁剪 */
    background-position: center;     /* 居中显示 */
    background-repeat: no-repeat;    /* 不重复 */
}
</style>
