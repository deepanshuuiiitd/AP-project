package edu.univ.erp.data;

import edu.univ.erp.domain.GradingComponent;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GradingComponentDao {
    private final DataSource ds = DataSourceProvider.erpDataSource();

    /**
     * Returns all grading components.
     * Defensive version: if the table does not exist, returns empty list instead of crashing.
     */
    public List<GradingComponent> listAll() {
        String sql = "SELECT component_id, name FROM grading_components ORDER BY component_id";
        List<GradingComponent> out = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new GradingComponent(rs.getInt(1), rs.getString(2)));
                }
            }

        } catch (SQLException e) {
            System.err.println("[WARN] GradingComponentDao.listAll(): " + e.getMessage());
            // Returning an empty list avoids UI crash when DB table missing or misnamed
            return new ArrayList<>();
        }
        return out;
    }

    /**
     * Defensive findById — if table missing, return Optional.empty().
     */
    public Optional<GradingComponent> findById(int id) {
        String sql = "SELECT component_id, name FROM grading_components WHERE component_id = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new GradingComponent(rs.getInt(1), rs.getString(2)));
                }
            }

        } catch (SQLException e) {
            System.err.println("[WARN] GradingComponentDao.findById(): " + e.getMessage());
            return Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * Safe insert — logs the error but does not crash UI.
     */
    public void insert(String name) {
        String sql = "INSERT INTO grading_components (name) VALUES (?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[WARN] GradingComponentDao.insert(): " + e.getMessage());
        }
    }
}
