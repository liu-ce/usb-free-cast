package com.ailin.mobile.service;

import com.ailin.mobile.model.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 屏幕捕获服务
 */
@Slf4j
@Service
public class ScreenCaptureService {
    
    @Autowired
    private WebSocketService webSocketService;
    
    /**
     * 设备连接映射
     */
    private final ConcurrentHashMap<Integer, Socket> deviceConnections = new ConcurrentHashMap<>();
    
    /**
     * 线程池
     */
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * 开始捕获指定设备的屏幕
     */
    @Async
    public void startCapture(DeviceInfo device) {
        int deviceId = device.getId();
        String ip = device.getIp();
        int port = device.getPort();
        
        // 检查是否已经在捕获
        if (deviceConnections.containsKey(deviceId)) {
            log.warn("设备 {} 已经在捕获中", deviceId);
            return;
        }
        
        executorService.submit(() -> {
            Socket socket = null;
            try {
                log.info("开始连接设备进行屏幕捕获: {}:{} (ID: {})", ip, port, deviceId);
                
                socket = new Socket(ip, port);
                socket.setKeepAlive(true);
                socket.setSoTimeout(30000); // 30秒超时
                
                deviceConnections.put(deviceId, socket);
                
                // 更新设备状态
                device.setConnected(true);
                device.setStatus("online");
                device.setLastUpdate(System.currentTimeMillis());
                
                // 开始接收数据
                captureFrames(device, socket);
                
            } catch (Exception e) {
                log.error("设备 {} 屏幕捕获异常: {}", deviceId, e.getMessage());
                device.setConnected(false);
                device.setStatus("offline");
            } finally {
                // 清理连接
                deviceConnections.remove(deviceId);
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        log.error("关闭socket连接失败", e);
                    }
                }
                log.info("设备 {} 屏幕捕获已停止", deviceId);
            }
        });
    }
    
    /**
     * 捕获帧数据
     */
    private void captureFrames(DeviceInfo device, Socket socket) throws IOException {
        int deviceId = device.getId();
        InputStream inputStream = socket.getInputStream();
        
        byte[] buffer = new byte[0];
        int frameCount = 0;
        
        log.info("开始接收设备 {} 的屏幕数据", deviceId);
        
        while (!socket.isClosed() && socket.isConnected()) {
            try {
                // 读取数据
                byte[] tempBuffer = new byte[4096];
                int bytesRead = inputStream.read(tempBuffer);
                
                if (bytesRead == -1) {
                    log.warn("设备 {} 连接已断开", deviceId);
                    break;
                }
                
                // 将新数据添加到缓冲区
                buffer = appendBytes(buffer, tempBuffer, bytesRead);
                
                // 查找JPEG头尾
                while (true) {
                    int start = findBytes(buffer, new byte[]{(byte) 0xFF, (byte) 0xD8}); // JPEG文件头
                    int end = findBytes(buffer, new byte[]{(byte) 0xFF, (byte) 0xD9});   // JPEG文件尾
                    
                    if (start != -1 && end != -1 && end > start) {
                        // 提取完整的JPEG帧
                        byte[] jpegFrame = extractBytes(buffer, start, end + 2);
                        buffer = removeBytes(buffer, 0, end + 2);
                        
                        frameCount++;
                        
                        // 更新设备最后更新时间
                        device.setLastUpdate(System.currentTimeMillis());
                        
                        // 通过WebSocket发送帧数据
                        webSocketService.sendFrame(deviceId, jpegFrame);
                        
                        if (frameCount % 100 == 0) {
                            log.debug("设备 {} 已捕获 {} 帧", deviceId, frameCount);
                        }
                    } else {
                        break;
                    }
                }
                
                // 限制缓冲区大小，避免内存溢出
                if (buffer.length > 1024 * 1024) { // 1MB
                    log.warn("设备 {} 缓冲区过大，清空部分数据", deviceId);
                    buffer = new byte[0];
                }
                
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    log.error("设备 {} 读取数据失败: {}", deviceId, e.getMessage());
                    throw e;
                }
                break;
            }
        }
        
        log.info("设备 {} 屏幕捕获结束，共捕获 {} 帧", deviceId, frameCount);
    }
    
    /**
     * 停止捕获指定设备
     */
    public void stopCapture(int deviceId) {
        Socket socket = deviceConnections.remove(deviceId);
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
                log.info("停止设备 {} 的屏幕捕获", deviceId);
            } catch (IOException e) {
                log.error("关闭设备 {} 连接失败", deviceId, e);
            }
        }
    }
    
    /**
     * 停止所有捕获
     */
    public void stopAllCaptures() {
        log.info("停止所有设备的屏幕捕获");
        deviceConnections.forEach((deviceId, socket) -> {
            try {
                socket.close();
            } catch (IOException e) {
                log.error("关闭设备 {} 连接失败", deviceId, e);
            }
        });
        deviceConnections.clear();
    }
    
    /**
     * 获取活跃连接数
     */
    public int getActiveConnectionCount() {
        return deviceConnections.size();
    }
    
    // 工具方法
    
    /**
     * 连接字节数组
     */
    private byte[] appendBytes(byte[] original, byte[] newBytes, int length) {
        byte[] result = new byte[original.length + length];
        System.arraycopy(original, 0, result, 0, original.length);
        System.arraycopy(newBytes, 0, result, original.length, length);
        return result;
    }
    
    /**
     * 查找字节序列
     */
    private int findBytes(byte[] source, byte[] target) {
        for (int i = 0; i <= source.length - target.length; i++) {
            boolean found = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 提取字节数组片段
     */
    private byte[] extractBytes(byte[] source, int start, int end) {
        int length = end - start;
        byte[] result = new byte[length];
        System.arraycopy(source, start, result, 0, length);
        return result;
    }
    
    /**
     * 移除字节数组片段
     */
    private byte[] removeBytes(byte[] source, int start, int end) {
        int remaining = source.length - end;
        byte[] result = new byte[remaining];
        System.arraycopy(source, end, result, 0, remaining);
        return result;
    }
}