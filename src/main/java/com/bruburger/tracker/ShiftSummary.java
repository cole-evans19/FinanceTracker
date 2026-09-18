package com.bruburger.tracker;

public record ShiftSummary(
    int shiftsWorked,
    double grossPay,
    double totalCashTips,
    double totalCardTips,
    double avgShiftsPerWeek,
    double avgGrossPayPerWeek,
    double avgTipsPerWeek
) {}