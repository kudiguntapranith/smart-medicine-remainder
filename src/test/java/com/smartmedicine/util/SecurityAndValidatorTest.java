package com.smartmedicine.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class SecurityAndValidatorTest {
    @Test void passwordIsHashedSaltedAndVerifiable() {
        String h1 = PasswordUtil.hash("secret123"), h2 = PasswordUtil.hash("secret123");
        assertNotEquals(h1, h2);
        assertFalse(h1.contains("secret123"));
        assertTrue(PasswordUtil.verify("secret123", h1));
        assertFalse(PasswordUtil.verify("wrong", h1));
        assertFalse(PasswordUtil.verify("secret123", "garbage"));
    }

    @Test void parsesAndRejectsTimes() {
        assertEquals(LocalTime.of(8, 5), Validator.parseTime("8:05"));
        assertEquals(LocalTime.of(20, 0), Validator.parseTime(" 20:00 "));
        assertThrows(ValidationException.class, () -> Validator.parseTime("25:00"));
        assertThrows(ValidationException.class, () -> Validator.parseTime("abc"));
    }

    @Test void validatesUsernameAndPassword() {
        assertEquals("john_1", Validator.username(" john_1 "));
        assertThrows(ValidationException.class, () -> Validator.username("a b"));
        assertThrows(ValidationException.class, () -> Validator.password("123"));
    }
}
