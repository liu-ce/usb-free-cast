package com.ailin.mobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 手机中控系统启动类
 * 
 * @author ailin
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MobileControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(MobileControlApplication.class, args);
    }
}