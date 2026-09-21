package com.bruburger.tracker;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JobDAO {

    public Job insertJob(int userId, String name) {
        String sql = "INSERT INTO jobs (user_id, name) VALUES (?, ?) RETURNING id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Job(rs.getInt("id"), userId, name);
                }
            }

        } catch (SQLException e) {
            System.out.println("Error inserting job: " + e.getMessage());
        }

        return null;
    }

    public List<Job> findJobsByUser(int userId) {
        List<Job> jobs = new ArrayList<>();
        String sql = "SELECT * FROM jobs WHERE user_id = ? ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    jobs.add(new Job(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("name")
                    ));
                }
            }

        } catch (SQLException e) {
            System.out.println("Error fetching jobs: " + e.getMessage());
        }

        return jobs;
    }

    public void deleteJob(int userId, int jobId) {
        String sql = "DELETE FROM jobs WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, jobId);
            stmt.setInt(2, userId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error deleting job: " + e.getMessage());
        }
    }
}