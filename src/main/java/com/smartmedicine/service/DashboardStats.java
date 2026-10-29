package com.smartmedicine.service;

import com.smartmedicine.model.DisplayStatus;
import com.smartmedicine.model.DoseRecord;
import java.time.LocalDateTime;
import java.util.List;

/** Counters for today's dashboard. {@code next} is the earliest upcoming dose, or null. */
public record DashboardStats(int total, int taken, int pending, int missed, DoseRecord next) {

    public static DashboardStats of(List<DoseRecord> doses, LocalDateTime now, int graceMinutes) {
        int taken = 0, pending = 0, missed = 0;
        DoseRecord next = null;
        for (DoseRecord d : doses) {
            DisplayStatus s = DoseStatusCalculator.effective(d, now, graceMinutes);
            switch (s) {
                case TAKEN -> taken++;
                case MISSED -> missed++;
                default -> pending++;
            }
            if (s == DisplayStatus.UPCOMING && (next == null || d.scheduledDateTime().isBefore(next.scheduledDateTime()))) {
                next = d;
            }
        }
        return new DashboardStats(doses.size(), taken, pending, missed, next);
    }
}
