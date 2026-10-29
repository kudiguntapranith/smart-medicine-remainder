package com.smartmedicine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smartmedicine.model.DisplayStatus;
import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.model.DoseStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DoseStatusCalculatorTest {
    private static final LocalDate DAY = LocalDate.of(2026, 3, 10);

    private DoseRecord dose(long id, int hour, DoseStatus status) {
        return new DoseRecord(id, 1, 1, 1L, "Aspirin", "1 tablet", DAY, LocalTime.of(hour, 0), status, null);
    }

    @Test void futurePendingDoseIsUpcomingNotMissed() {
        assertEquals(DisplayStatus.UPCOMING, DoseStatusCalculator.effective(dose(1, 20, DoseStatus.PENDING), DAY.atTime(9, 0), 60));
    }

    @Test void pendingDoseWithinGraceIsDue() {
        assertEquals(DisplayStatus.DUE, DoseStatusCalculator.effective(dose(1, 8, DoseStatus.PENDING), DAY.atTime(8, 30), 60));
        assertEquals(DisplayStatus.DUE, DoseStatusCalculator.effective(dose(1, 8, DoseStatus.PENDING), DAY.atTime(9, 0), 60));
    }

    @Test void pendingDoseAfterGraceIsMissed() {
        assertEquals(DisplayStatus.MISSED, DoseStatusCalculator.effective(dose(1, 8, DoseStatus.PENDING), DAY.atTime(9, 1), 60));
    }

    @Test void takenStaysTakenAndMorningDoseDoesNotAffectEvening() {
        LocalDateTime now = DAY.atTime(21, 0);
        assertEquals(DisplayStatus.TAKEN, DoseStatusCalculator.effective(dose(1, 8, DoseStatus.TAKEN), now, 60));
        assertEquals(DisplayStatus.DUE, DoseStatusCalculator.effective(dose(2, 20, DoseStatus.PENDING), now, 60));
    }
}
