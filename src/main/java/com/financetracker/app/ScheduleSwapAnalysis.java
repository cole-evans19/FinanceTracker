package com.financetracker.app;

import java.util.List;

public record ScheduleSwapAnalysis(
    List<SwapCandidate> weakestScheduled,
    List<SwapCandidate> bestUnscheduled,
    List<SwapSuggestion> suggestions,
    boolean recommendKeepCurrentSchedule
) {}