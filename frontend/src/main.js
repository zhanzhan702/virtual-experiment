import './assets/styles/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import Images from '@/constants/images'

// 将图片路径注入为 CSS 变量，全局 CSS 用 var(--img-xxx) 引用
// 更换图片格式只需改 @/constants/images.js 一处
const cssVars = {
  '--img-confirm-btn': Images.confirmButton,
  '--img-confirm-btn-hover': Images.confirmButtonHover,
  '--img-high-voltage-btn': Images.highVoltageButton,
  '--img-high-voltage-btn-hover': Images.highVoltageButtonHover,
  '--img-low-voltage-btn': Images.lowVoltageButton,
  '--img-low-voltage-btn-hover': Images.lowVoltageButtonHover,
  '--img-experiment-bg': Images.experimentViewBg,
  '--img-save-icon': Images.saveProgressIcon,
  '--img-save-icon-hover': Images.saveProgressIconHover,
  '--img-work-task': Images.workTaskButton,
  '--img-work-task-hover': Images.workTaskButtonHover,
  '--img-tool-selection-bg': Images.toolSelectionBg,
  '--img-hwt-bg': Images.hwtBackground
}
Object.entries(cssVars).forEach(([k, v]) => {
  document.documentElement.style.setProperty(k, `url(${v})`)
})

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
