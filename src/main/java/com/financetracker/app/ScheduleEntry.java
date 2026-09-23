package com.financetracker.app;

import java.time.LocalDate;

public record ScheduleEntry(LocalDate date, ShiftType type) {}