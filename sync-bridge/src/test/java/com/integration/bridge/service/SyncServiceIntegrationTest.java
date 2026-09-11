package com.integration.bridge.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.integration.bridge.entity.CustomerRecordMapping;
import com.integration.bridge.entity.SyncConflict;
import com.integration.bridge.entity.SyncJob;
import com.integration.bridge.repository.CustomerRecordMappingRepository;
import com.integration.bridge.repository.SyncConflictRepository;
import com.integration.bridge.repository.SyncJobRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SyncServiceIntegrationTest {

    private static WireMockServer wireMock;

    @Autowired
    private SyncService syncService;

    @Autowired
    private SyncJobRepository jobRepository;

    @Autowired
    private SyncConflictRepository conflictRepository;

    @Autowired
    private CustomerRecordMappingRepository mappingRepository;

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

    private void stubSystemAByEmail(String email) {
        wireMock.stubFor(get(urlEqualTo("/api/customers"))
                .willReturn(okJson("""
                        [
                          {
                            "id": 1,
                            "fullName": "Alice Johnson",
                            "emailAddress": "%s",
                            "phoneNumber": "+1-555-0101",
                            "updatedAt": "2026-09-02T10:00:00Z"
                          }
                        ]
                        """.formatted(email))));
    }

    private void stubSystemBByEmail(String email, String name, String lastModified) {
        wireMock.stubFor(post(urlEqualTo("/ws"))
                .withRequestBody(containing("GetAllCustomersRequest"))
                .willReturn(ok("""
                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                                       xmlns:cust="http://integration.com/systemb/customer">
                          <soap:Body>
                            <cust:GetAllCustomersResponse>
                              <cust:customers>
                                %s
                              </cust:customers>
                            </cust:GetAllCustomersResponse>
                          </soap:Body>
                        </soap:Envelope>
                        """.formatted(soapCustomer(10L, name, email, "+1-555-0101", lastModified)))
                        .withHeader("Content-Type", "text/xml")));
    }

    private void stubSystemBUpdate(Long customerNumber, String name, String email, String phone) {
        wireMock.stubFor(post(urlEqualTo("/ws"))
                .withRequestBody(containing("UpdateCustomerRequest"))
                .willReturn(ok("""
                        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                                       xmlns:cust="http://integration.com/systemb/customer">
                          <soap:Body>
                            <cust:UpdateCustomerResponse>
                              %s
                            </cust:UpdateCustomerResponse>
                          </soap:Body>
                        </soap:Envelope>
                        """.formatted(soapCustomer(customerNumber, name, email, phone, "2026-09-06T10:00:00Z")))
                        .withHeader("Content-Type", "text/xml")));
    }

    private String soapCustomer(Long customerNumber, String name, String email, String phone, String lastModified) {
        return """
                <cust:customer>
                  <cust:customerNumber>%d</cust:customerNumber>
                  <cust:name>%s</cust:name>
                  <cust:contactEmail>%s</cust:contactEmail>
                  <cust:phone>%s</cust:phone>
                  <cust:lastModified>%s</cust:lastModified>
                </cust:customer>
                """.formatted(customerNumber, name, email, phone, lastModified);
    }

    @Test
    void cleanSyncProducesSuccessWithoutConflicts() {
        stubSystemAByEmail("alice@example.com");
        stubSystemBByEmail("alice@example.com", "Alice Johnson", "2026-09-01T10:00:00Z");

        SyncJob job = syncService.executeSync();

        assertThat(job.getStatus()).isEqualTo(SyncJob.JobStatus.SUCCESS);
        assertThat(job.getRecordsFailed()).isZero();
        assertThat(job.getRecordsConflicted()).isZero();
        assertThat(job.getRecordsProcessed()).isEqualTo(1);

        List<SyncConflict> conflicts = conflictRepository.findByResolution(SyncConflict.Resolution.UNRESOLVED);
        assertThat(conflicts).isEmpty();
    }

    @Test
    void bothSidesChangedCreatesConflictWithoutWriteBack() {
        stubSystemAByEmail("alice@example.com");
        stubSystemBByEmail("alice@example.com", "Alice Johnson Changed", "2026-09-05T10:00:00Z");

        CustomerRecordMapping mapping = new CustomerRecordMapping();
        mapping.setSystemAId(1L);
        mapping.setSystemBId(10L);
        mapping.setCanonicalName("Alice Johnson");
        mapping.setCanonicalEmail("alice@example.com");
        mapping.setCanonicalPhone("+1-555-0101");
        mapping.setLastSyncedAt(Instant.parse("2026-09-01T10:00:00Z"));
        mapping.setSyncStatus(CustomerRecordMapping.SyncStatus.IN_SYNC);
        mappingRepository.save(mapping);

        SyncJob job = syncService.executeSync();

        assertThat(job.getRecordsConflicted()).isEqualTo(1);

        List<SyncConflict> conflicts = conflictRepository.findByResolution(SyncConflict.Resolution.UNRESOLVED);
        assertThat(conflicts).hasSize(1);
        SyncConflict conflict = conflicts.get(0);
        assertThat(conflict.getFieldName()).isEqualTo("name");
        assertThat(conflict.getValueFromA()).isEqualTo("Alice Johnson");
        assertThat(conflict.getValueFromB()).isEqualTo("Alice Johnson Changed");

        CustomerRecordMapping updated = mappingRepository.findById(mapping.getId()).orElseThrow();
        assertThat(updated.getSyncStatus()).isEqualTo(CustomerRecordMapping.SyncStatus.CONFLICT);

        wireMock.verify(0, putRequestedFor(urlPathMatching("/api/customers/[0-9]+")));
        wireMock.verify(0, postRequestedFor(urlEqualTo("/ws"))
                .withRequestBody(containing("UpdateCustomerRequest")));
    }

    @Test
    void systemBDownMarksJobFailedWithoutCrashing() {
        stubSystemAByEmail("alice@example.com");
        wireMock.stubFor(post(urlEqualTo("/ws"))
                .willReturn(aResponse().withStatus(500)));

        SyncJob job = syncService.executeSync();

        assertThat(job.getStatus()).isEqualTo(SyncJob.JobStatus.FAILED);
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void oneSidedChangePropagatesToOtherSystem() {
        wireMock.stubFor(get(urlEqualTo("/api/customers"))
                .willReturn(okJson("""
                        [
                          {
                            "id": 1,
                            "fullName": "Alice Johnson NEW",
                            "emailAddress": "alice@example.com",
                            "phoneNumber": "+1-555-0101",
                            "updatedAt": "2026-09-05T10:00:00Z"
                          }
                        ]
                        """)));
        stubSystemBByEmail("alice@example.com", "Alice Johnson", "2026-09-01T10:00:00Z");
        stubSystemBUpdate(10L, "Alice Johnson NEW", "alice@example.com", "+1-555-0101");

        CustomerRecordMapping mapping = new CustomerRecordMapping();
        mapping.setSystemAId(1L);
        mapping.setSystemBId(10L);
        mapping.setCanonicalName("Alice Johnson");
        mapping.setCanonicalEmail("alice@example.com");
        mapping.setCanonicalPhone("+1-555-0101");
        mapping.setLastSyncedAt(Instant.parse("2026-09-01T10:00:00Z"));
        mapping.setSyncStatus(CustomerRecordMapping.SyncStatus.IN_SYNC);
        mappingRepository.save(mapping);

        SyncJob job = syncService.executeSync();

        assertThat(job.getStatus()).isEqualTo(SyncJob.JobStatus.SUCCESS);

        wireMock.verify(1, postRequestedFor(urlEqualTo("/ws"))
                .withRequestBody(containing("UpdateCustomerRequest")));

        CustomerRecordMapping updated = mappingRepository.findById(mapping.getId()).orElseThrow();
        assertThat(updated.getCanonicalName()).isEqualTo("Alice Johnson NEW");
        assertThat(updated.getSyncStatus()).isEqualTo(CustomerRecordMapping.SyncStatus.IN_SYNC);
    }

    @Test
    void resolvingConflictWritesBackToBothSystems() {
        stubSystemAByEmail("alice@example.com");
        stubSystemBByEmail("alice@example.com", "Alice Johnson Changed", "2026-09-05T10:00:00Z");

        CustomerRecordMapping mapping = new CustomerRecordMapping();
        mapping.setSystemAId(1L);
        mapping.setSystemBId(10L);
        mapping.setCanonicalName("Alice Johnson");
        mapping.setCanonicalEmail("alice@example.com");
        mapping.setCanonicalPhone("+1-555-0101");
        mapping.setLastSyncedAt(Instant.parse("2026-09-01T10:00:00Z"));
        mapping.setSyncStatus(CustomerRecordMapping.SyncStatus.IN_SYNC);
        mappingRepository.save(mapping);

        syncService.executeSync();

        SyncConflict conflict = conflictRepository.findByResolution(SyncConflict.Resolution.UNRESOLVED).get(0);

        wireMock.stubFor(put(urlEqualTo("/api/customers/1"))
                .willReturn(okJson("{ \"id\": 1 }")));
        stubSystemBUpdate(10L, "Alice Johnson", "alice@example.com", "+1-555-0101");

        SyncConflict resolved = syncService.resolveConflict(conflict.getId(), "A_WINS", null);

        assertThat(resolved.getResolution()).isEqualTo(SyncConflict.Resolution.A_WINS);
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(resolved.getResolvedBy()).isEqualTo("dashboard-user");

        CustomerRecordMapping updated = mappingRepository.findById(mapping.getId()).orElseThrow();
        assertThat(updated.getSyncStatus()).isEqualTo(CustomerRecordMapping.SyncStatus.IN_SYNC);
        assertThat(updated.getCanonicalName()).isEqualTo("Alice Johnson");

        wireMock.verify(1, putRequestedFor(urlEqualTo("/api/customers/1")));
        wireMock.verify(1, postRequestedFor(urlEqualTo("/ws"))
                .withRequestBody(containing("UpdateCustomerRequest")));
    }
}