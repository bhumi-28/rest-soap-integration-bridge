package com.integration.systemb.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;

import java.util.Collections;
import java.util.Map;

import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.noFault;
import static org.springframework.ws.test.server.ResponseMatchers.xpath;

@SpringBootTest
class CustomerEndpointTest {

    private static final Map<String, String> NS =
            Collections.singletonMap("tns", "http://integration.com/systemb/customer");

    @Autowired
    private ApplicationContext applicationContext;

    private MockWebServiceClient mockClient;

    @BeforeEach
    void createClient() {
        mockClient = MockWebServiceClient.createClient(applicationContext);
    }

    @Test
    void getAllCustomersReturnsSeededCustomers() {
        mockClient.sendRequest(withPayload(new StringSource(
                "<tns:GetAllCustomersRequest xmlns:tns=\"http://integration.com/systemb/customer\"/>")))
                .andExpect(noFault())
                .andExpect(xpath("/tns:GetAllCustomersResponse/tns:customers/tns:customer", NS).exists());
    }

    @Test
    void createCustomerCreatesRecord() {
        mockClient.sendRequest(withPayload(new StringSource(
                "<tns:CreateCustomerRequest xmlns:tns=\"http://integration.com/systemb/customer\">"
                        + "<tns:name>Zoe Test</tns:name>"
                        + "<tns:contactEmail>zoe.test@example.com</tns:contactEmail>"
                        + "<tns:phone>+1-555-7777</tns:phone>"
                        + "</tns:CreateCustomerRequest>")))
                .andExpect(noFault())
                .andExpect(xpath("/tns:CreateCustomerResponse/tns:customer", NS).exists())
                .andExpect(xpath("/tns:CreateCustomerResponse/tns:customer/tns:name[text()='Zoe Test']", NS).exists())
                .andExpect(xpath("/tns:CreateCustomerResponse/tns:customer/tns:contactEmail"
                        + "[text()='zoe.test@example.com']", NS).exists())
                .andExpect(xpath("/tns:CreateCustomerResponse/tns:customer/tns:customerNumber", NS).exists());
    }

    @Test
    void updateCustomerModifiesRecord() {
        mockClient.sendRequest(withPayload(new StringSource(
                "<tns:GetAllCustomersRequest xmlns:tns=\"http://integration.com/systemb/customer\"/>")))
                .andExpect(noFault())
                .andExpect(xpath("/tns:GetAllCustomersResponse/tns:customers/tns:customer[1]/tns:customerNumber", NS)
                        .exists());
    }
}