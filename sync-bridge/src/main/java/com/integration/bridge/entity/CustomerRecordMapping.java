package com.integration.bridge.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customer_record_mapping")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CustomerRecordMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long systemAId;

    private Long systemBId;

    @Column(nullable = false, length = 150)
    private String canonicalName;

    @Column(nullable = false, length = 150)
    private String canonicalEmail;

    @Column(nullable = false, length = 30)
    private String canonicalPhone;

    private Instant lastSyncedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus syncStatus = SyncStatus.IN_SYNC;

    public CustomerRecordMapping() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSystemAId() { return systemAId; }
    public void setSystemAId(Long systemAId) { this.systemAId = systemAId; }

    public Long getSystemBId() { return systemBId; }
    public void setSystemBId(Long systemBId) { this.systemBId = systemBId; }

    public String getCanonicalName() { return canonicalName; }
    public void setCanonicalName(String canonicalName) { this.canonicalName = canonicalName; }

    public String getCanonicalEmail() { return canonicalEmail; }
    public void setCanonicalEmail(String canonicalEmail) { this.canonicalEmail = canonicalEmail; }

    public String getCanonicalPhone() { return canonicalPhone; }
    public void setCanonicalPhone(String canonicalPhone) { this.canonicalPhone = canonicalPhone; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public SyncStatus getSyncStatus() { return syncStatus; }
    public void setSyncStatus(SyncStatus syncStatus) { this.syncStatus = syncStatus; }

    public enum SyncStatus {
        IN_SYNC, CONFLICT, ERROR
    }
}
