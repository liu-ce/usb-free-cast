<template>
  <div class="device-card">
    <!-- 设备头部信息 -->
    <div class="device-card-header">
      <div class="flex" style="align-items: center;">
        <span class="device-id">#{device.id}</span>
        <span class="device-ip">{{ device.ip }}:{{ device.port }}</span>
      </div>
      
      <div class="flex" style="align-items: center; gap: 8px;">
        <!-- 状态指示器 -->
        <span class="status-indicator" :class="statusClass"></span>
        <span class="status-text" :style="{color: statusColor}">{{ statusText }}</span>
        
        <!-- 操作菜单 -->
        <el-dropdown @command="handleCommand">
          <el-button type="text" icon="el-icon-more" size="mini"></el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="refresh">刷新</el-dropdown-item>
            <el-dropdown-item command="remove" divided>移除设备</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </div>
    
    <!-- 设备屏幕显示区域 -->
    <div class="device-card-body">
      <div class="device-screen" ref="screenContainer">
        <!-- 屏幕画面 -->
        <img
          v-if="currentFrame"
          :src="currentFrame"
          alt="设备屏幕"
          @load="onImageLoad"
          @error="onImageError"
        />
        
        <!-- 占位符 -->
        <div v-else class="device-screen-placeholder">
          <div v-if="device.connected">
            <i class="el-icon-loading"></i>
            <div style="margin-top: 8px;">等待画面...</div>
          </div>
          <div v-else>
            <i class="el-icon-warning"></i>
            <div style="margin-top: 8px;">设备离线</div>
          </div>
        </div>
        
        <!-- 帧率显示 -->
        <div v-if="frameRate > 0" class="frame-rate">
          {{ frameRate }} FPS
        </div>
      </div>
      
      <!-- 设备信息 -->
      <div class="device-info">
        <div class="info-row">
          <span class="info-label">设备名称:</span>
          <span class="info-value">{{ device.deviceName || '未知设备' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">最后更新:</span>
          <span class="info-value">{{ lastUpdateText }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DeviceCard',
  
  props: {
    device: {
      type: Object,
      required: true
    }
  },
  
  data() {
    return {
      currentFrame: null,
      frameCount: 0,
      frameRate: 0,
      lastFrameTime: 0,
      frameRateTimer: null
    }
  },
  
  computed: {
    // 状态样式类
    statusClass() {
      if (this.device.connected) {
        return 'status-online'
      } else if (this.device.status === 'connecting') {
        return 'status-connecting'
      } else {
        return 'status-offline'
      }
    },
    
    // 状态颜色
    statusColor() {
      if (this.device.connected) {
        return '#52c41a'
      } else if (this.device.status === 'connecting') {
        return '#faad14'
      } else {
        return '#f5222d'
      }
    },
    
    // 状态文本
    statusText() {
      if (this.device.connected) {
        return '在线'
      } else if (this.device.status === 'connecting') {
        return '连接中'
      } else {
        return '离线'
      }
    },
    
    // 最后更新时间文本
    lastUpdateText() {
      if (!this.device.lastUpdate) {
        return '从未更新'
      }
      
      const now = Date.now()
      const diff = now - this.device.lastUpdate
      
      if (diff < 1000) {
        return '刚刚'
      } else if (diff < 60000) {
        return `${Math.floor(diff / 1000)}秒前`
      } else if (diff < 3600000) {
        return `${Math.floor(diff / 60000)}分钟前`
      } else {
        return new Date(this.device.lastUpdate).toLocaleTimeString()
      }
    }
  },
  
  mounted() {
    this.initFrameListener()
    this.startFrameRateCalculation()
  },
  
  beforeDestroy() {
    this.removeFrameListener()
    this.stopFrameRateCalculation()
  },
  
  methods: {
    // 初始化帧监听器
    initFrameListener() {
      this.handleScreenFrame = (event) => {
        const { deviceId, image, timestamp } = event.detail
        if (deviceId === this.device.id) {
          this.updateFrame(image, timestamp)
        }
      }
      
      window.addEventListener('screenFrame', this.handleScreenFrame)
    },
    
    // 移除帧监听器
    removeFrameListener() {
      if (this.handleScreenFrame) {
        window.removeEventListener('screenFrame', this.handleScreenFrame)
      }
    },
    
    // 更新帧数据
    updateFrame(image, timestamp) {
      this.currentFrame = image
      this.frameCount++
      
      // 计算帧率
      const now = Date.now()
      if (this.lastFrameTime > 0) {
        const timeDiff = now - this.lastFrameTime
        if (timeDiff > 0) {
          const instantFps = 1000 / timeDiff
          this.frameRate = Math.round(instantFps * 10) / 10
        }
      }
      this.lastFrameTime = now
    },
    
    // 开始帧率计算
    startFrameRateCalculation() {
      this.frameRateTimer = setInterval(() => {
        // 如果超过2秒没有新帧，重置帧率
        const now = Date.now()
        if (now - this.lastFrameTime > 2000) {
          this.frameRate = 0
        }
      }, 1000)
    },
    
    // 停止帧率计算
    stopFrameRateCalculation() {
      if (this.frameRateTimer) {
        clearInterval(this.frameRateTimer)
        this.frameRateTimer = null
      }
    },
    
    // 图片加载成功
    onImageLoad() {
      // 可以在这里添加图片加载成功的处理逻辑
    },
    
    // 图片加载失败
    onImageError() {
      console.error(`设备 ${this.device.id} 图片加载失败`)
    },
    
    // 处理下拉菜单命令
    handleCommand(command) {
      switch (command) {
        case 'refresh':
          this.handleRefresh()
          break
        case 'remove':
          this.handleRemove()
          break
      }
    },
    
    // 刷新设备
    handleRefresh() {
      this.$message.info(`刷新设备 ${this.device.id}`)
      // 清除当前帧，等待新的帧数据
      this.currentFrame = null
      this.frameCount = 0
      this.frameRate = 0
    },
    
    // 移除设备
    handleRemove() {
      this.$emit('remove', this.device.id)
    }
  }
}
</script>

<style scoped>
.device-id {
  background: #1890ff;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  margin-right: 8px;
}

.device-ip {
  font-size: 13px;
  color: #666;
  font-family: 'Courier New', monospace;
}

.status-text {
  font-size: 12px;
  font-weight: 500;
}

.device-screen {
  position: relative;
}

.frame-rate {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-family: 'Courier New', monospace;
}

.device-info {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 12px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  color: #666;
  font-weight: 500;
}

.info-value {
  color: #262626;
  font-family: 'Courier New', monospace;
}

.device-screen-placeholder {
  color: #999;
  text-align: center;
  font-size: 14px;
}

.device-screen-placeholder i {
  font-size: 24px;
  display: block;
}
</style>