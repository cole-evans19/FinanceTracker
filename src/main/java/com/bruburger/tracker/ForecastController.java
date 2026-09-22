package com.bruburger.tracker;

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

    private int getUserId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }

    @PostMapping
    public ScheduleSwapAnalysis forecastSchedule(
            Authentication authentication,
            @RequestParam("jobId") int jobId,
            @RequestBody List<ScheduleEntry> schedule) {

        int userId = getUserId(authentication);
        return forecastService.analyzeScheduleSwaps(userId, jobId, schedule, LocalDate.now());
    }
}

