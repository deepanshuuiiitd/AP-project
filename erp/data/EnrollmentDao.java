package edu.univ.erp.data;

import edu.univ.erp.domain.Enrollment;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnrollmentDao {
    private final DataSource ds = DataSourceProvider.erpDataSource();

    public List<Enrollment> findByStudentId(int studentId) {
        String sql = "SELECT enrollment_id, student_id, section_id, enrollment_date FROM enrollments WHERE student_id = ?";
        List<Enrollment> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("enrollment_date");
                    out.add(new Enrollment(
                            rs.getInt("enrollment_id"),
                            rs.getInt("student_id"),
                            rs.getInt("section_id"),
                            sqlDate // pass java.sql.Date (can be null)
                    ));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    public List<Enrollment> findBySectionId(int sectionId) {
        String sql = "SELECT enrollment_id, student_id, section_id, enrollment_date FROM enrollments WHERE section_id = ?";
        List<Enrollment> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("enrollment_date");
                    out.add(new Enrollment(
                            rs.getInt("enrollment_id"),
                            rs.getInt("student_id"),
                            rs.getInt("section_id"),
                            sqlDate
                    ));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    public Optional<Enrollment> findByStudentAndSection(int studentId, int sectionId) {
        String sql = "SELECT enrollment_id, student_id, section_id, enrollment_date FROM enrollments WHERE student_id = ? AND section_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Date sqlDate = rs.getDate("enrollment_date");
                    return Optional.of(new Enrollment(
                            rs.getInt("enrollment_id"),
                            rs.getInt("student_id"),
                            rs.getInt("section_id"),
                            sqlDate
                    ));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public int insert(int studentId, int sectionId, java.time.LocalDate enrollmentDate) {
        String sql = "INSERT INTO enrollments (student_id, section_id, enrollment_date) VALUES (?, ?, ?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, studentId);
            ps.setInt(2, sectionId);
            ps.setDate(3, java.sql.Date.valueOf(enrollmentDate));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            throw new RuntimeException("Insert succeeded but no key returned");
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void deleteById(int enrollmentId) {
        String sql = "DELETE FROM enrollments WHERE enrollment_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public int countBySectionId(int sectionId) {
        String sql = "SELECT COUNT(*) AS cnt FROM enrollments WHERE section_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt");
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return 0;
    }
}
