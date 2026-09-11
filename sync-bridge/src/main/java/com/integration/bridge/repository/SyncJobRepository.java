package com.integration.bridge.repository;

import com.integration.bridge.entity.SyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SyncJobRepository extends JpaRepository<SyncJob, Long> {
    List<SyncJob> findAllByOrderByStartedAtDesc();
}
