package com.bruburger.tracker;

public record ProfitabilityEntry(
    String dayOfWeek,
    String shiftType,
    String role,
    double avgGross,
    double avgTips,
    int count
) {}