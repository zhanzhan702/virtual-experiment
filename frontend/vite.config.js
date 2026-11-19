import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    // 视频/图片等二进制素材常被 Windows 索引或播放器占用，Vite 监听时会报
    // EBUSY 并直接退出（dev server 崩溃）。这些文件开发中不会热更新，忽略监听即可。
    watch: {
      ignored: ['**/assets/videos/**']
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
