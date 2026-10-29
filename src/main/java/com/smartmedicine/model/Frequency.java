package com.smartmedicine.model;

import java.time.LocalTime;
import java.util.List;

public enum Frequency {
    ONCE_DAILY("Once daily", 1, List.of(LocalTime.of(8, 0))),
    TWICE_DAILY("Twice daily", 2, List.of(LocalTime.of(8, 0), LocalTime.of(20, 0))),
    THREE_TIMES_DAILY("Three times daily", 3, List.of(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0))),
    CUSTOM("Custom", 0, List.of());

    private final String label;
    private final int expectedTimes;
    private final List<LocalTime> defaultTimes;

    Frequency(String label, int expectedTimes, List<LocalTime> defaultTimes) {
        this.label = label;
        this.expectedTimes = expectedTimes;
        this.defaultTimes = defaultTimes;
    }

    /** Required number of reminder times; 0 means "any number (at least one)". */
    public int expectedTimes() { return expectedTimes; }
    public List<LocalTime> defaultTimes() { return defaultTimes; }
    @Override public String toString() { return label; }
}
