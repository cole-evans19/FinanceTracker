package com.bruburger.tracker;

import java.time.DayOfWeek;
import java.time.LocalDate;

public record SwapCandidate(
    LocalDate date, DayOfWeek dayOfWeek, ShiftType type,
    double forecastGross, double forecastTipsPerHour, int sampleCount
) {}