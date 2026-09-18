package com.bruburger.tracker;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;


//One time use class, this was used after implementing SQLite
//to transfer the data I had in a .txt to my SQL so I didn't
//have to recreate it. Could be useful later on. 

//Sidenote, reused this again after I switched ShiftDOA to point
//at supabase instead of SQLite. 

public class DataMigrator {

    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();
        ShiftDAO shiftDAO = new ShiftDAO();

        int migrated = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader("shiftDatabase.txt"))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] fields = line.split(",");

                LocalDate date = LocalDate.parse(fields[0]);
                ShiftType type = ShiftType.valueOf(fields[1]);
                double hours = Double.parseDouble(fields[2]);
                double wage = Double.parseDouble(fields[3]);
                double cashTips = Double.parseDouble(fields[4]);
                double cardTips = Double.parseDouble(fields[5]);

                Shift shift = new Shift(date, type, hours, wage, cashTips, cardTips);

                try {
                    shiftDAO.insertShift(shift);
                    migrated++;
                } catch (DuplicateShiftException e) {
                    System.out.println("Skipping duplicate: " + date + " " + type);
                    skipped++;
                }
            }

        } catch (IOException e) {
            System.out.println("Could not read shiftDatabase.txt: " + e.getMessage());
            return;
        }

        System.out.println("Migration complete. Migrated: " + migrated + ", Skipped: " + skipped);
    }
}
