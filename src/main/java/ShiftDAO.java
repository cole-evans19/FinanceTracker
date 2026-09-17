import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ShiftDAO {

    public void insertShift(Shift shift) throws DuplicateShiftException {
        String sql = """
            INSERT INTO shifts (date, type, hours, wage, cash_tips, card_tips)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, shift.getDate().toString());
            stmt.setString(2, shift.getType().toString());
            stmt.setDouble(3, shift.getHours());
            stmt.setDouble(4, shift.getWage());
            stmt.setDouble(5, shift.getCashTips());
            stmt.setDouble(6, shift.getCardTips());

            stmt.executeUpdate();

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                throw new DuplicateShiftException(
                    "A shift already exists for " + shift.getDate() + " (" + shift.getType() + ")"
                );
            }
            System.out.println("Error inserting shift: " + e.getMessage());
        }
    }

    public void deleteShift(LocalDate date, ShiftType type) {
        String sql = "DELETE FROM shifts WHERE date = ? AND type = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, date.toString());
            stmt.setString(2, type.toString());

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error deleting shift: " + e.getMessage());
        }
    }

    public List<Shift> findShiftsBetween(LocalDate startDate, LocalDate endDate) {
        List<Shift> shifts = new ArrayList<>();
        String sql = "SELECT * FROM shifts WHERE date BETWEEN ? AND ? ORDER BY date";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, startDate.toString());
            stmt.setString(2, endDate.toString());

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
        return new Shift(
            LocalDate.parse(rs.getString("date")),
            ShiftType.valueOf(rs.getString("type")),
            rs.getDouble("hours"),
            rs.getDouble("wage"),
            rs.getDouble("cash_tips"),
            rs.getDouble("card_tips")
        );
    }
}
