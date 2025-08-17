import Vue from 'vue'
import VueRouter from 'vue-router'
import MobileControl from '@/views/MobileControl.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'MobileControl',
    component: MobileControl
  }
]

const router = new VueRouter({
  mode: 'hash',
  base: process.env.BASE_URL,
  routes
})

export default router