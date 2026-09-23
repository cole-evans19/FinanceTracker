package com.financetracker.app;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ShiftForecastService {

    // Tunable blending weight: how much to trust last year's same-season data
    // vs. recent weeks. 0 = ignore history entirely, 1 = ignore recent trend entirely.
    // Kept low-ish by default since we don't yet have enough years of data
    // to fully trust a seasonal signal.
    public static final double DEFAULT_ALPHA = 0.45;

    // How many weeks back "recent" looks, for the same day-of-week + shift type.
    public static final int ROLLING_WEEKS = 6;

    // How many days on either side of "the same date last year" count as
    // "same season" when gathering last-year data for a slot.
    public static final int SEASON_WINDOW_DAYS = 21;

    private final ShiftDAO shiftDAO;

    public ShiftForecastService(ShiftDAO shiftDAO) {
        this.shiftDAO = shiftDAO;
    }

    /**
     * Forecasts expected profitability for a given (day-of-week, shift-type) slot.
     *
     * @param targetDate the future date being forecasted (used to find "same season" last year)
     * @param asOfDate   the date the forecast is being made from (used for the recent-weeks window)
     */
    public ForecastResult forecastSlot(
            int userId, int jobId, DayOfWeek dayOfWeek, ShiftType type,
            LocalDate targetDate, LocalDate asOfDate) throws InsufficientForecastDataException {

        return forecastSlot(userId, jobId, dayOfWeek, type, targetDate, asOfDate, DEFAULT_ALPHA);
    }

    public ForecastResult forecastSlot(
            int userId, int jobId, DayOfWeek dayOfWeek, ShiftType type,
            LocalDate targetDate, LocalDate asOfDate, double alpha) throws InsufficientForecastDataException {

        List<Shift> recentShifts = fetchMatching(userId, jobId, dayOfWeek, type,
            asOfDate.minusWeeks(ROLLING_WEEKS), asOfDate.minusDays(1));

        LocalDate lastYearTarget = targetDate.minusYears(1);
        List<Shift> lastYearShifts = fetchMatching(userId, jobId, dayOfWeek, type,
            lastYearTarget.minusDays(SEASON_WINDOW_DAYS), lastYearTarget.plusDays(SEASON_WINDOW_DAYS));

        double recentAvgGross = average(recentShifts, Shift::getGrossPay);
        double recentAvgTipsPerHour = averageTipsPerHour(recentShifts);
        double lastYearAvgGross = average(lastYearShifts, Shift::getGrossPay);
        double lastYearAvgTipsPerHour = averageTipsPerHour(lastYearShifts);

        int recentCount = recentShifts.size();
        int lastYearCount = lastYearShifts.size();

        if (recentCount == 0 && lastYearCount == 0) {
            throw new InsufficientForecastDataException(
                "No historical data for " + dayOfWeek + " " + type + " — cannot forecast yet."
            );
        }

        double forecastGross;
        double forecastTipsPerHour;
        double alphaUsed;
        boolean usedFallback;

        if (lastYearCount == 0) {
            // No same-season data from last year yet — fall back to recent trend only.
            forecastGross = recentAvgGross;
            forecastTipsPerHour = recentAvgTipsPerHour;
            alphaUsed = 0.0;
            usedFallback = true;
        } else if (recentCount == 0) {
            // No recent shifts of this type yet (e.g. a slot you haven't worked in a while) —
            // fall back to last year's same-season average only.
            forecastGross = lastYearAvgGross;
            forecastTipsPerHour = lastYearAvgTipsPerHour;
            alphaUsed = 1.0;
            usedFallback = true;
        } else {
            forecastGross = (alpha * lastYearAvgGross) + ((1 - alpha) * recentAvgGross);
            forecastTipsPerHour = (alpha * lastYearAvgTipsPerHour) + ((1 - alpha) * recentAvgTipsPerHour);
            alphaUsed = alpha;
            usedFallback = false;
        }

        return new ForecastResult(
            dayOfWeek, type, forecastGross, forecastTipsPerHour, alphaUsed,
            lastYearAvgGross, lastYearCount, recentAvgGross, recentCount, usedFallback
        );
    }

    /**
     * Scores an upcoming schedule, ranking entries by forecast gross pay and
     * flagging the weakest ones as candidates to consider dropping.
     */

    private List<Shift> fetchMatching(int userId, int jobId, DayOfWeek dayOfWeek, ShiftType type,
                                        LocalDate start, LocalDate end) {
        return shiftDAO.findShiftsBetween(userId, jobId, start, end).stream()
            .filter(s -> s.getDate().getDayOfWeek() == dayOfWeek && s.getType() == type)
            .toList();
    }

    private double average(List<Shift> shifts, java.util.function.ToDoubleFunction<Shift> extractor) {
        return shifts.isEmpty() ? 0.0 : shifts.stream().mapToDouble(extractor).average().orElse(0.0);
    }

    private double averageTipsPerHour(List<Shift> shifts) {
        return shifts.stream()
            .filter(s -> s.getHours() > 0)
            .mapToDouble(s -> s.getTotalTips() / s.getHours())
            .average()
            .orElse(0.0);
    }

    public static final int WINDOW_DAYS = 14;
    public static final int TOP_CANDIDATES_CONSIDERED = 2;

    public ScheduleSwapAnalysis analyzeScheduleSwaps(
            int userId, int jobId, List<ScheduleEntry> scheduled, LocalDate asOfDate) {

        LocalDate windowStart = asOfDate.plusDays(1);
        LocalDate windowEnd = asOfDate.plusDays(WINDOW_DAYS);

        java.util.Set<ScheduleEntry> scheduledSet = new java.util.HashSet<>(scheduled);

        // Forecast each already-scheduled entry; skip any with insufficient data (nothing to judge).
        List<SwapCandidate> scheduledForecasts = new ArrayList<>();
        for (ScheduleEntry entry : scheduled) {
            try {
                ForecastResult f = forecastSlot(userId, jobId, entry.date().getDayOfWeek(), entry.type(), entry.date(), asOfDate);
                scheduledForecasts.add(new SwapCandidate(
                    entry.date(), entry.date().getDayOfWeek(), entry.type(),
                    f.forecastGross(), f.forecastTipsPerHour(), f.lastYearSampleCount() + f.recentSampleCount()
                ));
            } catch (InsufficientForecastDataException ignored) {
                // Can't judge this shift's profitability — exclude it from swap consideration.
            }
        }

        // Build the universe of NOT-yet-scheduled (date, type) candidates across the window.
        List<SwapCandidate> unscheduledCandidates = new ArrayList<>();
        for (LocalDate date = windowStart; !date.isAfter(windowEnd); date = date.plusDays(1)) {
            for (ShiftType type : ShiftType.values()) {
                ScheduleEntry candidateEntry = new ScheduleEntry(date, type);
                if (scheduledSet.contains(candidateEntry)) continue;

                try {
                    ForecastResult f = forecastSlot(userId, jobId, date.getDayOfWeek(), type, date, asOfDate);
                    unscheduledCandidates.add(new SwapCandidate(
                        date, date.getDayOfWeek(), type,
                        f.forecastGross(), f.forecastTipsPerHour(), f.lastYearSampleCount() + f.recentSampleCount()
                    ));
                } catch (InsufficientForecastDataException ignored) {
                    // No history for this slot — not a viable recommendation either way.
                }
            }
        }

        List<SwapCandidate> weakestScheduled = scheduledForecasts.stream()
            .sorted(Comparator.comparingDouble(SwapCandidate::forecastGross))
            .limit(TOP_CANDIDATES_CONSIDERED)
            .toList();

        List<SwapCandidate> bestUnscheduled = unscheduledCandidates.stream()
            .sorted(Comparator.comparingDouble(SwapCandidate::forecastGross).reversed())
            .limit(TOP_CANDIDATES_CONSIDERED)
            .toList();

        // Match weakest-scheduled against best-unscheduled, one-to-one, only where it's a genuine improvement.
        List<SwapSuggestion> suggestions = new ArrayList<>();
        java.util.Set<SwapCandidate> usedOpportunities = new java.util.HashSet<>();

        for (SwapCandidate weak : weakestScheduled) {
            bestUnscheduled.stream()
                .filter(opportunity -> !usedOpportunities.contains(opportunity))
                .filter(opportunity -> opportunity.forecastGross() > weak.forecastGross())
                .findFirst()
                .ifPresent(opportunity -> {
                    usedOpportunities.add(opportunity);
                    suggestions.add(new SwapSuggestion(weak, opportunity, opportunity.forecastGross() - weak.forecastGross()));
                });
        }

        return new ScheduleSwapAnalysis(weakestScheduled, bestUnscheduled, suggestions, suggestions.isEmpty());
    }
}