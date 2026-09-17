package com.bruburger.tracker;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class calculator {

    private static final ShiftDAO shiftDAO = new ShiftDAO();

    public static void addShift(Shift shift) throws DuplicateShiftException {
        if (shift.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot add a shift with a future date.");
        }
        validateShiftValues(shift.getHours(), shift.getWage(), shift.getCashTips(), shift.getCardTips());

        shiftDAO.insertShift(shift);
    }

    public static void deleteShift(LocalDate date, ShiftType type) {
        shiftDAO.deleteShift(date, type);
    }

    // Core function: display all shifts between two dates (inclusive)
    public static void displayShifts(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        List<Shift> shifts = shiftDAO.findShiftsBetween(startDate, endDate);

        int shiftsWorked = shifts.size();
        double grossPay = 0;
        double totalCardTips = 0;
        double totalCashTips = 0;

        for (Shift shift : shifts) {
            grossPay += shift.getGrossPay();
            totalCashTips += shift.getCashTips();
            totalCardTips += shift.getCardTips();
        }

        long daysInRange = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double weeks = daysInRange / 7.0;

        double avgShiftsPerWeek = shiftsWorked / weeks;
        double avgGrossPayPerWeek = grossPay / weeks;
        double avgTipsPerWeek = (totalCashTips + totalCardTips) / weeks;

        System.out.println("Shifts worked: " + shiftsWorked);
        System.out.println("Gross pay: $" + grossPay);
        System.out.println("Total Card Tips: $" + totalCardTips);
        System.out.println("Total Cash Tips: $" + totalCashTips);
        System.out.println("Average shifts per week: " + avgShiftsPerWeek);
        System.out.println("Average gross pay per week: $" + avgGrossPayPerWeek);
        System.out.println("Average tips per week: $" + avgTipsPerWeek);
    }

    // Shortcut: past month, calculated relative to today
    public static void displayPastMonth() {
        LocalDate today = LocalDate.now();
        LocalDate oneMonthAgo = today.minusMonths(1);
        displayShifts(oneMonthAgo, today);
    }

    // Helper class to accumulate stats per (day, shiftType) combo
    private static class ShiftStats {
        double totalGross = 0;
        double totalTips = 0;
        int count = 0;
    }

    public static void shiftProfitability(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        List<Shift> shifts = shiftDAO.findShiftsBetween(startDate, endDate);
        Map<String, ShiftStats> statsMap = new HashMap<>();

        for (Shift shift : shifts) {
            DayOfWeek day = shift.getDate().getDayOfWeek();
            String key = day + " " + shift.getType();

            ShiftStats stats = statsMap.computeIfAbsent(key, k -> new ShiftStats());
            stats.totalGross += shift.getGrossPay();
            stats.totalTips += shift.getTotalTips();
            stats.count++;
        }

        if (statsMap.isEmpty()) {
            System.out.println("No shifts found in that date range.");
            return;
        }

        // Convert to a sortable list, ranked by average gross pay
        List<Map.Entry<String, ShiftStats>> ranked = new ArrayList<>(statsMap.entrySet());
        ranked.sort(Comparator.comparingDouble(
            (Map.Entry<String, ShiftStats> e) -> e.getValue().totalGross / e.getValue().count
        ).reversed());

        int topCount = Math.min(3, ranked.size());

        System.out.println("Most profitable shifts:");
        for (int i = 0; i < topCount; i++) {
            printRankedEntry(i + 1, ranked.get(i));
        }

        System.out.println();
        System.out.println("Least profitable shifts:");
        for (int i = 0; i < topCount; i++) {
            Map.Entry<String, ShiftStats> entry = ranked.get(ranked.size() - 1 - i);
            printRankedEntry(i + 1, entry);
        }
    }

    private static void printRankedEntry(int rank, Map.Entry<String, ShiftStats> entry) {
        ShiftStats stats = entry.getValue();
        double avgGross = stats.totalGross / stats.count;
        double avgTips = stats.totalTips / stats.count;
        System.out.println(rank + ". " + entry.getKey() +
            " — Avg Gross: $" + String.format("%.2f", avgGross) +
            ", Avg Tips: $" + String.format("%.2f", avgTips));
    }

    private static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
        if (endDate.isAfter(today)) {
            throw new IllegalArgumentException("End date cannot be in the future.");
        }
    }

    private static void validateShiftValues(double hours, double wage, double cashTips, double cardTips) {
        if (hours < 0) {
            throw new IllegalArgumentException("Hours cannot be negative.");
        }
        if (wage < 0) {
            throw new IllegalArgumentException("Wage cannot be negative.");
        }
        if (cashTips < 0) {
            throw new IllegalArgumentException("Cash tips cannot be negative.");
        }
        if (cardTips < 0) {
            throw new IllegalArgumentException("Card tips cannot be negative.");
        }
    }

    public static void begin() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n--- Shift Tracker ---");
            System.out.println("1. Add shift");
            System.out.println("2. Delete shift");
            System.out.println("3. Display past month");
            System.out.println("4. Display custom range");
            System.out.println("5. Shift profitability");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");

            String input = scanner.nextLine().trim();
            int choice;

            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number between 1 and 6.");
                continue;
            }

            switch (choice) {
                case 1:
                    handleAddShift(scanner);
                    break;
                case 2:
                    handleDeleteShift(scanner);
                    break;
                case 3:
                    displayPastMonth();
                    break;
                case 4:
                    handleCustomRange(scanner);
                    break;
                case 5:
                    handleProfitability(scanner);
                    break;
                case 6:
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Please enter a number between 1 and 6.");
            }
        }

        scanner.close();
    }

    private static void handleAddShift(Scanner scanner) {
        try {
            LocalDate date = promptForDate(scanner, "Enter shift date (YYYY-MM-DD): ");

            System.out.print("Enter shift type (MORNING/EVENING): ");
            String typeInput = scanner.nextLine().trim().toUpperCase();
            ShiftType type;
            try {
                type = ShiftType.valueOf(typeInput);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("'" + typeInput + "' is not a valid shift type. Use MORNING or EVENING.");
            }

            System.out.print("Enter hours worked: ");
            double hours = Double.parseDouble(scanner.nextLine().trim());

            System.out.print("Enter hourly wage: ");
            double wage = Double.parseDouble(scanner.nextLine().trim());

            System.out.print("Enter cash tips: ");
            double cashTips = Double.parseDouble(scanner.nextLine().trim());

            System.out.print("Enter card tips: ");
            double cardTips = Double.parseDouble(scanner.nextLine().trim());

            Shift shift = new Shift(date, type, hours, wage, cashTips, cardTips);
            addShift(shift);
            System.out.println("Shift added successfully.");

        } catch (DuplicateShiftException e) {
            System.out.println("Could not add shift: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }

    private static void handleDeleteShift(Scanner scanner) {
        try {
            LocalDate date = promptForDate(scanner, "Enter shift date to delete (YYYY-MM-DD): ");

            System.out.print("Enter shift type (MORNING/EVENING): ");
            ShiftType type = ShiftType.valueOf(scanner.nextLine().trim().toUpperCase());

            deleteShift(date, type);
            System.out.println("Delete request processed.");

        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }

    private static void handleCustomRange(Scanner scanner) {
        try {
            LocalDate start = promptForDate(scanner, "Enter start date (YYYY-MM-DD): ");
            LocalDate end = promptForDate(scanner, "Enter end date (YYYY-MM-DD): ");
            displayShifts(start, end);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }

    private static void handleProfitability(Scanner scanner) {
        try {
            LocalDate start = promptForDate(scanner, "Enter start date (YYYY-MM-DD): ");
            LocalDate end = promptForDate(scanner, "Enter end date (YYYY-MM-DD): ");
            shiftProfitability(start, end);
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }

    private static LocalDate promptForDate(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return LocalDate.parse(scanner.nextLine().trim());
    }

    public static void main(String args[]) {
        DatabaseManager.initializeDatabase();
        begin();
    }
}