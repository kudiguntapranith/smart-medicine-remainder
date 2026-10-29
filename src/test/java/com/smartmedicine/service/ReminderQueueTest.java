package com.smartmedicine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.model.DoseStatus;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReminderQueueTest {
    private static final LocalDate DAY = LocalDate.of(2026, 3, 10);
    private static final Duration GRACE = Duration.ofMinutes(60);

    private DoseRecord dose(long id, int hour, DoseStatus status) {
        return new DoseRecord(id, 1, 1, id, "Med" + id, "1 tablet", DAY, LocalTime.of(hour, 0), status, null);
    }

    @Test void firesOnlyDueDosesInTimeOrder() {
        ReminderQueue q = new ReminderQueue();
        q.rebuild(List.of(dose(3, 20, DoseStatus.PENDING), dose(1, 8, DoseStatus.PENDING), dose(2, 12, DoseStatus.PENDING)));
        List<DoseRecord> due = q.pollDue(DAY.atTime(12, 5), Duration.ofHours(6));
        assertEquals(List.of(1L, 2L), due.stream().map(DoseRecord::id).toList());
        assertEquals(1, q.size());   // the 20:00 dose is still queued
    }

    @Test void neverFiresTheSameDoseTwiceEvenAfterRebuild() {
        ReminderQueue q = new ReminderQueue();
        List<DoseRecord> doses = List.of(dose(1, 8, DoseStatus.PENDING));
        q.rebuild(doses);
        assertEquals(1, q.pollDue(DAY.atTime(8, 1), GRACE).size());
        q.rebuild(doses);            // rescheduling must not create a duplicate alarm
        assertTrue(q.pollDue(DAY.atTime(8, 2), GRACE).isEmpty());
    }

    @Test void skipsDosesOlderThanGraceAndNonPendingDoses() {
        ReminderQueue q = new ReminderQueue();
        q.rebuild(List.of(dose(1, 6, DoseStatus.PENDING), dose(2, 8, DoseStatus.TAKEN)));
        assertTrue(q.pollDue(DAY.atTime(10, 0), GRACE).isEmpty());
    }

    @Test void futureDosesAreNotFired() {
        ReminderQueue q = new ReminderQueue();
        q.rebuild(List.of(dose(1, 20, DoseStatus.PENDING)));
        assertTrue(q.pollDue(DAY.atTime(9, 0), GRACE).isEmpty());
        assertEquals(20, q.peekNext().orElseThrow().scheduledTime().getHour());
    }
}
