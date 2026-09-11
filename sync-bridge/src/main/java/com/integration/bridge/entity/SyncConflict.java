package com.integration.bridge.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sync_conflict")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SyncConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sync_job_id", nullable = false)
    private SyncJob syncJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapping_id", nullable = false)
    private CustomerRecordMapping mapping;

    @Column(nullable = false, length = 50)
    private String fieldName;

    @Column(length = 255)
    private String valueFromA;

    @Column(length = 255)
    private String valueFromB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Resolution resolution = Resolution.UNRESOLVED;

    @Column(length = 100)
    private String resolvedBy;

    private Instant resolvedAt;

    public SyncConflict() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SyncJob getSyncJob() { return syncJob; }
    public void setSyncJob(SyncJob syncJob) { this.syncJob = syncJob; }

    public CustomerRecordMapping getMapping() { return mapping; }
    public void setMapping(CustomerRecordMapping mapping) { this.mapping = mapping; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getValueFromA() { return valueFromA; }
    public void setValueFromA(String valueFromA) { this.valueFromA = valueFromA; }

    public String getValueFromB() { return valueFromB; }
    public void setValueFromB(String valueFromB) { this.valueFromB = valueFromB; }

    public Resolution getResolution() { return resolution; }
    public void setResolution(Resolution resolution) { this.resolution = resolution; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public enum Resolution {
        UNRESOLVED, A_WINS, B_WINS, MANUAL
    }
}
