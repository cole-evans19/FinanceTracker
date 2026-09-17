package com.bruburger.tracker;

import org.springframework.http.HttpStatus;
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

    @GetMapping
    public List<Shift> getShifts(
            @RequestParam("start") String start,
            @RequestParam("end") String end) {

        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        return shiftDAO.findShiftsBetween(startDate, endDate);
    }

    @PostMapping
    public ResponseEntityWrapper addShift(@RequestBody Shift shift) {
        try {
            shiftDAO.insertShift(shift);
            return new ResponseEntityWrapper("Shift added successfully.");
        } catch (DuplicateShiftException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @DeleteMapping
    public ResponseEntityWrapper deleteShift(
            @RequestParam("date") String date,
            @RequestParam("type") String type) {

        LocalDate parsedDate = LocalDate.parse(date);
        ShiftType parsedType = ShiftType.valueOf(type.toUpperCase());

        shiftDAO.deleteShift(parsedDate, parsedType);
        return new ResponseEntityWrapper("Delete request processed.");
    }

    // small helper record for simple JSON success messages
    public record ResponseEntityWrapper(String message) {}
}