import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'test',
      component: () => import('@/views/TestView.vue')
    },
    // 后续可添加实验、管理员等路由
  ]
})

export default router