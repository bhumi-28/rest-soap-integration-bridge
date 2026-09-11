package com.integration.bridge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeDetectorTest {

    private ChangeDetector detector;
    private Instant lastSync;

    @BeforeEach
    void setUp() {
        detector = new ChangeDetector();
        lastSync = Instant.parse("2026-09-01T10:00:00Z");
    }

    @Test
    void identicalValuesMeansNoChange() {
        Instant now = Instant.parse("2026-09-02T10:00:00Z");
        assertThat(detector.detect("Alice", now, "Alice", now, lastSync))
                .isEqualTo(ChangeDetector.FieldChange.NO_CHANGE);
    }

    @Test
    void onlyASideChangedAfterLastSyncWins() {
        Instant aChanged = Instant.parse("2026-09-02T10:00:00Z");
        Instant bOld = Instant.parse("2026-08-01T10:00:00Z");
        assertThat(detector.detect("New Name", aChanged, "Old Name", bOld, lastSync))
                .isEqualTo(ChangeDetector.FieldChange.A_CHANGED);
    }

    @Test
    void onlyBSideChangedAfterLastSyncWins() {
        Instant aOld = Instant.parse("2026-08-01T10:00:00Z");
        Instant bChanged = Instant.parse("2026-09-03T10:00:00Z");
        assertThat(detector.detect("Old Name", aOld, "New Name", bChanged, lastSync))
                .isEqualTo(ChangeDetector.FieldChange.B_CHANGED);
    }

    @Test
    void bothSidesChangedAfterLastSyncIsConflict() {
        Instant aChanged = Instant.parse("2026-09-02T10:00:00Z");
        Instant bChanged = Instant.parse("2026-09-03T10:00:00Z");
        assertThat(detector.detect("Version A", aChanged, "Version B", bChanged, lastSync))
                .isEqualTo(ChangeDetector.FieldChange.CONFLICT);
    }

    @Test
    void differingValuesWithNoTimestampsAndLastSyncNullIsConflict() {
        assertThat(detector.detect("A value", null, "B value", null, null))
                .isEqualTo(ChangeDetector.FieldChange.CONFLICT);
    }

    @Test
    void differingValuesBeforeLastSyncMeansNoChange() {
        Instant old = Instant.parse("2026-01-01T10:00:00Z");
        assertThat(detector.detect("Value 1", old, "Value 2", old, lastSync))
                .isEqualTo(ChangeDetector.FieldChange.NO_CHANGE);
    }

    @Test
    void differingValuesNullableSideIgnored() {
        Instant aChanged = Instant.parse("2026-09-02T10:00:00Z");
        assertThat(detector.detect("Phone A", aChanged, null, null, lastSync))
                .isEqualTo(ChangeDetector.FieldChange.A_CHANGED);
    }
}