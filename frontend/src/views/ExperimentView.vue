/* 实验选择页面，选择进行高压场景模拟还是低压场景模拟，以及选择训练模式以及挑战模式的悬浮窗 */

<template>
  <div class="experiment-view">
    <ScenarioSelector @select="onScenarioSelect" />
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import ScenarioSelector from '@/components/ScenarioSelector.vue'

const authStore = useAuthStore()

function onScenarioSelect(type) {
  console.log('选择场景:', type)
}
</script>

<style scoped>
.experiment-view{
    width: 100vw;
    height: 100vh;
    padding: 10vh 10vw;
    background-image:url(@/assets/images/ExperimentViewBackgroundImage.jpg) ;
    background-size: contain;          /* 让图片等比缩放铺满容器，多余部分裁剪 */
    background-position: center;     /* 居中显示 */
    background-repeat: no-repeat;    /* 不重复 */
    display: flex;
    flex-direction: column;
    align-items: center;
}
</style>
