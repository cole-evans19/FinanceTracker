package com.bruburger.tracker;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftDAO shiftDAO;

    public ShiftController(ShiftDAO shiftDAO) {
        this.shiftDAO = shiftDAO;
    }

    private int getUserId(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getId();
    }

    @GetMapping
    public List<Shift> getShifts(
            Authentication authentication,
            @RequestParam("jobId") int jobId,
            @RequestParam("start") String start,
            @RequestParam("end") String end) {

        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        int userId = getUserId(authentication);
        return shiftDAO.findShiftsBetween(userId, jobId, startDate, endDate);
    }

    @PostMapping
    public ResponseEntityWrapper addShift(
            Authentication authentication, 
            @RequestParam("jobId") int jobId,
            @RequestBody Shift shift) {

        int userId = getUserId(authentication);
        try {
            shiftDAO.insertShift(userId, jobId, shift);
            return new ResponseEntityWrapper("Shift added successfully.");
        } catch (DuplicateShiftException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntityWrapper deleteShift(
            Authentication authentication,
            @RequestParam("jobId") int jobId, 
            @RequestParam("date") String date,
            @RequestParam("type") String type) {

        int userId = getUserId(authentication);
        shiftDAO.deleteShift(userId, jobId, LocalDate.parse(date), ShiftType.valueOf(type.toUpperCase()));
        return new ResponseEntityWrapper("Delete request processed.");
    }

    @GetMapping("/summary")
    public ShiftSummary getSummary(
            Authentication authentication,
            @RequestParam("jobId") int jobId,
            @RequestParam("start") String start,
            @RequestParam("end") String end) {

        int userId = getUserId(authentication);
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        List<Shift> shifts = shiftDAO.findShiftsBetween(userId, jobId, startDate, endDate);

        int shiftsWorked = shifts.size();
        double grossPay = 0;
        double totalCashTips = 0;
        double totalCardTips = 0;

        for (Shift shift : shifts) {
            grossPay += shift.getGrossPay();
            totalCashTips += shift.getCashTips();
            totalCardTips += shift.getCardTips();
        }

        long daysInRange = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double weeks = daysInRange / 7.0;

        return new ShiftSummary(
            shiftsWorked,
            grossPay,
            totalCashTips,
            totalCardTips,
            shiftsWorked / weeks,
            grossPay / weeks,
            (totalCashTips + totalCardTips) / weeks
        );
    }

    @GetMapping("/profitability")
    public ProfitabilityResponse getProfitability(
            Authentication authentication,  
            @RequestParam("jobId") int jobId,      
            @RequestParam("start") String start,
            @RequestParam("end") String end) {

        int userId = getUserId(authentication);
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        List<Shift> shifts = shiftDAO.findShiftsBetween(userId, jobId, startDate, endDate);

        java.util.Map<String, double[]> statsMap = new java.util.HashMap<>();
        // [0] = totalGross, [1] = totalTips, [2] = count

        for (Shift shift : shifts) {
            String key = shift.getDate().getDayOfWeek() + " " + shift.getType();
            double[] stats = statsMap.computeIfAbsent(key, k -> new double[3]);
            stats[0] += shift.getGrossPay();
            stats[1] += shift.getTotalTips();
            stats[2] += 1;
        }

        List<ProfitabilityEntry> ranked = new java.util.ArrayList<>();
        for (var entry : statsMap.entrySet()) {
            String[] parts = entry.getKey().split(" ");
            double[] stats = entry.getValue();
            int count = (int) stats[2];
            ranked.add(new ProfitabilityEntry(
                parts[0], parts[1],
                stats[0] / count,
                stats[1] / count,
                count
            ));
        }

        ranked.sort((a, b) -> Double.compare(b.avgGross(), a.avgGross()));

        int topCount = Math.min(3, ranked.size());
        List<ProfitabilityEntry> most = ranked.subList(0, topCount);
        List<ProfitabilityEntry> least = ranked.subList(Math.max(0, ranked.size() - topCount), ranked.size());

        return new ProfitabilityResponse(most, least);
    }

    // small helper record for simple JSON success messages
    public record ResponseEntityWrapper(String message) {}
}