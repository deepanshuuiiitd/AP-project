package edu.univ.erp.service;

import edu.univ.erp.data.DataSourceProvider;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportService - small helper to run reporting queries and return rows as String[].
 * Each returned List has header row as first element.
 */
public class ReportService {
    private final DataSource ds = DataSourceProvider.erpDataSource();

    public List<String[]> studentsPerCourse() {
        String sql = "SELECT c.course_id, c.code, c.title, COALESCE(COUNT(e.enrollment_id),0) AS student_count " +
                "FROM courses c " +
                "LEFT JOIN sections s ON s.course_id = c.course_id " +
                "LEFT JOIN enrollments e ON e.section_id = s.section_id " +
                "GROUP BY c.course_id, c.code, c.title " +
                "ORDER BY student_count DESC, c.code";
        List<String[]> out = new ArrayList<>();
        out.add(new String[]{"Course ID", "Code", "Title", "Students Enrolled"});
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new String[]{
                        String.valueOf(rs.getInt("course_id")),
                        rs.getString("code"),
                        rs.getString("title"),
                        String.valueOf(rs.getInt("student_count"))
                });
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    public List<String[]> studentsPerSection() {
        String sql = "SELECT s.section_id, c.code, c.title, s.semester, s.year, COALESCE(COUNT(e.enrollment_id),0) AS student_count " +
                "FROM sections s " +
                "LEFT JOIN courses c ON c.course_id = s.course_id " +
                "LEFT JOIN enrollments e ON e.section_id = s.section_id " +
                "GROUP BY s.section_id, c.code, c.title, s.semester, s.year " +
                "ORDER BY student_count DESC, s.section_id";
        List<String[]> out = new ArrayList<>();
        out.add(new String[]{"Section ID", "Course Code", "Course Title", "Semester", "Year", "Students Enrolled"});
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new String[]{
                        String.valueOf(rs.getInt("section_id")),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("semester"),
                        String.valueOf(rs.getInt("year")),
                        String.valueOf(rs.getInt("student_count"))
                });
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    public List<String[]> gradeDistribution() {
        String sql = "SELECT c.course_id, c.code, c.title, g.grade, COUNT(*) AS cnt " +
                "FROM grades g " +
                "JOIN enrollments e ON e.enrollment_id = g.enrollment_id " +
                "JOIN sections s ON e.section_id = s.section_id " +
                "JOIN courses c ON s.course_id = c.course_id " +
                "GROUP BY c.course_id, g.grade " +
                "ORDER BY c.course_id, g.grade";
        List<String[]> out = new ArrayList<>();
        out.add(new String[]{"Course ID", "Course Code", "Course Title", "Grade", "Count"});
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new String[]{
                        String.valueOf(rs.getInt("course_id")),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("grade"),
                        String.valueOf(rs.getInt("cnt"))
                });
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    public List<String[]> instructorLoad() {
        // note: join instructors to auth_db.users if username needed; this assumes same server and privileges
        String sql = "SELECT u.user_id, u.username, COALESCE(COUNT(s.section_id),0) AS sections_count " +
                "FROM auth_db.users u " +
                "JOIN instructors i ON i.user_id = u.user_id " +
                "LEFT JOIN sections s ON s.instructor_id = i.user_id " +
                "WHERE u.role = 'INSTRUCTOR' " +
                "GROUP BY u.user_id, u.username " +
                "ORDER BY sections_count DESC, u.username";
        List<String[]> out = new ArrayList<>();
        out.add(new String[]{"Instructor ID", "Username", "Sections Taught"});
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new String[]{
                        String.valueOf(rs.getInt("user_id")),
                        rs.getString("username"),
                        String.valueOf(rs.getInt("sections_count"))
                });
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    public List<String[]> summary() {
        String sqlStudents = "SELECT COUNT(*) FROM students";
        String sqlCourses = "SELECT COUNT(*) FROM courses";
        String sqlSections = "SELECT COUNT(*) FROM sections";
        String sqlEnrolls = "SELECT COUNT(*) FROM enrollments";
        List<String[]> out = new ArrayList<>();
        out.add(new String[]{"Metric", "Value"});
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sqlStudents);
                 ResultSet rs = ps.executeQuery()) { if (rs.next()) out.add(new String[]{"Students", String.valueOf(rs.getInt(1))}); }
            try (PreparedStatement ps = c.prepareStatement(sqlCourses);
                 ResultSet rs = ps.executeQuery()) { if (rs.next()) out.add(new String[]{"Courses", String.valueOf(rs.getInt(1))}); }
            try (PreparedStatement ps = c.prepareStatement(sqlSections);
                 ResultSet rs = ps.executeQuery()) { if (rs.next()) out.add(new String[]{"Sections", String.valueOf(rs.getInt(1))}); }
            try (PreparedStatement ps = c.prepareStatement(sqlEnrolls);
                 ResultSet rs = ps.executeQuery()) { if (rs.next()) out.add(new String[]{"Enrollments", String.valueOf(rs.getInt(1))}); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }
}
