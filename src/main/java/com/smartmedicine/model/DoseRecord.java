package com.smartmedicine.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** One individual scheduled dose (one row of medicine_history). */
public record DoseRecord(long id, long userId, long medicineId, Long scheduleId, String medicineName, String dosage,
                         LocalDate scheduledDate, LocalTime scheduledTime, DoseStatus status, LocalDateTime takenAt) {

    public LocalDateTime scheduledDateTime() { return LocalDateTime.of(scheduledDate, scheduledTime); }
}
