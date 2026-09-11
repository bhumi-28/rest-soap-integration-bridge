package com.integration.bridge.client;

import com.integration.bridge.dto.CustomerRecordBDto;
import com.integration.bridge.soap.CreateCustomerRequest;
import com.integration.bridge.soap.CreateCustomerResponse;
import com.integration.bridge.soap.CustomerRecordBType;
import com.integration.bridge.soap.GetAllCustomersRequest;
import com.integration.bridge.soap.GetAllCustomersResponse;
import com.integration.bridge.soap.UpdateCustomerRequest;
import com.integration.bridge.soap.UpdateCustomerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Component
public class SystemBSoapClient {

    private static final Logger log = LoggerFactory.getLogger(SystemBSoapClient.class);

    private final WebServiceTemplate webServiceTemplate;

    public SystemBSoapClient(WebServiceTemplate webServiceTemplate,
                             @Value("${system-b.base-url}") String baseUrl) {
        this.webServiceTemplate = webServiceTemplate;
        if (baseUrl != null && !baseUrl.isBlank()) {
            this.webServiceTemplate.setDefaultUri(baseUrl + "/ws");
        }
    }

    public List<CustomerRecordBDto> getAllCustomers() {
        return retry(() -> {
            GetAllCustomersResponse response = (GetAllCustomersResponse)
                    webServiceTemplate.marshalSendAndReceive(new GetAllCustomersRequest());
            List<CustomerRecordBType> customers = response != null ? response.getCustomers() : List.of();
            List<CustomerRecordBDto> dtos = new ArrayList<>();
            if (customers != null) {
                for (CustomerRecordBType customer : customers) {
                    dtos.add(toDto(customer));
                }
            }
            return dtos;
        }, "fetch customers from System B");
    }

    public CustomerRecordBDto createCustomer(String name, String email, String phone) {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setName(name);
        request.setContactEmail(email);
        request.setPhone(phone);

        return retry(() -> {
            CreateCustomerResponse response = (CreateCustomerResponse)
                    webServiceTemplate.marshalSendAndReceive(request);
            return response != null ? toDto(response.getCustomer()) : null;
        }, "create customer in System B");
    }

    public CustomerRecordBDto updateCustomer(Long customerNumber, String name, String email, String phone) {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setCustomerNumber(customerNumber);
        request.setName(name);
        request.setContactEmail(email);
        request.setPhone(phone);

        return retry(() -> {
            UpdateCustomerResponse response = (UpdateCustomerResponse)
                    webServiceTemplate.marshalSendAndReceive(request);
            return response != null ? toDto(response.getCustomer()) : null;
        }, "update customer " + customerNumber + " in System B");
    }

    private CustomerRecordBDto toDto(CustomerRecordBType type) {
        if (type == null) return null;
        CustomerRecordBDto dto = new CustomerRecordBDto();
        dto.setCustomerNumber(type.getCustomerNumber());
        dto.setName(type.getName());
        dto.setContactEmail(type.getContactEmail());
        dto.setPhone(type.getPhone());
        if (type.getLastModified() != null) {
            dto.setLastModified(Instant.parse(type.getLastModified()));
        }
        return dto;
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