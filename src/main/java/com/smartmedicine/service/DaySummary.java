package com.smartmedicine.service;

import java.time.LocalDate;

public record DaySummary(LocalDate date, int taken, int missed, int pending) { }
