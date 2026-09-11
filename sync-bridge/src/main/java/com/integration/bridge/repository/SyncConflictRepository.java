package com.integration.bridge.repository;

import com.integration.bridge.entity.SyncConflict;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SyncConflictRepository extends JpaRepository<SyncConflict, Long> {
    List<SyncConflict> findByResolution(SyncConflict.Resolution resolution);
    List<SyncConflict> findByMappingIdAndResolution(Long mappingId, SyncConflict.Resolution resolution);
}
