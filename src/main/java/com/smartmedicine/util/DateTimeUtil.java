package com.smartmedicine.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class DateTimeUtil {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_24 = DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeUtil() { }

    public static String formatDate(LocalDate d) { return d == null ? "" : DATE.format(d); }
    public static String formatLongDate(LocalDate d) { return LONG_DATE.format(d); }
    public static String formatTime(LocalTime t) { return TIME.format(t); }
    public static String formatTime24(LocalTime t) { return TIME_24.format(t); }
    public static LocalDateTime nowMinutes() { return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES); }
}
