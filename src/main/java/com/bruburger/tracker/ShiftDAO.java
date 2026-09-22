package com.bruburger.tracker;

import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository 
public class ShiftDAO {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void insertShift(int userId, int jobId, Shift shift) throws DuplicateShiftException {
        String sql = """
            INSERT INTO shifts (user_id, job_id, date, type, hours, wage, cash_tips, card_tips, role, custom_attributes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            
            stmt.setInt(1, userId);
            stmt.setInt(2, jobId);
            stmt.setDate(3, java.sql.Date.valueOf(shift.getDate()));
            stmt.setString(4, shift.getType().toString());
            stmt.setDouble(5, shift.getHours());
            stmt.setDouble(6, shift.getWage());
            stmt.setDouble(7, shift.getCashTips());
            stmt.setDouble(8, shift.getCardTips());
            stmt.setString(9, shift.getRole());

            String customAttributesJson = shift.getCustomAttributes() != null
                ? objectMapper.writeValueAsString(shift.getCustomAttributes())
                : null;
            stmt.setString(10, customAttributesJson);

            stmt.executeUpdate();

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                throw new DuplicateShiftException(
                    "A shift already exists for " + shift.getDate() + " (" + shift.getType() + ")"
                );
            }
            System.out.println("Error inserting shift: " + e.getMessage());
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.out.println("Error serializing custom attributes: " + e.getMessage());
        }
    }

    public void deleteShift(int userId, int jobId, LocalDate date, ShiftType type) {
        String sql = "DELETE FROM shifts WHERE user_id = ? AND job_id = ? AND date = ? AND type = ?";

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, jobId);
            stmt.setDate(3, java.sql.Date.valueOf(date));
            stmt.setString(4, type.toString());

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error deleting shift: " + e.getMessage());
        }
    }

    public List<Shift> findShiftsBetween(int userId, int jobId, LocalDate startDate, LocalDate endDate) {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT * FROM shifts WHERE user_id = ? AND job_id = ? AND date BETWEEN ? AND ? ORDER BY date";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, jobId);
            stmt.setDate(3, java.sql.Date.valueOf(startDate));
            stmt.setDate(4, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    shifts.add(mapRowToShift(rs));
                }
            }

        } catch (SQLException e) {
            System.out.println("Error fetching shifts: " + e.getMessage());
        }

        return shifts;
    }

    private Shift mapRowToShift(ResultSet rs) throws SQLException {
        Shift shift = new Shift(
            rs.getDate("date").toLocalDate(),
            ShiftType.valueOf(rs.getString("type")),
            rs.getDouble("hours"),
            rs.getDouble("wage"),
            rs.getDouble("cash_tips"),
            rs.getDouble("card_tips"),
            rs.getString("role")
        );

        String customAttributesJson = rs.getString("custom_attributes");
        if (customAttributesJson != null) {
            try {
                Map<String, String> customAttributes = objectMapper.readValue(
                    customAttributesJson,
                    new TypeReference<Map<String, String>>() {}
                );
                shift.setCustomAttributes(customAttributes);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                System.out.println("Error parsing custom attributes: " + e.getMessage());
            }
        }
        return shift;
    }
}
