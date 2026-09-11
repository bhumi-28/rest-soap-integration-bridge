package com.integration.systemb.repository;

import com.integration.systemb.entity.CustomerRecordB;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface CustomerRecordBRepository extends JpaRepository<CustomerRecordB, Long> {
    List<CustomerRecordB> findByLastModifiedAfter(Instant lastModifiedSince);
    List<CustomerRecordB> findByContactEmailIgnoreCase(String contactEmail);
}
