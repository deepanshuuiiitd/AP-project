package edu.univ.erp.data;

import edu.univ.erp.domain.Grade;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

/**
 * Grade DAO - basic find / upsert / delete for grades table.
 * Assumes table `grades (grade_id INT AUTO_INCREMENT PK, enrollment_id INT, grade VARCHAR(5))`
 */
public class GradeDao {
    private final DataSource ds = DataSourceProvider.erpDataSource();

    public Optional<Grade> findByEnrollmentId(int enrollmentId) {
        String sql = "SELECT grade_id, enrollment_id, grade FROM grades WHERE enrollment_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Grade g = new Grade(rs.getInt("grade_id"), rs.getInt("enrollment_id"), rs.getString("grade"));
                    return Optional.of(g);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    /**
     * Upsert grade by enrollment id: update if exists, otherwise insert.
     * Returns true if a row was created/updated successfully.
     */
    public boolean upsertByEnrollmentId(int enrollmentId, String grade) {
        // Normalize grade string
        String gradeVal = grade == null ? null : grade.trim();
        // check existing
        Optional<Grade> existing = findByEnrollmentId(enrollmentId);
        if (existing.isPresent()) {
            // if gradeVal is empty/null, delete row; else update
            if (gradeVal == null || gradeVal.isEmpty()) {
                deleteByEnrollmentId(enrollmentId);
                return true;
            } else {
                String sql = "UPDATE grades SET grade = ? WHERE enrollment_id = ?";
                try (Connection c = ds.getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, gradeVal);
                    ps.setInt(2, enrollmentId);
                    ps.executeUpdate();
                    return true;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            // insert new if not blank
            if (gradeVal == null || gradeVal.isEmpty()) {
                // nothing to insert
                return false;
            }
            String sql = "INSERT INTO grades (enrollment_id, grade) VALUES (?, ?)";
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, enrollmentId);
                ps.setString(2, gradeVal);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void deleteByEnrollmentId(int enrollmentId) {
        String sql = "DELETE FROM grades WHERE enrollment_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
