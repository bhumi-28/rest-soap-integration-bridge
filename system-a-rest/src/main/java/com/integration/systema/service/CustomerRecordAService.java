package com.integration.systema.service;

import com.integration.systema.entity.CustomerRecordA;
import com.integration.systema.repository.CustomerRecordARepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerRecordAService {

    private final CustomerRecordARepository repository;

    public CustomerRecordAService(CustomerRecordARepository repository) {
        this.repository = repository;
    }

    public List<CustomerRecordA> findAll(Instant updatedSince) {
        if (updatedSince != null) {
            return repository.findByUpdatedAtAfter(updatedSince);
        }
        return repository.findAll();
    }

    public Optional<CustomerRecordA> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<CustomerRecordA> findByEmail(String email) {
        return repository.findByEmailAddressIgnoreCase(email).stream().findFirst();
    }

    public CustomerRecordA create(CustomerRecordA record) {
        record.setId(null);
        return repository.save(record);
    }

    public Optional<CustomerRecordA> update(Long id, CustomerRecordA updated) {
        return repository.findById(id).map(existing -> {
            existing.setFullName(updated.getFullName());
            existing.setEmailAddress(updated.getEmailAddress());
            existing.setPhoneNumber(updated.getPhoneNumber());
            return repository.save(existing);
        });
    }

    public List<CustomerRecordA> saveAll(List<CustomerRecordA> records) {
        return repository.saveAll(records);
    }
}
