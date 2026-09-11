package com.integration.bridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SyncBridgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SyncBridgeApplication.class, args);
    }
}
