package com.integration.systema.repository;

import com.integration.systema.entity.CustomerRecordA;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface CustomerRecordARepository extends JpaRepository<CustomerRecordA, Long> {
    List<CustomerRecordA> findByUpdatedAtAfter(Instant updatedSince);
    List<CustomerRecordA> findByEmailAddressIgnoreCase(String emailAddress);
}
