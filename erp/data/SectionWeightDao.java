package edu.univ.erp.data;

import edu.univ.erp.domain.SectionGradeWeight;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SectionWeightDao {
    private final DataSource ds = DataSourceProvider.erpDataSource();

    public List<SectionGradeWeight> findBySection(int sectionId) {
        String sql = "SELECT id, section_id, component_id, weight FROM section_grade_weights WHERE section_id = ? ORDER BY component_id";
        List<SectionGradeWeight> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new SectionGradeWeight(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getBigDecimal(4)));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    public void upsert(int sectionId, int componentId, BigDecimal weight) {
        String sql = "INSERT INTO section_grade_weights (section_id, component_id, weight) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE weight = VALUES(weight)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ps.setInt(2, componentId);
            ps.setBigDecimal(3, weight);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void deleteForSection(int sectionId) {
        String sql = "DELETE FROM section_grade_weights WHERE section_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Optional<SectionGradeWeight> findBySectionAndComponent(int sectionId, int componentId) {
        String sql = "SELECT id, section_id, component_id, weight FROM section_grade_weights WHERE section_id = ? AND component_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ps.setInt(2, componentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new SectionGradeWeight(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getBigDecimal(4)));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }
}
