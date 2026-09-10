import './assets/styles/main.css'
// 管理后台主题（青绿 + 浅色），规则限定在 .admin-layout / .admin-dialog 之下
import './assets/styles/admin.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
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
// 中文语言包：分页等组件默认是英文（Total / Go to），需显式指定
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
