package edu.univ.erp.data;

import edu.univ.erp.domain.Student;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class StudentDao {
    private final DataSource ds = DataSourceProvider.erpDataSource();

    public Optional<Student> findByUserId(int userId) {
        String sql = "SELECT user_id, roll_no, program, year FROM students WHERE user_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Student(
                            rs.getInt("user_id"),
                            rs.getString("roll_no"),
                            rs.getString("program"),
                            rs.getInt("year")
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading student", e);
        }
        return Optional.empty();
    }
}
