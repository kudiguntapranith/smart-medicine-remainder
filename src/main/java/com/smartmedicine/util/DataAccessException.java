package com.smartmedicine.util;

/** Wraps SQLException so higher layers do not depend on JDBC types. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) { super(message, cause); }
}
