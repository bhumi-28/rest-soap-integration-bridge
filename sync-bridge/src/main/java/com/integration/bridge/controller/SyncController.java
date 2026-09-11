package com.integration.bridge.controller;

import com.integration.bridge.entity.CustomerRecordMapping;
import com.integration.bridge.entity.SyncConflict;
import com.integration.bridge.entity.SyncJob;
import com.integration.bridge.repository.CustomerRecordMappingRepository;
import com.integration.bridge.repository.SyncConflictRepository;
import com.integration.bridge.repository.SyncJobRepository;
import com.integration.bridge.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;
    private final SyncJobRepository jobRepository;
    private final SyncConflictRepository conflictRepository;
    private final CustomerRecordMappingRepository mappingRepository;

    public SyncController(SyncService syncService, SyncJobRepository jobRepository,
                          SyncConflictRepository conflictRepository,
                          CustomerRecordMappingRepository mappingRepository) {
        this.syncService = syncService;
        this.jobRepository = jobRepository;
        this.conflictRepository = conflictRepository;
        this.mappingRepository = mappingRepository;
    }

    @PostMapping("/trigger")
    public ResponseEntity<SyncJob> triggerSync() {
        SyncJob job = syncService.executeSync();
        return ResponseEntity.ok(job);
    }

    @GetMapping("/jobs")
    public List<SyncJob> listJobs() {
        return jobRepository.findAllByOrderByStartedAtDesc();
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<SyncJob> getJob(@PathVariable Long id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/conflicts")
    public List<SyncConflict> listConflicts(
            @RequestParam(required = false, defaultValue = "UNRESOLVED") String status) {
        SyncConflict.Resolution resolution = SyncConflict.Resolution.valueOf(status);
        return conflictRepository.findByResolution(resolution);
    }

    @PostMapping("/conflicts/{id}/resolve")
    public ResponseEntity<SyncConflict> resolveConflict(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            SyncConflict resolved = syncService.resolveConflict(
                    id,
                    body.get("resolution"),
                    body.get("manualValue"));
            return ResponseEntity.ok(resolved);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/mappings")
    public List<CustomerRecordMapping> listMappings() {
        return mappingRepository.findAll();
    }
}
