package com.ailin.mobile.service;

import com.ailin.mobile.config.MobileConfig;
import com.ailin.mobile.model.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 网络扫描服务
 */
@Slf4j
@Service
public class NetworkScanService {
    
    @Autowired
    private MobileConfig mobileConfig;
    
    @Autowired
    private ScreenCaptureService screenCaptureService;
    
    /**
     * HTTP客户端
     */
    private final RestTemplate restTemplate;
    
    /**
     * 设备列表（按ID排序）
     */
    private final ConcurrentHashMap<Integer, DeviceInfo> deviceMap = new ConcurrentHashMap<>();
    
    /**
     * 下一个可用的设备ID
     */
    private final AtomicInteger nextDeviceId = new AtomicInteger(1);
    
    /**
     * 构造函数
     */
    public NetworkScanService() {
        // 创建RestTemplate并设置超时
        this.restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1000);  // 连接超时1秒
        factory.setReadTimeout(2000);     // 读取超时2秒
        this.restTemplate.setRequestFactory(factory);
    }
    
    /**
     * 扫描局域网设备（全量扫描）
     */
    @Async
    public CompletableFuture<List<DeviceInfo>> scanNetwork() {
        log.info("开始全量扫描局域网设备...");
        
        String baseIp = mobileConfig.getNetwork().getBaseIp();
        int startRange = mobileConfig.getNetwork().getStartRange();
        int endRange = mobileConfig.getNetwork().getEndRange();
        int batchSize = mobileConfig.getNetwork().getBatchSize();
        
        List<DeviceInfo> allDevices = new ArrayList<>();
        
        // 分批扫描
        for (int start = startRange; start <= endRange; start += batchSize) {
            int end = Math.min(start + batchSize - 1, endRange);
            
            log.debug("扫描批次: {}.{} ~ {}.{}", baseIp, start, baseIp, end);
            
            List<DeviceInfo> batchDevices = scanBatch(baseIp, start, end).join();
            allDevices.addAll(batchDevices);
            
            // 批次间稍作延时，避免网络压力过大
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        log.info("全量扫描完成，发现 {} 个在线设备", allDevices.size());
        return CompletableFuture.completedFuture(allDevices);
    }
    
    /**
     * 分批扫描指定IP范围
     */
    @Async
    public CompletableFuture<List<DeviceInfo>> scanBatch(String baseIp, int startRange, int endRange) {
        log.debug("开始分批扫描: {}.{} ~ {}.{}", baseIp, startRange, baseIp, endRange);
        
        List<CompletableFuture<DeviceInfo>> futures = new ArrayList<>();
        
        // 扫描IP范围，使用HTTP检测
        for (int i = startRange; i <= endRange; i++) {
            String ip = baseIp + "." + i;
            futures.add(checkDeviceByHttp(ip));
        }
        
        // 等待所有扫描完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();
        
        // 收集结果
        List<DeviceInfo> activeDevices = new ArrayList<>();
        for (CompletableFuture<DeviceInfo> future : futures) {
            try {
                DeviceInfo device = future.get();
                if (device != null && device.isConnected()) {
                    activeDevices.add(device);
                }
            } catch (Exception e) {
                log.debug("获取扫描结果失败: {}", e.getMessage());
            }
        }
        
        log.debug("分批扫描完成，发现 {} 个在线设备", activeDevices.size());
        return CompletableFuture.completedFuture(activeDevices);
    }
    
    /**
     * 通过HTTP检查设备（快速检测）
     */
    @Async
    public CompletableFuture<DeviceInfo> checkDeviceByHttp(String ip) {
        int detectPort = mobileConfig.getNetwork().getDetectPort();
        int streamPort = mobileConfig.getNetwork().getStreamPort();
        
        try {
            // 构建检测URL
            String url = "http://" + ip + ":" + detectPort + "/getscreensize";
            
            log.debug("HTTP检测设备: {}", url);
            
            // 发送HTTP GET请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                String screenSize = response.getBody() != null ? response.getBody().trim() : "Unknown";
                log.info("设备 {} 在线，屏幕尺寸: {}", ip, screenSize);
                
                // 设备响应正常，分配设备ID
                int deviceId = getOrCreateDeviceId(ip, streamPort);
                DeviceInfo device = new DeviceInfo(deviceId, ip, streamPort);
                device.setConnected(true);
                device.setStatus("online");
                device.setLastUpdate(System.currentTimeMillis());
                device.setDeviceName("设备 " + screenSize);
                // 解析如 "1280 720" 或 "1280x720"
                try {
                    String normalized = screenSize.replace("x", " ").replace("X", " ").trim();
                    String[] parts = normalized.split("\\s+");
                    if (parts.length >= 2) {
                        int w = Integer.parseInt(parts[0]);
                        int h = Integer.parseInt(parts[1]);
                        device.setScreenWidth(w);
                        device.setScreenHeight(h);
                    }
                } catch (Exception ignore) {}
                
                // 更新设备映射
                deviceMap.put(deviceId, device);
                
                log.info("添加在线设备: {} (ID: {}, 屏幕: {})", ip, deviceId, screenSize);
                
                // 启动屏幕捕获
                screenCaptureService.startCapture(device);
                
                return CompletableFuture.completedFuture(device);
            }
            
        } catch (ResourceAccessException e) {
            log.debug("设备 {} HTTP请求超时: {}", ip, e.getMessage());
        } catch (Exception e) {
            log.debug("设备 {} HTTP检测失败: {}", ip, e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 通过Socket检查设备（兼容旧方式）
     */
    @Async
    public CompletableFuture<DeviceInfo> checkDeviceBySocket(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(ip, port), 
                          mobileConfig.getNetwork().getConnectTimeout());
            
            // 设备可连接，分配设备ID
            int deviceId = getOrCreateDeviceId(ip, port);
            DeviceInfo device = new DeviceInfo(deviceId, ip, port);
            device.setConnected(true);
            device.setStatus("online");
            device.setLastUpdate(System.currentTimeMillis());
            
            // 更新设备映射
            deviceMap.put(deviceId, device);
            
            log.debug("发现在线设备: {}:{} (ID: {})", ip, port, deviceId);
            
            // 启动屏幕捕获
            screenCaptureService.startCapture(device);
            
            return CompletableFuture.completedFuture(device);
            
        } catch (SocketTimeoutException e) {
            log.debug("设备连接超时: {}:{}", ip, port);
        } catch (IOException e) {
            log.debug("设备连接失败: {}:{} - {}", ip, port, e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 获取或创建设备ID
     */
    private synchronized int getOrCreateDeviceId(String ip, int port) {
        // 检查是否已存在相同IP的设备
        for (DeviceInfo device : deviceMap.values()) {
            if (device.getIp().equals(ip) && device.getPort() == port) {
                return device.getId();
            }
        }
        
        // 创建新的设备ID
        // 分配最小可用的正整数ID，在合理范围内查找
        for (int i = 1; i <= 10000; i++) {  // 限制在10000以内查找
            if (!deviceMap.containsKey(i)) {
                return i;
            }
        }
        
        // 如果在合理范围内没有可用ID，返回下一个ID
        return nextDeviceId.getAndIncrement();
    }
    
    /**
     * 获取所有设备列表
     */
    public List<DeviceInfo> getAllDevices() {
        return new ArrayList<>(deviceMap.values());
    }
    
    /**
     * 获取指定设备
     */
    public DeviceInfo getDevice(int deviceId) {
        return deviceMap.get(deviceId);
    }
    
    /**
     * 移除设备
     */
    public void removeDevice(int deviceId) {
        DeviceInfo device = deviceMap.remove(deviceId);
        if (device != null) {
            log.info("移除设备: {}:{} (ID: {})", device.getIp(), device.getPort(), deviceId);
            // 停止屏幕捕获
            screenCaptureService.stopCapture(deviceId);
        }
    }
    
    /**
     * 更新网络配置
     */
    public void updateNetworkConfig(String baseIp, int startRange, int endRange) {
        mobileConfig.getNetwork().setBaseIp(baseIp);
        mobileConfig.getNetwork().setStartRange(startRange);
        mobileConfig.getNetwork().setEndRange(endRange);
        
        log.info("更新网络配置: {}.{}-{}", baseIp, startRange, endRange);
    }
}