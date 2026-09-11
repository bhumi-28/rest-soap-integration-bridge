package com.integration.bridge.service;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Objects;

@Component
public class ChangeDetector {

    public enum FieldChange {
        NO_CHANGE,
        A_CHANGED,
        B_CHANGED,
        CONFLICT
    }

    /**
     * Compares one field across the two systems relative to the last sync point.
     *
     * Values disagreeing while only one side changed since last sync -> that side wins (propagate).
     * Values disagreeing while both sides changed since last sync -> true conflict.
     * Values agreeing (or neither changed) -> no change.
     */
    public FieldChange detect(String valueA, Instant updatedAtA,
                              String valueB, Instant lastModifiedB,
                              Instant lastSyncedAt) {
        if (Objects.equals(valueA, valueB)) {
            return FieldChange.NO_CHANGE;
        }

        boolean aChanged = lastSyncedAt == null
                || (updatedAtA != null && updatedAtA.isAfter(lastSyncedAt));
        boolean bChanged = lastSyncedAt == null
                || (lastModifiedB != null && lastModifiedB.isAfter(lastSyncedAt));

        if (aChanged && bChanged) {
            return FieldChange.CONFLICT;
        }
        if (aChanged) {
            return FieldChange.A_CHANGED;
        }
        if (bChanged) {
            return FieldChange.B_CHANGED;
        }
        return FieldChange.NO_CHANGE;
    }
}