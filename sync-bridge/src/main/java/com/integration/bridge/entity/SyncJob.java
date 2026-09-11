package com.integration.bridge.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sync_job")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SyncJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.RUNNING;

    private int recordsProcessed;
    private int recordsConflicted;
    private int recordsFailed;

    public SyncJob() {
        this.startedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public int getRecordsProcessed() { return recordsProcessed; }
    public void setRecordsProcessed(int recordsProcessed) { this.recordsProcessed = recordsProcessed; }

    public int getRecordsConflicted() { return recordsConflicted; }
    public void setRecordsConflicted(int recordsConflicted) { this.recordsConflicted = recordsConflicted; }

    public int getRecordsFailed() { return recordsFailed; }
    public void setRecordsFailed(int recordsFailed) { this.recordsFailed = recordsFailed; }

    public enum JobStatus {
        RUNNING, SUCCESS, FAILED, PARTIAL
    }
}
