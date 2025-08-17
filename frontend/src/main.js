import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'

// Element UI
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'

// 自定义样式
import './assets/css/global.css'

// 配置
Vue.config.productionTip = false

// 使用Element UI
Vue.use(ElementUI)

// 全局配置
Vue.prototype.$API_BASE = process.env.NODE_ENV === 'development' 
  ? 'http://localhost:8080/api' 
  : '/api'

Vue.prototype.$WS_BASE = process.env.NODE_ENV === 'development' 
  ? 'ws://localhost:8080/api' 
  : `ws://${window.location.host}/api`

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')