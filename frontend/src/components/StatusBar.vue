<template>
  <div class="status-bar">
    <div class="status-left">
      <span class="status-item">
        状态: 
        <span class="status-value" :style="{ color: statusColor }">{{ statusText }}</span>
      </span>
      
      <span class="status-item">
        在线设备: 
        <span class="status-value success">{{ onlineCount }}</span>
      </span>
      
      <span class="status-item">
        离线设备: 
        <span class="status-value error">{{ offlineCount }}</span>
      </span>
      
      <span class="status-item">
        总设备数: 
        <span class="status-value">{{ totalCount }}</span>
      </span>
    </div>
    
    <div class="status-right">
      <span class="status-item">
        刷新时间: {{ updateTime }}
      </span>
    </div>
  </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex'

export default {
  name: 'StatusBar',
  
  data() {
    return {
      updateTime: ''
    }
  },
  
  computed: {
    ...mapState(['wsConnected']),
    ...mapGetters(['deviceCount', 'onlineDeviceCount']),
    
    statusText() {
      return this.wsConnected ? '正常' : '断开'
    },
    
    statusColor() {
      return this.wsConnected ? '#52c41a' : '#f5222d'
    },
    
    totalCount() {
      return this.deviceCount
    },
    
    onlineCount() {
      return this.onlineDeviceCount
    },
    
    offlineCount() {
      return this.deviceCount - this.onlineDeviceCount
    }
  },
  
  mounted() {
    this.updateCurrentTime()
    this.timer = setInterval(this.updateCurrentTime, 1000)
  },
  
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer)
    }
  },
  
  methods: {
    updateCurrentTime() {
      const now = new Date()
      this.updateTime = now.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }
  }
}
</script>

<style scoped>
.status-bar {
  height: 32px;
  background: #f0f0f0;
  border-top: 1px solid #d9d9d9;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.status-right {
  display: flex;
  align-items: center;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-value {
  font-weight: 600;
  color: #262626;
}

.status-value.success {
  color: #52c41a;
}

.status-value.error {
  color: #f5222d;
}

@media (max-width: 768px) {
  .status-bar {
    flex-direction: column;
    height: auto;
    padding: 8px 16px;
    gap: 4px;
  }
  
  .status-left {
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>