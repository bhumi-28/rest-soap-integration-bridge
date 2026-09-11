package com.integration.systemb.config;

import com.integration.systemb.entity.CustomerRecordB;
import com.integration.systemb.repository.CustomerRecordBRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CustomerRecordBRepository repository;

    public DataSeeder(CustomerRecordBRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.saveAll(List.of(
                new CustomerRecordB("Alice Johnson", "alice.johnson@example.com", "+1-555-0101"),
                new CustomerRecordB("Bob Smith", "bob@example.com", "+1-555-1111"),
                new CustomerRecordB("Frank Miller", "frank@example.com", "+1-555-0201"),
                new CustomerRecordB("Grace Lee", "grace@example.com", "+1-555-0202"),
                new CustomerRecordB("Henry Wilson", "henry@example.com", "+1-555-0203")
            ));
            System.out.println(">>> System B: Seeded 5 customer records");
        }
    }
}
