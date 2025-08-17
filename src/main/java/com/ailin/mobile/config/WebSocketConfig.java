package com.ailin.mobile.config;

import com.ailin.mobile.handler.ScreenWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private ScreenWebSocketHandler screenWebSocketHandler;
    
    @Autowired
    private MobileConfig mobileConfig;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册屏幕共享WebSocket处理器
        registry.addHandler(screenWebSocketHandler, mobileConfig.getWebsocket().getEndpoint())
                .setAllowedOrigins(mobileConfig.getWebsocket().getAllowedOrigins());
    }
}