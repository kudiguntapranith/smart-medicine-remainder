package com.smartmedicine.service;

import com.smartmedicine.model.DisplayStatus;
import com.smartmedicine.model.DoseRecord;
import java.time.LocalDateTime;

/** Derives the user-visible status of a dose. A ringing alarm never changes a dose to TAKEN. */
public final class DoseStatusCalculator {
    private DoseStatusCalculator() { }

    public static DisplayStatus effective(DoseRecord dose, LocalDateTime now, int graceMinutes) {
        switch (dose.status()) {
            case TAKEN: return DisplayStatus.TAKEN;
            case MISSED: return DisplayStatus.MISSED;
            default:
                LocalDateTime due = dose.scheduledDateTime();
                if (now.isBefore(due)) return DisplayStatus.UPCOMING;
                if (now.isAfter(due.plusMinutes(graceMinutes))) return DisplayStatus.MISSED;
                return DisplayStatus.DUE;
        }
    }
}
