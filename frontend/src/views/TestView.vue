<template>
    <div style="padding: 30px">
        <h1>前后端连通测试</h1>
        <el-button type="primary" @click="fetchHello">点我连接后端</el-button>
        <p style="margin-top: 20px; color: green">{{ message }}</p>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const message = ref('')
const fetchHello = async () => {
    try {
        const res = await axios.get('/api/hello')
        message.value = res.data
    } catch (e) {
        message.value = '连接失败：' + e.message
    }
}
</script>