/* 验证码组件，生成并显示验证码图片，并提供输入框供用户输入验证码。 */

<template>
  <div class="code-row">
    <input v-model="inputCode" placeholder="验证码" class="code-input" />
    <canvas ref="canvasRef" width="120" height="45" @click="refreshCode" class="canvas"/>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
// 父组件可以通过 @updateCode 获取验证码值
const emit = defineEmits(['updateCode'])
const canvasRef = ref()
const inputCode = ref('')
let code = ''
const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'

// 生成验证码并更新父组件的验证码值
function createCode() {
  code = ''
  for(let i=0;i<4;i++){
    code += chars[Math.floor(Math.random()*chars.length)]
  }
  emit('updateCode', code)
  draw()
}


// 绘制验证码图片
function draw(){
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  ctx.clearRect(0,0,canvas.width,canvas.height)
  ctx.fillStyle='#fff'
  ctx.fillRect(0,0,canvas.width,canvas.height)
  ctx.font='28px Arial'
  ctx.fillStyle='#000'
  ctx.fillText(code, 15, 32)
  for(let i=0;i<5;i++){
    ctx.beginPath()
    ctx.moveTo(Math.random()*120, Math.random()*45)
    ctx.lineTo(Math.random()*120, Math.random()*45)
    ctx.stroke()
  }
}
// 刷新验证码
function refreshCode(){ createCode() }

// 初始化时生成验证码
onMounted(()=>{ createCode() })
</script>

<style scoped>
.code-row{
  display:flex;
  gap:10px;
  margin-bottom:20px;
}
.code-input{
  flex:1;
  height:45px;
  padding-left:10px;
}
.canvas{
  cursor:pointer;
  border:1px solid #ddd;
}
</style>