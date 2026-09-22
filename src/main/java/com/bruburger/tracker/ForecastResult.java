package com.bruburger.tracker;

import java.time.DayOfWeek;

public record ForecastResult(
    DayOfWeek dayOfWeek,
    ShiftType type,
    double forecastGross,
    double forecastTipsPerHour,
    double alphaUsed,
    double lastYearAvgGross,
    int lastYearSampleCount,
    double recentAvgGross,
    int recentSampleCount,
    boolean usedFallback
) {}
