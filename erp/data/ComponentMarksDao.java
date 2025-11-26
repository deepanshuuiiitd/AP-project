package edu.univ.erp.data;

import edu.univ.erp.domain.ComponentMark;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ComponentMarksDao {
    private final DataSource ds = DataSourceProvider.erpDataSource();

    public List<ComponentMark> findByEnrollment(int enrollmentId) {
        String sql = "SELECT id, enrollment_id, component_id, marks FROM component_marks WHERE enrollment_id = ? ORDER BY component_id";
        List<ComponentMark> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new ComponentMark(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getBigDecimal(4)));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    public Optional<ComponentMark> find(int enrollmentId, int componentId) {
        String sql = "SELECT id, enrollment_id, component_id, marks FROM component_marks WHERE enrollment_id = ? AND component_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            ps.setInt(2, componentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new ComponentMark(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getBigDecimal(4)));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public void upsert(int enrollmentId, int componentId, BigDecimal marks) {
        String sql = "INSERT INTO component_marks (enrollment_id, component_id, marks) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE marks = VALUES(marks)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            ps.setInt(2, componentId);
            if (marks == null) ps.setNull(3, Types.DECIMAL);
            else ps.setBigDecimal(3, marks);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
