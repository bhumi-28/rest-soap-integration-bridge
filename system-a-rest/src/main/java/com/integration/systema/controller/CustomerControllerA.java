package com.integration.systema.controller;

import com.integration.systema.entity.CustomerRecordA;
import com.integration.systema.service.CustomerRecordAService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerControllerA {

    private final CustomerRecordAService service;

    public CustomerControllerA(CustomerRecordAService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerRecordA> list(@RequestParam(required = false) String updatedSince) {
        Instant since = updatedSince != null ? Instant.parse(updatedSince) : null;
        return service.findAll(since);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerRecordA> get(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CustomerRecordA> create(@RequestBody CustomerRecordA record) {
        CustomerRecordA created = service.create(record);
        return ResponseEntity.created(URI.create("/api/customers/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerRecordA> update(@PathVariable Long id, @RequestBody CustomerRecordA record) {
        return service.update(id, record)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
