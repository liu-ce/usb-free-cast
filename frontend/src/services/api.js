import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: process.env.NODE_ENV === 'development' ? 'http://localhost:8080/api' : '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    console.log('发送请求:', config.method.toUpperCase(), config.url)
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    console.log('收到响应:', response.status, response.config.url)
    return response.data
  },
  error => {
    console.error('响应错误:', error)
    
    let message = '请求失败'
    if (error.response) {
      // 服务器返回错误状态码
      switch (error.response.status) {
        case 400:
          message = '请求参数错误'
          break
        case 401:
          message = '未授权访问'
          break
        case 403:
          message = '访问被禁止'
          break
        case 404:
          message = '请求的资源不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = `服务器错误 (${error.response.status})`
      }
    } else if (error.request) {
      // 网络错误
      message = '网络连接失败，请检查网络设置'
    }
    
    return Promise.reject({ message, error })
  }
)

// API方法
export const mobileApi = {
  // 扫描网络设备
  scanNetwork() {
    return api.post('/mobile/scan')
  },
  
  // 获取设备列表
  getDevices() {
    return api.get('/mobile/devices')
  },
  
  // 获取指定设备信息
  getDevice(deviceId) {
    return api.get(`/mobile/devices/${deviceId}`)
  },
  
  // 移除设备
  removeDevice(deviceId) {
    return api.delete(`/mobile/devices/${deviceId}`)
  },
  
  // 更新网络配置
  updateNetworkConfig(config) {
    return api.post('/mobile/config/network', config)
  },
  
  // 获取系统状态
  getSystemStatus() {
    return api.get('/mobile/status')
  }
}

export default api