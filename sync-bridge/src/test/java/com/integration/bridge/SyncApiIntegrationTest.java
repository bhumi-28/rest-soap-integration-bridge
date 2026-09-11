package com.integration.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.integration.bridge.entity.CustomerRecordMapping;
import com.integration.bridge.repository.CustomerRecordMappingRepository;
import com.integration.bridge.repository.SyncConflictRepository;
import com.integration.bridge.repository.SyncJobRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SyncApiIntegrationTest {

    private static WireMockServer wireMock;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private SyncJobRepository jobRepository;

    @Autowired
    private SyncConflictRepository conflictRepository;

    @Autowired
    private CustomerRecordMappingRepository mappingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startServer() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopServer() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("system-a.base-url", () -> "http://localhost:" + wireMock.port());
        registry.add("system-b.base-url", () -> "http://localhost:" + wireMock.port());
        registry.add("sync.scheduling.enabled", () -> "false");
    }

    @BeforeEach
    void cleanUp() {
        wireMock.resetAll();
        conflictRepository.deleteAll();
        mappingRepository.deleteAll();
        jobRepository.deleteAll();
    }

    private void stubConflictingAlice() {
        wireMock.stubFor(get(urlEqualTo("/api/customers"))
                .willReturn(okJson("""
                        [
                          {
                            "id": 1,
                            "fullName": "Alice Johnson",
                            "emailAddress": "alice@example.com",
                            "phoneNumber": "+1-555-0101",
                            "updatedAt": "2026-09-05T10:00:00Z"
                          }
                        ]
                        """)));
        wireMock.stubFor(post(urlEqualTo("/ws"))
                .withRequestBody(containing("GetAllCustomersRequest"))
                .willReturn(ok("""
                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                                       xmlns:cust="http://integration.com/systemb/customer">
                          <soap:Body>
                            <cust:GetAllCustomersResponse>
                              <cust:customers>
                                <cust:customer>
                                  <cust:customerNumber>10</cust:customerNumber>
                                  <cust:name>Alice Johnson Changed</cust:name>
                                  <cust:contactEmail>alice@example.com</cust:contactEmail>
                                  <cust:phone>+1-555-0101</cust:phone>
                                  <cust:lastModified>2026-09-06T10:00:00Z</cust:lastModified>
                                </cust:customer>
                              </cust:customers>
                            </cust:GetAllCustomersResponse>
                          </soap:Body>
                        </soap:Envelope>
                        """).withHeader("Content-Type", "text/xml")));
    }

    private void seedInSyncMapping() {
        CustomerRecordMapping mapping = new CustomerRecordMapping();
        mapping.setSystemAId(1L);
        mapping.setSystemBId(10L);
        mapping.setCanonicalName("Alice Johnson");
        mapping.setCanonicalEmail("alice@example.com");
        mapping.setCanonicalPhone("+1-555-0101");
        mapping.setLastSyncedAt(Instant.parse("2026-09-01T10:00:00Z"));
        mapping.setSyncStatus(CustomerRecordMapping.SyncStatus.IN_SYNC);
        mappingRepository.save(mapping);
    }

    @Test
    void conflictsEndpointsSerializeJsonWithoutLazyProxyErrors() {
        stubConflictingAlice();
        seedInSyncMapping();

        ResponseEntity<String> trigger = rest.postForEntity("/api/sync/trigger", null, String.class);
        assertThat(trigger.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> conflicts = rest.getForEntity(
                "/api/sync/conflicts?status=UNRESOLVED", String.class);
        assertThat(conflicts.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(conflicts.getBody())
                .contains("\"fieldName\":\"name\"")
                .contains("\"valueFromA\":\"Alice Johnson\"")
                .contains("\"valueFromB\":\"Alice Johnson Changed\"")
                .contains("\"resolution\":\"UNRESOLVED\"");
    }

    @Test
    void resolvingConflictOverHttpWritesBackAndReturnsSerializableConflict() throws Exception {
        stubConflictingAlice();
        seedInSyncMapping();

        rest.postForEntity("/api/sync/trigger", null, String.class);

        ResponseEntity<String> unresolved = rest.getForEntity(
                "/api/sync/conflicts?status=UNRESOLVED", String.class);
        long conflictId = objectMapper.readTree(unresolved.getBody())
                .get(0).get("id").asLong();

        wireMock.stubFor(put(urlEqualTo("/api/customers/1"))
                .willReturn(okJson("{ \"id\": 1 }")));
        wireMock.stubFor(post(urlEqualTo("/ws"))
                .withRequestBody(containing("UpdateCustomerRequest"))
                .willReturn(ok("""
                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                                       xmlns:cust="http://integration.com/systemb/customer">
                          <soap:Body>
                            <cust:UpdateCustomerResponse>
                              <cust:customer>
                                <cust:customerNumber>10</cust:customerNumber>
                                <cust:name>Alice Johnson</cust:name>
                                <cust:contactEmail>alice@example.com</cust:contactEmail>
                                <cust:phone>+1-555-0101</cust:phone>
                                <cust:lastModified>2026-09-07T10:00:00Z</cust:lastModified>
                              </cust:customer>
                            </cust:UpdateCustomerResponse>
                          </soap:Body>
                        </soap:Envelope>
                        """).withHeader("Content-Type", "text/xml")));

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        ResponseEntity<String> resolved = rest.postForEntity(
                "/api/sync/conflicts/" + conflictId + "/resolve",
                new org.springframework.http.HttpEntity<>("{\"resolution\":\"A_WINS\"}", headers),
                String.class);

        assertThat(resolved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resolved.getBody())
                .contains("\"resolution\":\"A_WINS\"")
                .contains("\"fieldName\":\"name\"")
                .contains("\"resolvedBy\":\"dashboard-user\"");

        wireMock.verify(1, putRequestedFor(urlEqualTo("/api/customers/1")));
        wireMock.verify(1, postRequestedFor(urlEqualTo("/ws"))
                .withRequestBody(containing("UpdateCustomerRequest")));
    }
}