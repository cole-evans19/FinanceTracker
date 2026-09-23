package com.financetracker.app;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shifts/forecast")
public class ForecastController {

    private final ShiftForecastService forecastService;

    public ForecastController(ShiftForecastService forecastService) {
        this.forecastService = forecastService;
    }
    
    @PostMapping
    public ScheduleSwapAnalysis forecastSchedule(
            Authentication authentication,
            @RequestParam("jobId") int jobId,
            @RequestBody List<ScheduleEntry> schedule) {

        int userId = SecurityUtils.getUserId(authentication);
        return forecastService.analyzeScheduleSwaps(userId, jobId, schedule, LocalDate.now());
    }
}

