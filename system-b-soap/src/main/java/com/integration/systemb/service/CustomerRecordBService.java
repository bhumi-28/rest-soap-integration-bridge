package com.integration.systemb.service;

import com.integration.systemb.entity.CustomerRecordB;
import com.integration.systemb.repository.CustomerRecordBRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerRecordBService {

    private final CustomerRecordBRepository repository;

    public CustomerRecordBService(CustomerRecordBRepository repository) {
        this.repository = repository;
    }

    public List<CustomerRecordB> findAll(Instant lastModifiedSince) {
        if (lastModifiedSince != null) {
            return repository.findByLastModifiedAfter(lastModifiedSince);
        }
        return repository.findAll();
    }

    public Optional<CustomerRecordB> findByCustomerNumber(Long customerNumber) {
        return repository.findById(customerNumber);
    }

    public Optional<CustomerRecordB> findByEmail(String email) {
        return repository.findByContactEmailIgnoreCase(email).stream().findFirst();
    }

    public CustomerRecordB create(CustomerRecordB record) {
        record.setCustomerNumber(null);
        return repository.save(record);
    }

    public Optional<CustomerRecordB> update(Long customerNumber, CustomerRecordB updated) {
        return repository.findById(customerNumber).map(existing -> {
            existing.setName(updated.getName());
            existing.setContactEmail(updated.getContactEmail());
            existing.setPhone(updated.getPhone());
            return repository.save(existing);
        });
    }
}
