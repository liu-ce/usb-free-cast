import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    // 设备列表
    devices: [],
    // WebSocket连接状态
    wsConnected: false,
    // 系统状态
    systemStatus: {
      deviceCount: 0,
      activeConnections: 0,
      activeSessions: 0
    },
    // 网络配置
    networkConfig: {
      baseIp: '192.168.31',
      startRange: 0,
      endRange: 255,
      detectPort: 9801,
      streamPort: 9802
    },
    // 扫描状态
    isScanning: false
  },
  
  mutations: {
    // 设置设备列表
    SET_DEVICES(state, devices) {
      state.devices = devices
    },
    
    // 添加设备
    ADD_DEVICE(state, device) {
      const existingIndex = state.devices.findIndex(d => d.id === device.id)
      if (existingIndex >= 0) {
        Vue.set(state.devices, existingIndex, device)
      } else {
        state.devices.push(device)
      }
    },
    
    // 移除设备
    REMOVE_DEVICE(state, deviceId) {
      state.devices = state.devices.filter(d => d.id !== deviceId)
    },
    
    // 更新设备状态
    UPDATE_DEVICE_STATUS(state, { deviceId, status, connected }) {
      const device = state.devices.find(d => d.id === deviceId)
      if (device) {
        device.status = status
        device.connected = connected
        device.lastUpdate = Date.now()
      }
    },
    
    // 设置WebSocket连接状态
    SET_WS_CONNECTED(state, connected) {
      state.wsConnected = connected
    },
    
    // 设置系统状态
    SET_SYSTEM_STATUS(state, status) {
      state.systemStatus = { ...state.systemStatus, ...status }
    },
    
    // 设置网络配置
    SET_NETWORK_CONFIG(state, config) {
      state.networkConfig = { ...state.networkConfig, ...config }
    },
    
    // 设置扫描状态
    SET_SCANNING(state, isScanning) {
      state.isScanning = isScanning
    }
  },
  
  actions: {
    // 连接WebSocket
    connectWebSocket({ commit, dispatch }) {
      const wsUrl = `${Vue.prototype.$WS_BASE}/ws/screen`
      const ws = new WebSocket(wsUrl)
      
      ws.onopen = () => {
        console.log('WebSocket连接成功')
        commit('SET_WS_CONNECTED', true)
      }
      
      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          dispatch('handleWebSocketMessage', data)
        } catch (error) {
          console.error('解析WebSocket消息失败:', error)
        }
      }
      
      ws.onclose = () => {
        console.log('WebSocket连接关闭')
        commit('SET_WS_CONNECTED', false)
        // 5秒后重连
        setTimeout(() => {
          dispatch('connectWebSocket')
        }, 5000)
      }
      
      ws.onerror = (error) => {
        console.error('WebSocket连接错误:', error)
        commit('SET_WS_CONNECTED', false)
      }
      
      // 保存WebSocket实例
      Vue.prototype.$ws = ws
    },
    
    // 处理WebSocket消息
    handleWebSocketMessage({ commit }, data) {
      switch (data.type) {
        case 'frame': {
          // 处理屏幕帧数据
          const frameEvent = new CustomEvent('screenFrame', {
            detail: {
              deviceId: data.deviceId,
              image: data.image,
              timestamp: data.timestamp
            }
          })
          window.dispatchEvent(frameEvent)
          break
        }
        case 'device_status': {
          // 更新设备状态
          commit('UPDATE_DEVICE_STATUS', {
            deviceId: data.deviceId,
            status: data.status,
            connected: data.connected
          })
          break
        }
        case 'device_list': {
          // 更新设备列表
          commit('SET_DEVICES', data.devices)
          break
        }
        default:
          console.log('未知的WebSocket消息类型:', data.type)
      }
    }
  },
  
  getters: {
    // 获取在线设备
    onlineDevices: state => {
      return state.devices.filter(device => device.connected)
    },
    
    // 获取设备总数
    deviceCount: state => {
      return state.devices.length
    },
    
    // 获取在线设备数
    onlineDeviceCount: state => {
      return state.devices.filter(device => device.connected).length
    }
  }
})