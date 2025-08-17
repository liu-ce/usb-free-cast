<template>
  <div 
    class="device-list-item"
    :class="{ 
      'selected': selected, 
      'online': device.connected,
      'offline': !device.connected 
    }"
    @click="handleSelect"
  >
    <div class="device-info">
      <div class="device-header">
        <span class="device-number">{{ String(device.id).padStart(2, '0') }}</span>
        <span class="device-group">组{{ Math.ceil(device.id / 5) }}</span>
        <div class="device-actions">
          <el-dropdown @command="handleCommand" trigger="click">
            <el-button type="text" icon="el-icon-more" size="mini"></el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="remove">移除设备</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
      
      <div class="device-details">
        <div class="device-ip">{{ device.ip }}:{{ device.port }}</div>
        <div class="device-status">
          <span class="status-dot" :class="statusClass"></span>
          <span class="status-text">{{ statusText }}</span>
        </div>
      </div>
      
      <div class="device-stats" v-if="device.connected">
        <span class="stat-item">
          <i class="el-icon-time"></i>
          {{ lastUpdateText }}
        </span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DeviceListItem',
  
  props: {
    device: {
      type: Object,
      required: true
    },
    selected: {
      type: Boolean,
      default: false
    }
  },
  
  computed: {
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
        return '从未更新'
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
  
  methods: {
    handleSelect() {
      this.$emit('select', this.device)
    },
    
    handleCommand(command) {
      switch (command) {
        case 'remove':
          this.$emit('remove', this.device.id)
          break
      }
    }
  }
}
</script>

<style scoped>
.device-list-item {
  margin: 0 8px 4px 8px;
  padding: 12px;
  background: white;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.device-list-item:hover {
  border-color: #d9d9d9;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.device-list-item.selected {
  border-color: #1890ff;
  background: #f6ffed;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.2);
}

.device-list-item.online {
  border-left: 4px solid #52c41a;
}

.device-list-item.offline {
  border-left: 4px solid #f5222d;
}

.device-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.device-number {
  background: #1890ff;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  font-family: 'Courier New', monospace;
  margin-right: 8px;
  min-width: 32px;
  text-align: center;
}

.device-group {
  background: #f5f5f5;
  color: #666;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 11px;
  margin-right: auto;
}

.device-actions {
  opacity: 0;
  transition: opacity 0.2s ease;
}

.device-list-item:hover .device-actions {
  opacity: 1;
}

.device-details {
  margin-bottom: 8px;
}

.device-ip {
  font-size: 12px;
  color: #666;
  font-family: 'Courier New', monospace;
  margin-bottom: 4px;
}

.device-status {
  display: flex;
  align-items: center;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
}

.status-dot.status-online {
  background-color: #52c41a;
}

.status-dot.status-offline {
  background-color: #f5222d;
}

.status-dot.status-connecting {
  background-color: #faad14;
  animation: pulse 1.5s infinite;
}

.status-text {
  font-size: 11px;
  color: #666;
  font-weight: 500;
}

.device-stats {
  border-top: 1px solid #f5f5f5;
  padding-top: 6px;
  margin-top: 6px;
}

.stat-item {
  font-size: 10px;
  color: #999;
  display: flex;
  align-items: center;
}

.stat-item i {
  margin-right: 4px;
  font-size: 10px;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}
</style>