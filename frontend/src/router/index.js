import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'

const routes = [
  { path: '/', component: LoginView },
  { path: '/register', component: RegisterView },
  { path: '/experiment', component: () => import('@/views/ExperimentView.vue') },
  { path: '/admin', component: () => import('@/views/AdminView.vue') },
  { path: '/HWT', component: () => import('@/views/HighVoltage/HWorkTicketView.vue') },
  { path: '/HTS', component: () => import('@/views/HighVoltage/HToolSelectionView.vue') },
  { path: '/HSO', component: () => import('@/views/HighVoltage/HSceneOverviewView.vue') },
  { path: '/HCL', component: () => import('@/views/HighVoltage/HCabinetLocalView.vue') },
  { path: '/LWT', component: () => import('@/views/LowVoltage/LWorkTicketView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
