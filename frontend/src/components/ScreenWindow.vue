<template>
  <div 
    class="screen-window"
    :style="containerStyle"
    :class="{ 
      'online': device.connected,
      'offline': !device.connected,
      'has-frame': hasFrame
    }"
    @click="handleClick"
  >
    <!-- 窗口头部 -->
    <div class="window-header">
      <span class="window-number">{{ String(device.id).padStart(2, '0') }}</span>
    </div>
    
    <!-- 屏幕显示区域 -->
    <div class="screen-display">
      <!-- Canvas画布 -->
      <canvas
        v-show="hasFrame"
        ref="screenCanvas"
        class="screen-canvas rotated"
      ></canvas>
      
      <!-- 占位符 -->
      <div v-show="!hasFrame" class="screen-placeholder">
        <div v-if="device.connected" class="placeholder-loading">
          <i class="el-icon-loading"></i>
          <div>等待画面</div>
        </div>
        <div v-else class="placeholder-offline">
          <i class="el-icon-warning-outline"></i>
          <div>设备离线</div>
        </div>
      </div>
      
      <!-- 覆盖信息 -->
      <div class="screen-overlay">
        <div class="overlay-info">
          <div class="device-ip">{{ device.ip }}</div>
          <div class="last-update" v-if="device.lastUpdate">
            {{ lastUpdateText }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ScreenWindow',
  
  props: {
    device: {
      type: Object,
      required: true
    }
  },
  
  data() {
    return {
      hasFrame: false,
      frameCount: 0,
      frameRate: 0,
      lastFrameTime: 0,
      frameRateTimer: null,
      imgNaturalWidth: 0,
      imgNaturalHeight: 0
    }
  },
  
  watch: {
    // 监听设备连接状态变化
    'device.connected'(newVal) {
      if (!newVal) {
        // 设备断开连接时，重置画面状态
        this.hasFrame = false
      }
    }
  },
  
  computed: {
    // 容器尺寸：跟图片实际尺寸一样宽高
    containerStyle() {
      // 使用图片的实际尺寸作为容器尺寸，因为图片旋转了-90°，所以容器用旋转后的尺寸
      if (this.imgNaturalWidth && this.imgNaturalHeight) {
        // 图片旋转-90°后，宽高互换
        return {
          width: `${this.imgNaturalHeight}px`,
          height: `${this.imgNaturalWidth}px`,
          aspectRatio: 'auto'
        }
      }
      // 如果有设备尺寸信息，优先使用
      if (this.device.screenWidth && this.device.screenHeight) {
        // 图片旋转-90°后，宽高互换
        return {
          width: `${this.device.screenHeight}px`, 
          height: `${this.device.screenWidth}px`,
          aspectRatio: 'auto'
        }
      }
      // 默认尺寸
      return { aspectRatio: '9 / 16' }
    },
    statusClass() {
      if (this.device.connected) {
        return 'status-online'
      } else if (this.device.status === 'connecting') {
        return 'status-connecting'
      } else {
        return 'status-offline'
      }
    },
    
    statusText() {
      if (this.device.connected) {
        return '在线'
      } else if (this.device.status === 'connecting') {
        return '连接中'
      } else {
        return '离线'
      }
    },
    
    lastUpdateText() {
      if (!this.device.lastUpdate) {
        return ''
      }
      
      const now = Date.now()
      const diff = now - this.device.lastUpdate
      
      if (diff < 1000) {
        return '刚刚'
      } else if (diff < 60000) {
        return `${Math.floor(diff / 1000)}s前`
      } else if (diff < 3600000) {
        return `${Math.floor(diff / 60000)}m前`
      } else {
        return new Date(this.device.lastUpdate).toLocaleTimeString()
      }
    }
  },
  
  mounted() {
    this.initFrameListener()
    this.startFrameRateCalculation()
    // 初始化canvas
    this.$nextTick(() => {
      if (this.$refs.screenCanvas) {
        const canvas = this.$refs.screenCanvas
        // 设置默认尺寸，避免显示异常
        canvas.width = 100
        canvas.height = 100
      }
    })
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
      this.drawImageToCanvas(image)
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

    // 使用Canvas绘制图片
    drawImageToCanvas(dataUrl) {
      if (!this.$refs.screenCanvas) return
      
      const canvas = this.$refs.screenCanvas
      const ctx = canvas.getContext('2d')
      
      try {
        // 提取base64数据部分
        const base64Data = dataUrl.replace(/^data:image\/jpeg;base64,/, '')
        
        // 将base64转换为二进制数据
        const binaryString = atob(base64Data)
        const bytes = new Uint8Array(binaryString.length)
        for (let i = 0; i < binaryString.length; i++) {
          bytes[i] = binaryString.charCodeAt(i)
        }
        
        // 创建Blob对象
        const blob = new Blob([bytes], { type: 'image/jpeg' })
        
        // 使用createImageBitmap API（避免网络面板显示）
        createImageBitmap(blob).then(imageBitmap => {
          // 更新图片尺寸信息（用于容器尺寸计算）
          this.imgNaturalWidth = imageBitmap.width
          this.imgNaturalHeight = imageBitmap.height
          
          // 设置canvas内部分辨率为图片原始尺寸
          canvas.width = imageBitmap.width
          canvas.height = imageBitmap.height
          
          // 清除canvas
          ctx.clearRect(0, 0, canvas.width, canvas.height)
          
          // 绘制图片
          ctx.drawImage(imageBitmap, 0, 0)
          
          // 清理ImageBitmap资源
          imageBitmap.close()
          
          // 设置有画面标志
          if (!this.hasFrame) {
            this.hasFrame = true
          }
        }).catch(error => {
          console.error(`设备 ${this.device.id} Canvas图片处理失败:`, error)
          this.hasFrame = false
        })
        
      } catch (error) {
        console.error(`设备 ${this.device.id} base64解码失败:`, error)
        this.hasFrame = false
      }
    },
    
    // 开始帧率计算
    startFrameRateCalculation() {
      this.frameRateTimer = setInterval(() => {
        // 如果超过2秒没有新帧，重置帧率和画面状态
        const now = Date.now()
        if (now - this.lastFrameTime > 2000) {
          this.frameRate = 0
          // 长时间没有新帧时，重置画面状态
          if (this.hasFrame && (now - this.lastFrameTime > 5000)) {
            this.hasFrame = false
          }
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
    

    
    // 点击窗口
    handleClick() {
      this.$emit('select', this.device)
    }
  }
}
</script>

<style scoped>
.screen-window {
  background: #000;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.screen-window:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.screen-window.online {
  border-color: #52c41a;
}

.screen-window.offline {
  border-color: #f5222d;
  background: #1a1a1a;
}

.screen-window.has-frame {
  background: transparent;
}

/* 窗口头部 */
.window-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 28px;
  background: transparent;
  color: white;
  display: flex;
  align-items: center;
  padding: 0 8px;
  z-index: 10;
  font-size: 12px;
}

.window-number {
  background: #1890ff;
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: 600;
  font-family: 'Courier New', monospace;
  margin-right: 8px;
  min-width: 24px;
  text-align: center;
  font-size: 11px;
}

/* 移除状态文字样式 */

.window-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 移除fps样式 */

/* 屏幕显示区域 */
.screen-display {
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.screen-canvas {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #000;
  max-height: 180%;
  display: block;
}

/* 逆时针旋转90度 */
.screen-canvas.rotated {
  transform: rotate(-90deg);
  transform-origin: center center;
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: 180%;
}

/* 占位符 */
.screen-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  text-align: center;
}

.placeholder-loading,
.placeholder-offline {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.placeholder-loading i {
  font-size: 20px;
  color: #1890ff;
}

.placeholder-offline i {
  font-size: 20px;
  color: #f5222d;
}

.placeholder-loading div,
.placeholder-offline div {
  font-size: 11px;
  color: #999;
}

/* 覆盖信息 */
.screen-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  color: white;
  padding: 20px 8px 6px 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.screen-window:hover .screen-overlay {
  opacity: 1;
}

.overlay-info {
  text-align: center;
}

.device-ip {
  font-size: 10px;
  font-family: 'Courier New', monospace;
  margin-bottom: 2px;
}

.last-update {
  font-size: 9px;
  color: #ccc;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .window-header {
    height: 24px;
    font-size: 11px;
  }
  
  .window-number {
    font-size: 10px;
    padding: 1px 4px;
  }
  
  .frame-rate {
    font-size: 9px;
  }
}
</style>