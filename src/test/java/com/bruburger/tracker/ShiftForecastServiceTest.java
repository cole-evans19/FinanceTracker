package com.bruburger.tracker;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShiftForecastServiceTest {

    private Shift shift(LocalDate date, ShiftType type, double hours, double wage, double cashTips, double cardTips) {
        return new Shift(date, type, hours, wage, cashTips, cardTips, "server");
    }

    @Test
    void forecastSlot_blendsLastYearAndRecentData() throws InsufficientForecastDataException {
        ShiftDAO mockDao = mock(ShiftDAO.class);
        ShiftForecastService service = new ShiftForecastService(mockDao);

        LocalDate asOfDate = LocalDate.of(2026, 9, 15);
        LocalDate targetDate = LocalDate.of(2026, 9, 22); // a Tuesday

        // Recent weeks: two $100 gross Tuesday EVENING shifts
        List<Shift> recent = List.of(
            shift(LocalDate.of(2026, 9, 1), ShiftType.EVENING, 7, 15, 40, 45),
            shift(LocalDate.of(2026, 9, 8), ShiftType.EVENING, 7, 15, 40, 45)
        );
        // Last year, same season: two $80 gross Tuesday EVENING shifts
        List<Shift> lastYear = List.of(
            shift(LocalDate.of(2025, 9, 2), ShiftType.EVENING, 7, 15, 30, 35),
            shift(LocalDate.of(2025, 9, 9), ShiftType.EVENING, 7, 15, 30, 35)
        );

        when(mockDao.findShiftsBetween(eq(1), eq(1), any(), any()))
            .thenReturn(recent) // first call: recent window
            .thenReturn(lastYear); // second call: last-year window

        ForecastResult result = service.forecastSlot(1, 1, DayOfWeek.TUESDAY, ShiftType.EVENING, targetDate, asOfDate);

        assertThat(result.usedFallback()).isFalse();
        assertThat(result.recentSampleCount()).isEqualTo(2);
        assertThat(result.lastYearSampleCount()).isEqualTo(2);
        // forecast should sit between the two averages (blended, not equal to either)
        assertThat(result.forecastGross()).isBetween(result.lastYearAvgGross(), result.recentAvgGross());
    }

    @Test
    void forecastSlot_fallsBackToRecentWhenNoLastYearData() throws InsufficientForecastDataException {
        ShiftDAO mockDao = mock(ShiftDAO.class);
        ShiftForecastService service = new ShiftForecastService(mockDao);

        List<Shift> recent = List.of(
            shift(LocalDate.of(2026, 8, 31), ShiftType.MORNING, 5, 15, 10, 15) // a Monday
        );

        when(mockDao.findShiftsBetween(eq(1), eq(1), any(), any()))
            .thenReturn(recent)
            .thenReturn(List.of()); // no last-year data

        ForecastResult result = service.forecastSlot(
            1, 1, DayOfWeek.MONDAY, ShiftType.MORNING,
            LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 8)
        );

        assertThat(result.usedFallback()).isTrue();
        assertThat(result.alphaUsed()).isEqualTo(0.0);
        assertThat(result.forecastGross()).isEqualTo(result.recentAvgGross());
    }

    @Test
    void forecastSlot_throwsWhenNoDataAtAll() {
        ShiftDAO mockDao = mock(ShiftDAO.class);
        ShiftForecastService service = new ShiftForecastService(mockDao);

        when(mockDao.findShiftsBetween(eq(1), eq(1), any(), any())).thenReturn(List.of());

        assertThrows(InsufficientForecastDataException.class, () ->
            service.forecastSlot(1, 1, DayOfWeek.SUNDAY, ShiftType.NIGHT,
                LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 8))
        );
    }
}