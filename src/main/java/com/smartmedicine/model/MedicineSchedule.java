package com.smartmedicine.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record MedicineSchedule(long id, long medicineId, LocalTime reminderTime, boolean active, LocalDateTime createdAt) { }
