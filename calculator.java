import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

    public static void addShift(Shift shift) throws DuplicateShiftException {
        if (shift.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot add a shift with a future date.");
        }
        validateShiftValues(shift.getHours(),shift.getWage(), shift.getCashTips(), shift.getCardTips());
        if (isDuplicateShift(shift)) {
            throw new DuplicateShiftException(
                "A shift already exists for " + shift.getDate() + " (" + shift.getType() + ")"
            );
        }

        try (FileWriter writer = new FileWriter("shiftDatabase.txt", true)) {
            String line = shift.getDate() + "," +
                          shift.getType() + "," +
                          shift.getHours() + "," +
                          shift.getWage() + "," +
                          shift.getCashTips() + "," +
                          shift.getCardTips() + "\n";
            writer.write(line);
        } catch (IOException e) {
            System.out.println("Error writing shift to file: " + e.getMessage());
        }
    }

    public static void deleteShift(LocalDate date, ShiftType type) {
        List<String> remainingLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("shiftDatabase.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                String existingDate = fields[0];
                String existingType = fields[1];

                boolean isMatch = existingDate.equals(date.toString()) &&
                                   existingType.equals(type.toString());

                if (!isMatch) {
                    remainingLines.add(line);
                }
            }
        } catch (IOException e) {
            // File doesn't exist yet — nothing to delete
            return;
        }

        try (FileWriter writer = new FileWriter("shiftDatabase.txt", false)) {
            for (String line : remainingLines) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            System.out.println("Error rewriting shift database: " + e.getMessage());
        }
    }

    private static boolean isDuplicateShift(Shift shift) {
        try (BufferedReader reader = new BufferedReader(new FileReader("shiftDatabase.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                String existingDate = fields[0];
                String existingType = fields[1];

                if (existingDate.equals(shift.getDate().toString()) &&
                    existingType.equals(shift.getType().toString())) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    // Core function: display all shifts between two dates (inclusive)
    public static void displayShifts(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        int shiftsWorked = 0;
        double grossPay = 0;
        double totalCardTips = 0;
        double totalCashTips = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader("shiftDatabase.txt"))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                LocalDate shiftDate = LocalDate.parse(fields[0]);

                if (!shiftDate.isBefore(startDate) && !shiftDate.isAfter(endDate)) {
                    double hours = Double.parseDouble(fields[2]);
                    double wage = Double.parseDouble(fields[3]);
                    double cashTips = Double.parseDouble(fields[4]);
                    double cardTips = Double.parseDouble(fields[5]);

                    shiftsWorked++;
                    grossPay += (hours * wage) + cashTips + cardTips;
                    totalCashTips += cashTips;
                    totalCardTips += cardTips;
                }
            }
        } catch (IOException e) {
            System.out.println("No shift data found.");
            return;
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
        Map<String, ShiftStats> statsMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("shiftDatabase.txt"))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                LocalDate shiftDate = LocalDate.parse(fields[0]);
                String type = fields[1];

                if (!shiftDate.isBefore(startDate) && !shiftDate.isAfter(endDate)) {
                    double hours = Double.parseDouble(fields[2]);
                    double wage = Double.parseDouble(fields[3]);
                    double cashTips = Double.parseDouble(fields[4]);
                    double cardTips = Double.parseDouble(fields[5]);

                    double gross = (hours * wage) + cashTips + cardTips;
                    double tips = cashTips + cardTips;

                    DayOfWeek day = shiftDate.getDayOfWeek();
                    String key = day + " " + type;

                    ShiftStats stats = statsMap.computeIfAbsent(key, k -> new ShiftStats());
                    stats.totalGross += gross;
                    stats.totalTips += tips;
                    stats.count++;
                }
            }
        } catch (IOException e) {
            System.out.println("No shift data found.");
            return;
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
        begin();
    } 
}