package com.example.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupCompleteIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupCompleteIndicator.class);
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("=========================================");
        logger.info("系统启动完成!");
        logger.info("=========================================");
    }
}