package edu.univ.erp.data;

import edu.univ.erp.domain.Section;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SectionDao {
    private final DataSource ds = DataSourceProvider.erpDataSource();

    public List<Section> findByInstructorId(int instructorId) {
        String sql = "SELECT section_id, course_id, instructor_id, semester, year FROM sections WHERE instructor_id = ?";
        List<Section> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Section(
                            rs.getInt("section_id"),
                            rs.getInt("course_id"),
                            rs.getInt("instructor_id"),
                            rs.getString("semester"),
                            rs.getInt("year")
                    ));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    /**
     * Find sections that a given student is enrolled in.
     * Joins enrollments -> sections and returns Section objects.
     */
    public List<Section> findByStudentId(int studentId) {
        String sql = "SELECT s.section_id, s.course_id, s.instructor_id, s.semester, s.year " +
                "FROM sections s " +
                "JOIN enrollments e ON e.section_id = s.section_id " +
                "WHERE e.student_id = ?";
        List<Section> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Section(
                            rs.getInt("section_id"),
                            rs.getInt("course_id"),
                            rs.getInt("instructor_id"),
                            rs.getString("semester"),
                            rs.getInt("year")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }


    public List<Section> findAll() {
        String sql = "SELECT section_id, course_id, instructor_id, semester, year FROM sections";
        List<Section> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Section(
                        rs.getInt("section_id"),
                        rs.getInt("course_id"),
                        rs.getInt("instructor_id"),
                        rs.getString("semester"),
                        rs.getInt("year")
                ));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    public Optional<Section> findById(int sectionId) {
        String sql = "SELECT section_id, course_id, instructor_id, semester, year "
                + "FROM sections WHERE section_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Section(
                            rs.getInt("section_id"),
                            rs.getInt("course_id"),
                            rs.getInt("instructor_id"),
                            rs.getString("semester"),
                            rs.getInt("year")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding section by id", e);
        }
        return Optional.empty();
    }


    public int insert(int courseId, int instructorId, String semester, int year) {
        String sql = "INSERT INTO sections (course_id, instructor_id, semester, year) VALUES (?, ?, ?, ?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, courseId);
            ps.setInt(2, instructorId);
            ps.setString(3, semester);
            ps.setInt(4, year);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            throw new RuntimeException("Insert succeeded but no key returned");
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void update(int sectionId, int courseId, int instructorId, String semester, int year) {
        String sql = "UPDATE sections SET course_id = ?, instructor_id = ?, semester = ?, year = ? WHERE section_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, instructorId);
            ps.setString(3, semester);
            ps.setInt(4, year);
            ps.setInt(5, sectionId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void delete(int sectionId) {
        String sql = "DELETE FROM sections WHERE section_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
