package com.bruburger.tracker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FinanceTrackerApplication {

    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();
        SpringApplication.run(FinanceTrackerApplication.class, args);
    }
}