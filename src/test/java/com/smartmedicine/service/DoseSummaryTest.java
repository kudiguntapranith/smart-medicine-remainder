package com.smartmedicine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.model.DoseStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DoseSummaryTest {
    private DoseRecord dose(long id, LocalDate d, int hour, DoseStatus s) {
        return new DoseRecord(id, 1, 1, 1L, "Aspirin", "1 tablet", d, LocalTime.of(hour, 0), s, null);
    }

    @Test void summarizesPerDayNewestFirstAndDashboardCounts() {
        LocalDate d1 = LocalDate.of(2026, 3, 9), d2 = LocalDate.of(2026, 3, 10);
        List<DoseRecord> doses = List.of(dose(1, d1, 8, DoseStatus.TAKEN), dose(2, d1, 20, DoseStatus.MISSED),
                dose(3, d2, 8, DoseStatus.TAKEN), dose(4, d2, 20, DoseStatus.PENDING));
        var now = d2.atTime(10, 0);
        List<DaySummary> days = DoseService.summarize(doses, now, 60);
        assertEquals(d2, days.get(0).date());
        assertEquals(new DaySummary(d2, 1, 0, 1), days.get(0));
        assertEquals(new DaySummary(d1, 1, 1, 0), days.get(1));

        DashboardStats s = DashboardStats.of(doses.subList(2, 4), now, 60);
        assertEquals(2, s.total());
        assertEquals(1, s.taken());
        assertEquals(1, s.pending());
        assertEquals(4L, s.next().id());
    }
}
