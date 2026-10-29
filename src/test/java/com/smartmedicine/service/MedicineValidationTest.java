package com.smartmedicine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.smartmedicine.model.Frequency;
import com.smartmedicine.util.ValidationException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MedicineValidationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    @Test void acceptsValidInputAndSortsTimes() {
        List<LocalTime> t = MedicineService.validate("Aspirin", "1 tablet", Frequency.TWICE_DAILY, TODAY, null, null,
                List.of(LocalTime.of(20, 0), LocalTime.of(8, 0)));
        assertEquals(List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)), t);
    }

    @Test void rejectsBadInput() {
        assertThrows(ValidationException.class, () -> MedicineService.validate(" ", "1", Frequency.CUSTOM, TODAY, null, null, List.of(LocalTime.NOON)));
        assertThrows(ValidationException.class, () -> MedicineService.validate("A", "1", Frequency.CUSTOM, TODAY, TODAY.minusDays(1), null, List.of(LocalTime.NOON)));
        assertThrows(ValidationException.class, () -> MedicineService.validate("A", "1", Frequency.CUSTOM, TODAY, null, null, List.of()));
        assertThrows(ValidationException.class, () -> MedicineService.validate("A", "1", Frequency.THREE_TIMES_DAILY, TODAY, null, null, List.of(LocalTime.NOON)));
        assertThrows(ValidationException.class, () -> MedicineService.validate("A", "1", Frequency.CUSTOM, TODAY, null, null, List.of(LocalTime.NOON, LocalTime.NOON)));
    }
}
