package com.ailin.mobile.controller;

import com.ailin.mobile.model.DeviceInfo;
import com.ailin.mobile.service.NetworkScanService;
import com.ailin.mobile.service.ScreenCaptureService;
import com.ailin.mobile.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 手机中控REST API控制器
 */
@Slf4j
@RestController
@RequestMapping("/mobile")
@CrossOrigin(origins = "*")
public class MobileController {
    
    @Autowired
    private NetworkScanService networkScanService;
    
    @Autowired
    private ScreenCaptureService screenCaptureService;
    
    @Autowired
    private WebSocketService webSocketService;
    
    /**
     * 扫描局域网设备（全量扫描）
     */
    @PostMapping("/scan")
    public ResponseEntity<?> scanNetwork() {
        try {
            log.info("收到全量扫描网络请求");
            
            CompletableFuture<List<DeviceInfo>> future = networkScanService.scanNetwork();
            List<DeviceInfo> devices = future.get();
            
            // 通过WebSocket通知前端
            webSocketService.sendDeviceList(devices);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "扫描完成");
            response.put("devices", devices);
            response.put("count", devices.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("扫描网络失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "扫描失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 分批扫描局域网设备
     */
    @PostMapping("/scan/batch")
    public ResponseEntity<?> scanNetworkBatch(@RequestBody Map<String, Object> request) {
        try {
            String baseIp = (String) request.get("baseIp");
            Integer startRange = (Integer) request.get("startRange");
            Integer endRange = (Integer) request.get("endRange");
            
            if (baseIp == null || startRange == null || endRange == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "参数不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            log.info("收到分批扫描网络请求: {}.{}-{}", baseIp, startRange, endRange);
            
            CompletableFuture<List<DeviceInfo>> future = networkScanService.scanBatch(baseIp, startRange, endRange);
            List<DeviceInfo> devices = future.get();
            
            // 通过WebSocket通知前端
            webSocketService.sendDeviceList(devices);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "分批扫描完成");
            response.put("devices", devices);
            response.put("count", devices.size());
            response.put("range", baseIp + "." + startRange + "-" + endRange);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("分批扫描网络失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "分批扫描失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取设备列表
     */
    @GetMapping("/devices")
    public ResponseEntity<?> getDevices() {
        try {
            List<DeviceInfo> devices = networkScanService.getAllDevices();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("devices", devices);
            response.put("count", devices.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取设备列表失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取设备列表失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取指定设备信息
     */
    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<?> getDevice(@PathVariable int deviceId) {
        try {
            DeviceInfo device = networkScanService.getDevice(deviceId);
            
            Map<String, Object> response = new HashMap<>();
            if (device != null) {
                response.put("success", true);
                response.put("device", device);
            } else {
                response.put("success", false);
                response.put("message", "设备不存在");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取设备信息失败: deviceId={}", deviceId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取设备信息失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 移除设备
     */
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<?> removeDevice(@PathVariable int deviceId) {
        try {
            networkScanService.removeDevice(deviceId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "设备已移除");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("移除设备失败: deviceId={}", deviceId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "移除设备失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 更新网络配置
     */
    @PostMapping("/config/network")
    public ResponseEntity<?> updateNetworkConfig(@RequestBody Map<String, Object> config) {
        try {
            String baseIp = (String) config.get("baseIp");
            Integer startRange = (Integer) config.get("startRange");
            Integer endRange = (Integer) config.get("endRange");
            
            if (baseIp == null || startRange == null || endRange == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "参数不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            networkScanService.updateNetworkConfig(baseIp, startRange, endRange);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "网络配置已更新");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("更新网络配置失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新网络配置失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取系统状态
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        try {
            int deviceCount = networkScanService.getAllDevices().size();
            int activeConnections = screenCaptureService.getActiveConnectionCount();
            int activeSessions = webSocketService.getActiveSessionCount();
            
            Map<String, Object> status = new HashMap<>();
            status.put("deviceCount", deviceCount);
            status.put("activeConnections", activeConnections);
            status.put("activeSessions", activeSessions);
            status.put("timestamp", System.currentTimeMillis());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", status);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取系统状态失败: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}