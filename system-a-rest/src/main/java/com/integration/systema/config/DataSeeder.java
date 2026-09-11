package com.integration.systema.config;

import com.integration.systema.entity.CustomerRecordA;
import com.integration.systema.repository.CustomerRecordARepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CustomerRecordARepository repository;

    public DataSeeder(CustomerRecordARepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.saveAll(List.of(
                new CustomerRecordA("Alice Johnson", "alice@example.com", "+1-555-0101"),
                new CustomerRecordA("Bob Smith", "bob@example.com", "+1-555-0102"),
                new CustomerRecordA("Carol White", "carol@example.com", "+1-555-0103"),
                new CustomerRecordA("David Brown", "david@example.com", "+1-555-0104"),
                new CustomerRecordA("Eve Davis", "eve@example.com", "+1-555-0105")
            ));
            System.out.println(">>> System A: Seeded 5 customer records");
        }
    }
}
