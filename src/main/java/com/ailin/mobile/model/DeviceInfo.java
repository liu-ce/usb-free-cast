package com.ailin.mobile.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {
    
    /**
     * 设备ID（窗口排序号）
     */
    private int id;
    
    /**
     * IP地址
     */
    private String ip;
    
    /**
     * 端口
     */
    private int port;
    
    /**
     * 设备状态：online、offline、connecting
     */
    private String status;
    
    /**
     * 连接状态
     */
    private boolean connected;
    
    /**
     * 最后更新时间
     */
    private long lastUpdate;
    
    /**
     * 设备名称（可选）
     */
    private String deviceName;

    /**
     * 屏幕分辨率（可选，从 /getscreensize 解析）
     */
    private Integer screenWidth;
    private Integer screenHeight;
    
    public DeviceInfo(int id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
        this.status = "offline";
        this.connected = false;
        this.lastUpdate = System.currentTimeMillis();
    }
}