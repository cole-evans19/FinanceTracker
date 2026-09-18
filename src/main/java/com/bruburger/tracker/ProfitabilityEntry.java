package com.bruburger.tracker;

public record ProfitabilityEntry(
    String dayOfWeek,
    String shiftType,
    double avgGross,
    double avgTips,
    int count
) {}