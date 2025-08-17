package com.ailin.mobile.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务
 */
@Slf4j
@Service
public class WebSocketService {
    
    /**
     * WebSocket会话映射
     */
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    /**
     * JSON转换器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 添加WebSocket会话
     */
    public void addSession(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);
        log.info("WebSocket会话已连接: {}", sessionId);
        
        // 发送欢迎消息
        sendMessage(sessionId, createMessage("welcome", "连接成功"));
    }
    
    /**
     * 移除WebSocket会话
     */
    public void removeSession(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("关闭WebSocket会话失败: {}", sessionId, e);
            }
        }
        log.info("WebSocket会话已断开: {}", sessionId);
    }
    
    /**
     * 发送屏幕帧数据
     */
    public void sendFrame(int deviceId, byte[] frameData) {
        if (frameData == null || frameData.length == 0) {
            return;
        }
        
        try {
            // 将图像数据转换为Base64
            String base64Image = Base64.getEncoder().encodeToString(frameData);
            
            // 创建消息
            Map<String, Object> message = new HashMap<>();
            message.put("type", "frame");
            message.put("deviceId", deviceId);
            message.put("image", "data:image/jpeg;base64," + base64Image);
            message.put("timestamp", System.currentTimeMillis());
            
            // 广播给所有连接的客户端
            broadcastMessage(message);
            
        } catch (Exception e) {
            log.error("发送屏幕帧失败: deviceId={}", deviceId, e);
        }
    }
    
    /**
     * 发送设备状态更新
     */
    public void sendDeviceStatus(int deviceId, String status, boolean connected) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "device_status");
        message.put("deviceId", deviceId);
        message.put("status", status);
        message.put("connected", connected);
        message.put("timestamp", System.currentTimeMillis());
        
        broadcastMessage(message);
    }
    
    /**
     * 发送设备列表
     */
    public void sendDeviceList(Object deviceList) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "device_list");
        message.put("devices", deviceList);
        message.put("timestamp", System.currentTimeMillis());
        
        broadcastMessage(message);
    }
    
    /**
     * 向指定会话发送消息
     */
    public void sendMessage(String sessionId, Object message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (Exception e) {
                log.error("发送消息失败: sessionId={}", sessionId, e);
                // 移除无效会话
                removeSession(sessionId);
            }
        }
    }
    
    /**
     * 广播消息给所有客户端
     */
    public void broadcastMessage(Object message) {
        if (sessions.isEmpty()) {
            return;
        }
        
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);
            
            // 使用迭代器避免并发修改异常
            sessions.entrySet().removeIf(entry -> {
                String sessionId = entry.getKey();
                WebSocketSession session = entry.getValue();
                
                if (session == null || !session.isOpen()) {
                    log.debug("移除无效会话: {}", sessionId);
                    return true;
                }
                
                try {
                    session.sendMessage(textMessage);
                    return false;
                } catch (Exception e) {
                    log.error("广播消息失败: sessionId={}", sessionId, e);
                    try {
                        session.close();
                    } catch (IOException closeEx) {
                        log.error("关闭无效会话失败: {}", sessionId, closeEx);
                    }
                    return true;
                }
            });
            
        } catch (Exception e) {
            log.error("广播消息失败", e);
        }
    }
    
    /**
     * 获取活跃会话数
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * 处理客户端消息
     */
    public void handleMessage(String sessionId, String message) {
        try {
            log.debug("收到客户端消息: sessionId={}, message={}", sessionId, message);
            
            // 解析消息
            Map<String, Object> messageMap = objectMapper.readValue(message, Map.class);
            String type = (String) messageMap.get("type");
            
            switch (type) {
                case "ping":
                    // 心跳检测
                    sendMessage(sessionId, createMessage("pong", "心跳响应"));
                    break;
                    
                case "request_device_list":
                    // 请求设备列表 - 这里可以调用NetworkScanService
                    log.info("客户端请求设备列表: {}", sessionId);
                    break;
                    
                default:
                    log.warn("未知消息类型: {}", type);
                    break;
            }
            
        } catch (Exception e) {
            log.error("处理客户端消息失败: sessionId={}", sessionId, e);
        }
    }
    
    /**
     * 创建消息对象
     */
    private Map<String, Object> createMessage(String type, Object data) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("data", data);
        message.put("timestamp", System.currentTimeMillis());
        return message;
    }
}