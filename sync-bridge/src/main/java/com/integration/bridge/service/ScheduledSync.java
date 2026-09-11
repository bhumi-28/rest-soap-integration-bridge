package com.integration.bridge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sync.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledSync {

    private static final Logger log = LoggerFactory.getLogger(ScheduledSync.class);

    private final SyncService syncService;

    public ScheduledSync(SyncService syncService) {
        this.syncService = syncService;
    }

    @Scheduled(fixedRateString = "${sync.schedule.interval-ms:600000}")
    public void run() {
        log.info("Starting scheduled sync job");
        syncService.executeSync();
    }
}