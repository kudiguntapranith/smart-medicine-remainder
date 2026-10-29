package com.smartmedicine.util;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Reusable input validation. All failures throw {@link ValidationException} with a friendly message. */
public final class Validator {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_.]{3,30}");

    private Validator() { }

    public static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) throw new ValidationException(field + " is required.");
        String v = value.trim();
        if (v.length() > maxLength) throw new ValidationException(field + " must be at most " + maxLength + " characters.");
        return v;
    }

    public static String username(String value) {
        String v = requireText(value, "Username", 30);
        if (!USERNAME.matcher(v).matches())
            throw new ValidationException("Username must be 3-30 characters: letters, digits, '_' or '.'.");
        return v;
    }

    public static void password(String value) {
        if (value == null || value.length() < 6) throw new ValidationException("Password must be at least 6 characters.");
        if (value.length() > 100) throw new ValidationException("Password is too long.");
    }

    /** Parses 24-hour "HH:mm". */
    public static LocalTime parseTime(String text) {
        if (text == null || text.isBlank()) throw new ValidationException("Please enter a time in HH:mm format.");
        try {
            String t = text.trim();
            if (t.length() == 4 && t.charAt(1) == ':') t = "0" + t;
            return LocalTime.parse(t);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid time '" + text.trim() + "'. Use 24-hour HH:mm, e.g. 08:30 or 20:00.");
        }
    }
}
