package com.integration.bridge.client;

import com.integration.bridge.dto.CustomerRecordADto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@Component
public class SystemARestClient {

    private static final Logger log = LoggerFactory.getLogger(SystemARestClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SystemARestClient(RestTemplate restTemplate,
                             @Value("${system-a.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public List<CustomerRecordADto> getAllCustomers() {
        return retry(() -> {
            ResponseEntity<CustomerRecordADto[]> response = restTemplate.getForEntity(
                    baseUrl + "/api/customers", CustomerRecordADto[].class);
            return Arrays.asList(response.getBody() != null ? response.getBody() : new CustomerRecordADto[0]);
        }, "fetch customers from System A");
    }

    public List<CustomerRecordADto> getCustomersUpdatedSince(Instant since) {
        return retry(() -> {
            ResponseEntity<CustomerRecordADto[]> response = restTemplate.getForEntity(
                    baseUrl + "/api/customers?updatedSince={since}",
                    CustomerRecordADto[].class, since.toString());
            return Arrays.asList(response.getBody() != null ? response.getBody() : new CustomerRecordADto[0]);
        }, "fetch updated customers from System A");
    }

    public CustomerRecordADto getCustomerById(Long id) {
        return retry(() -> {
            ResponseEntity<CustomerRecordADto> response = restTemplate.getForEntity(
                    baseUrl + "/api/customers/{id}", CustomerRecordADto.class, id);
            return response.getBody();
        }, "fetch customer " + id + " from System A");
    }

    public CustomerRecordADto updateCustomer(Long id, CustomerRecordADto dto) {
        return retry(() -> {
            ResponseEntity<CustomerRecordADto> response = restTemplate.exchange(
                    baseUrl + "/api/customers/{id}",
                    HttpMethod.PUT,
                    new HttpEntity<>(dto),
                    CustomerRecordADto.class, id);
            return response.getBody();
        }, "update customer " + id + " in System A");
    }

    public CustomerRecordADto createCustomer(CustomerRecordADto dto) {
        return retry(() -> {
            ResponseEntity<CustomerRecordADto> response = restTemplate.postForEntity(
                    baseUrl + "/api/customers", dto, CustomerRecordADto.class);
            return response.getBody();
        }, "create customer in System A");
    }

    private <T> T retry(Supplier<T> action, String description) {
        try {
            return action.get();
        } catch (Exception first) {
            log.warn("Attempt 1/2 failed to {} : {}", description, first.getMessage());
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            try {
                return action.get();
            } catch (Exception second) {
                log.error("Attempt 2/2 failed to {} : {}", description, second.getMessage());
                throw second;
            }
        }
    }
}