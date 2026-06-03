/*管理整个项目的路由，定义登录页和注册页的路径，方便页面跳转。  故项目在运转时要下载Vue Router4才能与运行*/
/*同时由于我对于页面的第一反映是左右的格式，所以初步的想法是左右形式的，没有考虑标题等情况 */

import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'

const routes = [
  { path: '/', component: LoginView },             // 默认访问 '/' 跳转到登录页
  { path: '/register', component: RegisterView }    // '/register' 跳转到注册页
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router