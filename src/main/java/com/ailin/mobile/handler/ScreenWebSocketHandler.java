package com.ailin.mobile.handler;

import com.ailin.mobile.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

/**
 * 屏幕共享WebSocket处理器
 */
@Slf4j
@Component
public class ScreenWebSocketHandler implements WebSocketHandler {
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("WebSocket连接建立: sessionId={}, remoteAddress={}", 
                sessionId, session.getRemoteAddress());
        
        // 添加会话到服务中
        webSocketService.addSession(sessionId, session);
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            String payload = textMessage.getPayload();
            
            log.debug("收到文本消息: sessionId={}, payload={}", sessionId, payload);
            
            // 处理客户端消息
            webSocketService.handleMessage(sessionId, payload);
            
        } else if (message instanceof BinaryMessage) {
            BinaryMessage binaryMessage = (BinaryMessage) message;
            log.debug("收到二进制消息: sessionId={}, size={}", sessionId, binaryMessage.getPayloadLength());
            
            // 目前不处理客户端发送的二进制消息
            
        } else if (message instanceof PongMessage) {
            log.debug("收到Pong消息: sessionId={}", sessionId);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("WebSocket传输错误: sessionId={}", sessionId, exception);
        
        // 移除会话
        webSocketService.removeSession(sessionId);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        log.info("WebSocket连接关闭: sessionId={}, closeStatus={}", sessionId, closeStatus);
        
        // 移除会话
        webSocketService.removeSession(sessionId);
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}