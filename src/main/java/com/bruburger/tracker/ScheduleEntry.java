package com.bruburger.tracker;

import java.time.LocalDate;

public record ScheduleEntry(LocalDate date, ShiftType type) {}