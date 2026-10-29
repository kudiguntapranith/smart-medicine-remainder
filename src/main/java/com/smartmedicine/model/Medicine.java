package com.smartmedicine.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import com.smartmedicine.util.DateTimeUtil;

/** A medicine and its (active) reminder times. endDate == null means the schedule is ongoing. */
public record Medicine(long id, long userId, String name, String dosage, Frequency frequency,
                       LocalDate startDate, LocalDate endDate, String notes, boolean active,
                       List<LocalTime> times) {

    public boolean isScheduledOn(LocalDate date) {
        return active && !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate));
    }

    public String timesLabel() {
        return times.stream().map(DateTimeUtil::formatTime).collect(Collectors.joining(", "));
    }

    public String periodLabel() {
        return DateTimeUtil.formatDate(startDate) + " \u2192 " + (endDate == null ? "Ongoing" : DateTimeUtil.formatDate(endDate));
    }
}
