<template>
    <div class="scenario-selector">
        <div class="scenario-btn high" @click="select('high')"></div>
        <div class="scenario-btn low" @click="select('low')"></div>
    </div>
</template>

<script setup>
const emit = defineEmits(['select'])

function select(type) {
    emit('select', type)
}
</script>

<style scoped>
.scenario-selector {
    display: flex;
    padding-top: 40vh;
    gap: 48px;
    justify-content: center;
    align-items: center;
}

.scenario-btn {
    width: 300px;
    height: 133px;
    cursor: pointer;
    border-radius: 12px;
    transition: transform 0.2s;
    background-size: contain;
    background-repeat: no-repeat;
    background-position: center;
}

/* 高压 — 默认图片 */
.scenario-btn.high {
    background-image: url(@/assets/images/HighVoltageButton.png);
}

/* TODO: 高压 — 悬停图片（制作好后替换路径） */
.scenario-btn.high:hover {
    background-image: url(@/assets/images/HighVoltageButton.png);
    transform: scale(1.05);
}

.scenario-btn.high:active {
    transform: scale(0.9);
}

/* 低压 — 默认图片 */
.scenario-btn.low {
    background-image: url(@/assets/images/LowVoltageButton.png);
}

/* TODO: 低压 — 悬停图片（制作好后替换路径） */
.scenario-btn.low:hover {
    background-image: url(@/assets/images/LowVoltageButton.png);
    transform: scale(1.05);
}

.scenario-btn.low:active {
    transform: scale(0.9);
}
</style>
