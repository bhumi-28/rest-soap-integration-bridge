package com.integration.systemb.endpoint;

import com.integration.systemb.entity.CustomerRecordB;
import com.integration.systemb.service.CustomerRecordBService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.xml.xsd.XsdSchema;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Endpoint
public class CustomerEndpoint {

    private static final String NAMESPACE_URI = "http://integration.com/systemb/customer";

    private final CustomerRecordBService service;
    private final XsdSchema customerSchema;

    public CustomerEndpoint(CustomerRecordBService service, XsdSchema customerSchema) {
        this.service = service;
        this.customerSchema = customerSchema;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetAllCustomersRequest")
    @ResponsePayload
    public GetAllCustomersResponse getAllCustomers(@RequestPayload GetAllCustomersRequest request) {
        Instant since = request.getLastModifiedSince() != null
                ? Instant.parse(request.getLastModifiedSince())
                : null;
        List<CustomerRecordB> customers = service.findAll(since);

        GetAllCustomersResponse response = new GetAllCustomersResponse();
        List<CustomerRecordBType> customerTypes = customers.stream()
                .map(this::toType)
                .collect(Collectors.toList());
        response.setCustomers(customerTypes);
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetCustomerByNumberRequest")
    @ResponsePayload
    public GetCustomerByNumberResponse getCustomerByNumber(@RequestPayload GetCustomerByNumberRequest request) {
        CustomerRecordB customer = service.findByCustomerNumber(request.getCustomerNumber())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.getCustomerNumber()));

        GetCustomerByNumberResponse response = new GetCustomerByNumberResponse();
        response.setCustomer(toType(customer));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CreateCustomerRequest")
    @ResponsePayload
    public CreateCustomerResponse createCustomer(@RequestPayload CreateCustomerRequest request) {
        CustomerRecordB record = new CustomerRecordB(
                request.getName(),
                request.getContactEmail(),
                request.getPhone()
        );
        CustomerRecordB created = service.create(record);

        CreateCustomerResponse response = new CreateCustomerResponse();
        response.setCustomer(toType(created));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "UpdateCustomerRequest")
    @ResponsePayload
    public UpdateCustomerResponse updateCustomer(@RequestPayload UpdateCustomerRequest request) {
        CustomerRecordB updated = new CustomerRecordB(
                request.getName(),
                request.getContactEmail(),
                request.getPhone()
        );
        CustomerRecordB result = service.update(request.getCustomerNumber(), updated)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.getCustomerNumber()));

        UpdateCustomerResponse response = new UpdateCustomerResponse();
        response.setCustomer(toType(result));
        return response;
    }

    private CustomerRecordBType toType(CustomerRecordB entity) {
        CustomerRecordBType type = new CustomerRecordBType();
        type.setCustomerNumber(entity.getCustomerNumber());
        type.setName(entity.getName());
        type.setContactEmail(entity.getContactEmail());
        type.setPhone(entity.getPhone());
        type.setLastModified(entity.getLastModified() != null ? entity.getLastModified().toString() : null);
        return type;
    }
}
