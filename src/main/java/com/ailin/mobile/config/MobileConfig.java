package com.ailin.mobile.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 手机中控配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "mobile")
public class MobileConfig {
    
    /**
     * 网络配置
     */
    private Network network = new Network();
    
    /**
     * WebSocket配置
     */
    private Websocket websocket = new Websocket();
    
    @Data
    public static class Network {
        /**
         * 基础IP（如：192.168.31）
         */
        private String baseIp = "192.168.31";
        
        /**
         * 起始范围
         */
        private int startRange = 0;
        
        /**
         * 结束范围
         */
        private int endRange = 255;
        
        /**
         * HTTP检测端口
         */
        private int detectPort = 9801;
        
        /**
         * 投屏数据端口
         */
        private int streamPort = 9802;
        
        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 2000;
        
        /**
         * HTTP请求超时时间（毫秒）
         */
        private int httpTimeout = 1000;
        
        /**
         * 最大窗口数
         */
        private int maxWindows = 20;
        
        /**
         * 分批扫描大小
         */
        private int batchSize = 10;
    }
    
    @Data
    public static class Websocket {
        /**
         * WebSocket端点
         */
        private String endpoint = "/ws/screen";
        
        /**
         * 允许的跨域来源
         */
        private String allowedOrigins = "*";
    }
}