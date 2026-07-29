import './assets/styles/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { cssVars } from '@/constants/images'

// 将图片路径注入为 CSS 变量，CSS 中用 var(--img-xxx) 引用
// 更换图片格式只需改 @/constants/images.js 一处
Object.entries(cssVars).forEach(([k, v]) => {
  document.documentElement.style.setProperty(k, `url(${v})`)
})

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
