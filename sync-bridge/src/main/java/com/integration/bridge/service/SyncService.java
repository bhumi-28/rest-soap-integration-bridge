package com.integration.bridge.service;

import com.integration.bridge.client.SystemARestClient;
import com.integration.bridge.client.SystemBSoapClient;
import com.integration.bridge.dto.CustomerRecordADto;
import com.integration.bridge.dto.CustomerRecordBDto;
import com.integration.bridge.entity.CustomerRecordMapping;
import com.integration.bridge.entity.SyncConflict;
import com.integration.bridge.entity.SyncJob;
import com.integration.bridge.repository.CustomerRecordMappingRepository;
import com.integration.bridge.repository.SyncConflictRepository;
import com.integration.bridge.repository.SyncJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final SystemARestClient restClient;
    private final SystemBSoapClient soapClient;
    private final CustomerRecordMappingRepository mappingRepository;
    private final SyncJobRepository jobRepository;
    private final SyncConflictRepository conflictRepository;
    private final ChangeDetector changeDetector;

    public SyncService(SystemARestClient restClient, SystemBSoapClient soapClient,
                       CustomerRecordMappingRepository mappingRepository,
                       SyncJobRepository jobRepository, SyncConflictRepository conflictRepository,
                       ChangeDetector changeDetector) {
        this.restClient = restClient;
        this.soapClient = soapClient;
        this.mappingRepository = mappingRepository;
        this.jobRepository = jobRepository;
        this.conflictRepository = conflictRepository;
        this.changeDetector = changeDetector;
    }

    @Transactional
    public SyncJob executeSync() {
        SyncJob job = new SyncJob();
        job = jobRepository.save(job);

        int processed = 0;
        int conflicted = 0;
        int failed = 0;

        try {
            List<CustomerRecordADto> recordsA = restClient.getAllCustomers();
            List<CustomerRecordBDto> recordsB = soapClient.getAllCustomers();

            Map<String, CustomerRecordADto> byEmailA = recordsA.stream()
                    .filter(r -> r.getEmailAddress() != null)
                    .collect(Collectors.toMap(
                            r -> r.getEmailAddress().toLowerCase(),
                            r -> r,
                            (a, b) -> a));

            Map<String, CustomerRecordBDto> byEmailB = recordsB.stream()
                    .filter(r -> r.getContactEmail() != null)
                    .collect(Collectors.toMap(
                            r -> r.getContactEmail().toLowerCase(),
                            r -> r,
                            (a, b) -> a));

            Set<String> allEmails = new HashSet<>();
            allEmails.addAll(byEmailA.keySet());
            allEmails.addAll(byEmailB.keySet());

            for (String email : allEmails) {
                try {
                    CustomerRecordADto recordA = byEmailA.get(email);
                    CustomerRecordBDto recordB = byEmailB.get(email);

                    if (recordA != null && recordB != null) {
                        Map<String, Boolean> fieldResults = processExistingMapping(recordA, recordB, job);
                        processed++;
                        if (fieldResults.get("conflicted")) {
                            conflicted++;
                        }
                        if (fieldResults.get("failed")) {
                            failed++;
                        }
                    } else if (recordA != null) {
                        try {
                            processUnmatchedA(recordA, job);
                            processed++;
                        } catch (Exception e) {
                            log.error("Failed to create mapping for new record {}: {}", recordA.getEmailAddress(), e.getMessage());
                            failed++;
                        }
                    } else if (recordB != null) {
                        try {
                            processUnmatchedB(recordB, job);
                            processed++;
                        } catch (Exception e) {
                            log.error("Failed to create mapping for new record {}: {}", recordB.getContactEmail(), e.getMessage());
                            failed++;
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to process record with email {}: {}", email, e.getMessage());
                    failed++;
                }
            }

            job.setRecordsProcessed(processed);
            job.setRecordsConflicted(conflicted);
            job.setRecordsFailed(failed);
            job.setCompletedAt(Instant.now());

            if (failed == 0) {
                job.setStatus(SyncJob.JobStatus.SUCCESS);
            } else if (processed > failed) {
                job.setStatus(SyncJob.JobStatus.PARTIAL);
            } else {
                job.setStatus(SyncJob.JobStatus.FAILED);
            }

        } catch (Exception e) {
            log.error("Sync job failed completely: {}", e.getMessage());
            job.setStatus(SyncJob.JobStatus.FAILED);
            job.setRecordsFailed(failed > 0 ? failed : 1);
            job.setCompletedAt(Instant.now());
        }

        return jobRepository.save(job);
    }

    private Map<String, Boolean> processExistingMapping(CustomerRecordADto recordA, CustomerRecordBDto recordB, SyncJob job) {
        Map<String, Boolean> result = new HashMap<>();
        boolean conflicted = false;
        boolean failed = false;

        CustomerRecordMapping mapping = mappingRepository.findBySystemAId(recordA.getId())
                .orElseGet(() -> {
                    CustomerRecordMapping m = new CustomerRecordMapping();
                    m.setSystemAId(recordA.getId());
                    m.setSystemBId(recordB.getCustomerNumber());
                    m.setCanonicalName(recordA.getFullName());
                    m.setCanonicalEmail(recordA.getEmailAddress());
                    m.setCanonicalPhone(recordA.getPhoneNumber());
                    m.setLastSyncedAt(Instant.now());
                    return m;
                });

        Map<String, FieldResult> fieldResults = evaluateFields(mapping, recordA, recordB);

        for (FieldResult fr : fieldResults.values()) {
            if (fr.change == ChangeDetector.FieldChange.CONFLICT) {
                createConflict(mapping, fr.fieldName, fr.valueA, fr.valueB, job);
                conflicted = true;
            } else if (fr.change == ChangeDetector.FieldChange.A_CHANGED) {
                if (!propagateAtoB(mapping, recordA.getFullName(), recordA.getEmailAddress(), recordA.getPhoneNumber())) {
                    failed = true;
                }
            } else if (fr.change == ChangeDetector.FieldChange.B_CHANGED) {
                if (!propagateBtoA(mapping, recordB.getName(), recordB.getContactEmail(), recordB.getPhone())) {
                    failed = true;
                }
            }
        }

        if (conflicted) {
            mapping.setSyncStatus(CustomerRecordMapping.SyncStatus.CONFLICT);
        } else {
            mapping.setCanonicalName(recordA.getFullName());
            mapping.setCanonicalEmail(recordA.getEmailAddress());
            mapping.setCanonicalPhone(recordA.getPhoneNumber());
            mapping.setLastSyncedAt(Instant.now());
            if (mapping.getSyncStatus() != CustomerRecordMapping.SyncStatus.ERROR) {
                mapping.setSyncStatus(CustomerRecordMapping.SyncStatus.IN_SYNC);
            }
        }
        mappingRepository.save(mapping);

        result.put("conflicted", conflicted);
        result.put("failed", failed);
        return result;
    }

    private Map<String, FieldResult> evaluateFields(CustomerRecordMapping mapping,
                                                    CustomerRecordADto recordA,
                                                    CustomerRecordBDto recordB) {
        Map<String, FieldResult> results = new LinkedHashMap<>();

        results.put("name", evaluateField("name",
                recordA.getFullName(), recordA.getUpdatedAt(),
                recordB.getName(), recordB.getLastModified(), mapping.getLastSyncedAt()));

        results.put("email", evaluateField("email",
                recordA.getEmailAddress(), recordA.getUpdatedAt(),
                recordB.getContactEmail(), recordB.getLastModified(), mapping.getLastSyncedAt()));

        results.put("phone", evaluateField("phone",
                recordA.getPhoneNumber(), recordA.getUpdatedAt(),
                recordB.getPhone(), recordB.getLastModified(), mapping.getLastSyncedAt()));

        return results;
    }

    private FieldResult evaluateField(String name, String valueA, Instant updatedAtA,
                                      String valueB, Instant lastModifiedB, Instant lastSyncedAt) {
        ChangeDetector.FieldChange change = changeDetector.detect(valueA, updatedAtA, valueB, lastModifiedB, lastSyncedAt);
        return new FieldResult(name, valueA, valueB, change);
    }

    private void processUnmatchedA(CustomerRecordADto recordA, SyncJob job) {
        Optional<CustomerRecordMapping> existingMapping = mappingRepository.findBySystemAId(recordA.getId());
        if (existingMapping.isPresent()) return;

        CustomerRecordMapping mapping = new CustomerRecordMapping();
        mapping.setSystemAId(recordA.getId());
        mapping.setCanonicalName(recordA.getFullName());
        mapping.setCanonicalEmail(recordA.getEmailAddress());
        mapping.setCanonicalPhone(recordA.getPhoneNumber());
        mapping.setLastSyncedAt(Instant.now());
        mapping.setSyncStatus(CustomerRecordMapping.SyncStatus.IN_SYNC);

        try {
            CustomerRecordBDto created = soapClient.createCustomer(
                    recordA.getFullName(), recordA.getEmailAddress(), recordA.getPhoneNumber());
            mapping.setSystemBId(created.getCustomerNumber());
        } catch (Exception e) {
            log.warn("Could not push new record to System B: {}", e.getMessage());
            mapping.setSyncStatus(CustomerRecordMapping.SyncStatus.ERROR);
        }

        mappingRepository.save(mapping);
    }

    private void processUnmatchedB(CustomerRecordBDto recordB, SyncJob job) {
        Optional<CustomerRecordMapping> existingMapping = mappingRepository.findBySystemBId(recordB.getCustomerNumber());
        if (existingMapping.isPresent()) return;

        CustomerRecordMapping mapping = new CustomerRecordMapping();
        mapping.setSystemBId(recordB.getCustomerNumber());
        mapping.setCanonicalName(recordB.getName());
        mapping.setCanonicalEmail(recordB.getContactEmail());
        mapping.setCanonicalPhone(recordB.getPhone());
        mapping.setLastSyncedAt(Instant.now());
        mapping.setSyncStatus(CustomerRecordMapping.SyncStatus.IN_SYNC);

        try {
            CustomerRecordADto created = restClient.createCustomer(toADto(recordB));
            if (created != null && created.getId() != null) {
                mapping.setSystemAId(created.getId());
            }
        } catch (Exception e) {
            log.warn("Could not push new record to System A: {}", e.getMessage());
            mapping.setSyncStatus(CustomerRecordMapping.SyncStatus.ERROR);
        }

        mappingRepository.save(mapping);
    }

    private CustomerRecordADto toADto(CustomerRecordBDto recordB) {
        CustomerRecordADto dto = new CustomerRecordADto();
        dto.setFullName(recordB.getName());
        dto.setEmailAddress(recordB.getContactEmail());
        dto.setPhoneNumber(recordB.getPhone());
        return dto;
    }

    private void createConflict(CustomerRecordMapping mapping, String fieldName,
                                String valueA, String valueB, SyncJob job) {
        SyncConflict conflict = new SyncConflict();
        conflict.setMapping(mapping);
        conflict.setSyncJob(job);
        conflict.setFieldName(fieldName);
        conflict.setValueFromA(valueA);
        conflict.setValueFromB(valueB);
        conflict.setResolution(SyncConflict.Resolution.UNRESOLVED);
        conflictRepository.save(conflict);
    }

    private boolean propagateAtoB(CustomerRecordMapping mapping, String name, String email, String phone) {
        try {
            if (mapping.getSystemBId() != null) {
                soapClient.updateCustomer(mapping.getSystemBId(), name, email, phone);
            }
            mapping.setCanonicalName(name);
            mapping.setCanonicalEmail(email);
            mapping.setCanonicalPhone(phone);
            mapping.setLastSyncedAt(Instant.now());
            return true;
        } catch (Exception e) {
            log.error("Failed to propagate A->B for mapping {}: {}", mapping.getId(), e.getMessage());
            return false;
        }
    }

    private boolean propagateBtoA(CustomerRecordMapping mapping, String name, String email, String phone) {
        try {
            if (mapping.getSystemAId() != null) {
                CustomerRecordADto dto = new CustomerRecordADto();
                dto.setFullName(name);
                dto.setEmailAddress(email);
                dto.setPhoneNumber(phone);
                restClient.updateCustomer(mapping.getSystemAId(), dto);
            }
            mapping.setCanonicalName(name);
            mapping.setCanonicalEmail(email);
            mapping.setCanonicalPhone(phone);
            mapping.setLastSyncedAt(Instant.now());
            return true;
        } catch (Exception e) {
            log.error("Failed to propagate B->A for mapping {}: {}", mapping.getId(), e.getMessage());
            return false;
        }
    }

    @Transactional
    public SyncConflict resolveConflict(Long conflictId, String resolution, String manualValue) {
        SyncConflict conflict = conflictRepository.findById(conflictId)
                .orElseThrow(() -> new IllegalArgumentException("Conflict not found: " + conflictId));

        if (conflict.getResolution() != SyncConflict.Resolution.UNRESOLVED) {
            throw new IllegalStateException("Conflict already resolved");
        }

        CustomerRecordMapping mapping = conflict.getMapping();
        String winningValue;

        switch (resolution) {
            case "A_WINS":
                conflict.setResolution(SyncConflict.Resolution.A_WINS);
                winningValue = conflict.getValueFromA();
                break;
            case "B_WINS":
                conflict.setResolution(SyncConflict.Resolution.B_WINS);
                winningValue = conflict.getValueFromB();
                break;
            case "MANUAL":
                if (manualValue == null || manualValue.isBlank()) {
                    throw new IllegalArgumentException("Manual resolution requires a manualValue");
                }
                conflict.setResolution(SyncConflict.Resolution.MANUAL);
                winningValue = manualValue;
                break;
            default:
                throw new IllegalArgumentException("Invalid resolution: " + resolution);
        }

        conflict.setResolvedAt(Instant.now());
        conflict.setResolvedBy("dashboard-user");

        applyResolution(mapping, conflict.getFieldName(), winningValue);

        boolean noOpenConflicts = conflictRepository
                .findByMappingIdAndResolution(mapping.getId(), SyncConflict.Resolution.UNRESOLVED)
                .isEmpty();

        if (noOpenConflicts) {
            mapping.setSyncStatus(CustomerRecordMapping.SyncStatus.IN_SYNC);
        }
        mappingRepository.save(mapping);

        return conflictRepository.save(conflict);
    }

    private void applyResolution(CustomerRecordMapping mapping, String fieldName, String value) {
        String name = mapping.getCanonicalName();
        String email = mapping.getCanonicalEmail();
        String phone = mapping.getCanonicalPhone();

        switch (fieldName) {
            case "name" -> name = value;
            case "email" -> email = value;
            case "phone" -> phone = value;
        }

        try {
            if (mapping.getSystemAId() != null) {
                CustomerRecordADto dto = new CustomerRecordADto();
                dto.setFullName(name);
                dto.setEmailAddress(email);
                dto.setPhoneNumber(phone);
                restClient.updateCustomer(mapping.getSystemAId(), dto);
            }
        } catch (Exception e) {
            log.error("Failed to write resolution to System A: {}", e.getMessage());
        }

        try {
            if (mapping.getSystemBId() != null) {
                soapClient.updateCustomer(mapping.getSystemBId(), name, email, phone);
            }
        } catch (Exception e) {
            log.error("Failed to write resolution to System B: {}", e.getMessage());
        }

        mapping.setCanonicalName(name);
        mapping.setCanonicalEmail(email);
        mapping.setCanonicalPhone(phone);
        mapping.setLastSyncedAt(Instant.now());
    }

    private static class FieldResult {
        final String fieldName;
        final String valueA;
        final String valueB;
        final ChangeDetector.FieldChange change;

        FieldResult(String fieldName, String valueA, String valueB, ChangeDetector.FieldChange change) {
            this.fieldName = fieldName;
            this.valueA = valueA;
            this.valueB = valueB;
            this.change = change;
        }
    }
}