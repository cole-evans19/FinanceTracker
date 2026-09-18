package com.bruburger.tracker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:5432/postgres";
    private static final String DB_USER = "postgres.sgjfjjgstkoufgdfmcpm";
    private static final String DB_PASSWORD = System.getenv("SUPABASE_DB_PASSWORD");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static void initializeDatabase() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS shifts (
                id SERIAL PRIMARY KEY,
                date DATE NOT NULL,
                type TEXT NOT NULL,
                hours DOUBLE PRECISION NOT NULL,
                wage DOUBLE PRECISION NOT NULL,
                cash_tips DOUBLE PRECISION NOT NULL,
                card_tips DOUBLE PRECISION NOT NULL,
                UNIQUE(date, type)
            );
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableSQL);

        } catch (SQLException e) {
            System.out.println("Error initializing database: " + e.getMessage());
        }
    }
}