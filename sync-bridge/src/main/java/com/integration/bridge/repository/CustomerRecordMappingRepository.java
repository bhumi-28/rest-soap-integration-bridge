package com.integration.bridge.repository;

import com.integration.bridge.entity.CustomerRecordMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerRecordMappingRepository extends JpaRepository<CustomerRecordMapping, Long> {
    Optional<CustomerRecordMapping> findBySystemAId(Long systemAId);
    Optional<CustomerRecordMapping> findBySystemBId(Long systemBId);
    Optional<CustomerRecordMapping> findByCanonicalEmailIgnoreCase(String email);
    List<CustomerRecordMapping> findBySyncStatus(CustomerRecordMapping.SyncStatus status);
}
