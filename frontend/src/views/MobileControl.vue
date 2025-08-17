<template>
  <div class="mobile-control">
    <!-- 头部工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <h1 class="toolbar-title">手机中控系统</h1>
        <div class="connection-status">
          <span class="status-indicator" :class="wsConnected ? 'status-online' : 'status-offline'"></span>
          {{ wsConnected ? '已连接' : '未连接' }}
        </div>
      </div>
      
      <div class="toolbar-center">
        <!-- 统计信息 -->
        <div class="stats-info">
          <span class="stats-item">设备总数: <strong>{{ deviceCount }}</strong></span>
          <span class="stats-item">在线设备: <strong>{{ onlineDeviceCount }}</strong></span>
          <span class="stats-item">离线设备: <strong>{{ deviceCount - onlineDeviceCount }}</strong></span>
        </div>
      </div>
      
      <div class="toolbar-right">
        <!-- 操作按钮 -->
        <el-button 
          size="small"
          type="primary" 
          icon="el-icon-search" 
          :loading="isScanning"
          @click="handleScan"
        >
          {{ isScanning ? '扫描中...' : '扫描设备' }}
        </el-button>
        
        <el-button 
          size="small"
          icon="el-icon-setting" 
          @click="showConfigDialog = true"
        >
          配置
        </el-button>
        
        <el-button 
          size="small"
          icon="el-icon-refresh" 
          @click="handleRefresh"
        >
          刷新
        </el-button>
        
        <el-button 
          size="small"
          icon="el-icon-delete" 
          @click="clearDeviceCache"
        >
          清除缓存
        </el-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧设备列表 -->
      <div class="device-sidebar">
        <div class="sidebar-header">
          <span class="sidebar-title">设备列表</span>
          <el-button 
            type="text" 
            icon="el-icon-plus" 
            size="mini"
            :loading="isScanning"
            @click="handleScan"
          >
            {{ isScanning ? '扫描中' : '扫描' }}
          </el-button>
        </div>
        
        <div class="device-list">
          <DeviceListItem
            v-for="device in sortedDevices"
            :key="device.id"
            :device="device"
            :selected="selectedDevice && selectedDevice.id === device.id"
            @select="handleSelectDevice"
            @remove="handleRemoveDevice"
          />
          
          <!-- 空状态 -->
          <div v-if="devices.length === 0" class="empty-list">
            <el-empty 
              description="暂无设备" 
              :image-size="80"
            >
              <el-button 
                type="primary" 
                size="small"
                @click="handleScan"
              >
                开始扫描
              </el-button>
            </el-empty>
          </div>
        </div>
      </div>
      
      <!-- 右侧屏幕网格 -->
      <div class="screen-area">
        <div class="screen-grid">
          <ScreenWindow
            v-for="device in displayDevices"
            :key="device.id"
            :device="device"
            @select="handleSelectDevice"
          />
        </div>
      </div>
    </div>
    
    <!-- 底部状态栏 -->
    <StatusBar />

    <!-- 网络配置对话框 -->
    <el-dialog
      title="网络配置"
      :visible.sync="showConfigDialog"
      width="500px"
    >
      <div class="config-panel" style="margin: 0; box-shadow: none;">
        <div class="config-row">
          <label class="config-label">基础IP:</label>
          <el-input
            v-model="configForm.baseIp"
            placeholder="例如：192.168.31"
            style="flex: 1;"
          />
        </div>
        
        <div class="config-row">
          <label class="config-label">起始范围:</label>
          <el-input-number
            v-model="configForm.startRange"
            :min="0"
            :max="255"
            style="flex: 1;"
          />
        </div>
        
        <div class="config-row">
          <label class="config-label">结束范围:</label>
          <el-input-number
            v-model="configForm.endRange"
            :min="0"
            :max="255"
            style="flex: 1;"
          />
        </div>
        
        <div class="config-row">
          <label class="config-label">检测端口:</label>
          <el-input-number
            v-model="configForm.detectPort"
            :min="1"
            :max="65535"
            style="flex: 1;"
          />
        </div>
        
        <div class="config-row">
          <label class="config-label">投屏端口:</label>
          <el-input-number
            v-model="configForm.streamPort"
            :min="1"
            :max="65535"
            style="flex: 1;"
          />
        </div>
      </div>
      
      <div slot="footer">
        <el-button @click="showConfigDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateConfig">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { mapState, mapGetters, mapActions, mapMutations } from 'vuex'
import DeviceListItem from '@/components/DeviceListItem.vue'
import ScreenWindow from '@/components/ScreenWindow.vue'
import StatusBar from '@/components/StatusBar.vue'
import { mobileApi } from '@/services/api'

export default {
  name: 'MobileControl',
  components: {
    DeviceListItem,
    ScreenWindow,
    StatusBar
  },
  
  data() {
    return {
      showConfigDialog: false,
      selectedDevice: null,
      configForm: {
        baseIp: '192.168.31',
        startRange: 0,
        endRange: 255,
        detectPort: 9801,
        streamPort: 9802
      }
    }
  },
  
  computed: {
    ...mapState(['devices', 'wsConnected', 'networkConfig', 'isScanning']),
    ...mapGetters(['deviceCount', 'onlineDeviceCount']),
    
    // 按ID排序的设备列表
    sortedDevices() {
      return [...this.devices].sort((a, b) => a.id - b.id)
    },
    
    // 显示在屏幕网格中的设备（限制数量）
    displayDevices() {
      return this.sortedDevices
    },
    
  },
  
  created() {
    this.init()
  },
  
  beforeDestroy() {
    // 关闭WebSocket连接
    if (this.$ws) {
      this.$ws.close()
    }
  },
  
  methods: {
    ...mapActions(['connectWebSocket']),
    ...mapMutations(['SET_NETWORK_CONFIG', 'SET_SCANNING']),
    
    // 初始化
    async init() {
      // 连接WebSocket
      this.connectWebSocket()
      
      // 加载网络配置
      this.configForm = { ...this.networkConfig }
      
      // 加载缓存的设备
      this.loadCachedDevices()
    },
    

    
    // 扫描设备（调用后端API）
    async handleScan() {
      if (this.isScanning) return
      
      this.SET_SCANNING(true)
      
      try {
        // 清空现有设备列表
        this.$store.commit('SET_DEVICES', [])
        
        this.$message.info('开始扫描局域网设备...')
        
        const result = await mobileApi.scanNetwork()
        
        if (result.success) {
          this.$message.success(`扫描完成，发现 ${result.count} 个设备`)
          this.$store.commit('SET_DEVICES', result.devices)
          
          // 缓存到本地存储
          this.saveDevicesToCache(result.devices)
        } else {
          this.$message.error(result.message || '扫描失败')
        }
        
      } catch (error) {
        console.error('扫描设备失败:', error)
        this.$message.error(error.message || '扫描设备失败')
      } finally {
        this.SET_SCANNING(false)
      }
    },
    
    // 刷新设备列表
    async handleRefresh() {
      // 直接重新加载缓存的设备
      this.loadCachedDevices()
      this.$message.success('设备列表已刷新')
    },
    
    // 移除设备
    async handleRemoveDevice(deviceId) {
      try {
        await this.$confirm('确定要移除这个设备吗？', '确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // 从前端设备列表中移除
        this.$store.commit('REMOVE_DEVICE', deviceId)
        
        // 更新缓存
        const currentDevices = this.$store.state.devices
        this.saveDevicesToCache(currentDevices)
        
        this.$message.success('设备已移除')
        
      } catch (error) {
        if (error !== 'cancel') {
          console.error('移除设备失败:', error)
          this.$message.error('移除设备失败')
        }
      }
    },
    
    // 更新网络配置
    async handleUpdateConfig() {
      try {
        const result = await mobileApi.updateNetworkConfig(this.configForm)
        
        if (result.success) {
          this.SET_NETWORK_CONFIG(this.configForm)
          this.showConfigDialog = false
          this.$message.success('网络配置已更新')
        } else {
          this.$message.error(result.message || '更新配置失败')
        }
      } catch (error) {
        console.error('更新网络配置失败:', error)
        this.$message.error(error.message || '更新配置失败')
      }
    },
    
    // 选择设备
    handleSelectDevice(device) {
      this.selectedDevice = device
    },
    

    
    // 保存设备到缓存
    saveDevicesToCache(devices) {
      try {
        const cacheData = {
          devices: devices,
          timestamp: Date.now(),
          config: this.networkConfig
        }
        localStorage.setItem('mobile_control_devices', JSON.stringify(cacheData))
        console.log('设备列表已缓存')
      } catch (error) {
        console.error('保存设备缓存失败:', error)
      }
    },
    
    // 从缓存加载设备
    loadCachedDevices() {
      try {
        const cached = localStorage.getItem('mobile_control_devices')
        if (cached) {
          const cacheData = JSON.parse(cached)
          
          // 检查缓存是否过期（24小时）
          const now = Date.now()
          const cacheAge = now - cacheData.timestamp
          const maxAge = 24 * 60 * 60 * 1000 // 24小时
          
          if (cacheAge < maxAge && cacheData.devices && cacheData.devices.length > 0) {
            // 保留后端提供的设备ID和状态，避免刷新后ID错乱、状态变离线
            const devices = cacheData.devices
            this.$store.commit('SET_DEVICES', devices)
            this.$message.info(`从缓存加载了 ${devices.length} 个设备`)
            
            // 可选：验证缓存的设备是否仍然在线
            // this.verifyCachedDevices(devices)
          } else {
            // 清除过期缓存
            localStorage.removeItem('mobile_control_devices')
          }
        }
      } catch (error) {
        console.error('加载设备缓存失败:', error)
        localStorage.removeItem('mobile_control_devices')
      }
    },
    
    // 清除设备缓存
    clearDeviceCache() {
      localStorage.removeItem('mobile_control_devices')
      this.$message.success('设备缓存已清除')
    }
  }
}
</script>

<style scoped>
.mobile-control {
  height: 100vh;
  background-color: #f0f2f5;
  display: flex;
  flex-direction: column;
}

/* 工具栏样式 */
.toolbar {
  height: 60px;
  background: #001529;
  color: white;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 100;
}

.toolbar-left {
  flex: 0 0 auto;
}

.toolbar-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: white;
}

.connection-status {
  font-size: 12px;
  margin-top: 4px;
  color: #ccc;
}

.toolbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.stats-info {
  display: flex;
  gap: 24px;
}

.stats-item {
  color: #ccc;
  font-size: 13px;
}

.stats-item strong {
  color: #1890ff;
  margin-left: 4px;
}

.toolbar-right {
  flex: 0 0 auto;
  display: flex;
  gap: 8px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧设备列表 */
.device-sidebar {
  width: 280px;
  background: white;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  height: 50px;
  padding: 0 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fafafa;
}

.sidebar-title {
  font-weight: 600;
  color: #262626;
}

.device-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.empty-list {
  padding: 40px 20px;
  text-align: center;
}

/* 右侧屏幕区域 */
.screen-area {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: #f5f5f5;
}

.screen-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  max-width: 100%;
  align-items: flex-start;
}

/* 空窗口样式 */
.empty-window {
  aspect-ratio: 16/9;
  background: #fafafa;
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bfbfbf;
  transition: all 0.3s ease;
}

.empty-window:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.empty-window-content {
  text-align: center;
}

.empty-window-content i {
  font-size: 24px;
  display: block;
  margin-bottom: 8px;
}

.empty-window-content span {
  font-size: 12px;
  font-weight: 500;
}

/* 响应式布局 */
@media (max-width: 1400px) {
  /* flex布局自适应，无需额外设置 */
}

@media (max-width: 1200px) {
  /* flex布局自适应，无需额外设置 */
  
  .device-sidebar {
    width: 240px;
  }
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    height: auto;
    padding: 12px 16px;
  }
  
  .toolbar-center {
    margin: 8px 0;
  }
  
  .stats-info {
    flex-direction: column;
    gap: 8px;
  }
  
  .main-content {
    flex-direction: column;
  }
  
  .device-sidebar {
    width: 100%;
    height: 200px;
  }
  
  /* flex布局自适应 */
}

@media (max-width: 480px) {
  /* flex布局自适应 */
}
</style>