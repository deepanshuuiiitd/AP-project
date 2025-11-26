package edu.univ.erp.data;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

/**
 * SettingsDao — Handles key-value settings such as maintenance mode.
 * Works with either `system_settings` or legacy `settings` table.
 */
public class SettingsDao {

    private final DataSource ds;

    public SettingsDao() {
        this.ds = DataSourceProvider.erpDataSource();
    }

    /** ============================================================
     *  Helper: check if table exists
     *  ============================================================ */
    private boolean tableExists(String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return false;
    }

    /** ============================================================
     *  Read a setting (String) from table
     *  ============================================================ */
    public String findByKey(String key) {
        String table = tableExists("system_settings") ? "system_settings" :
                tableExists("settings")        ? "settings"        : null;

        if (table == null) return null;

        String sql = "SELECT setting_value FROM " + table + " WHERE setting_key = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("setting_value") : null;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /** ============================================================
     *  Update setting
     *  ============================================================ */
    public void updateValue(String key, String value) {
        String table = tableExists("system_settings") ? "system_settings" :
                tableExists("settings")        ? "settings"        : null;

        if (table == null) {
            createSystemSettingsTable();
            table = "system_settings";
        }

        String sql = "UPDATE " + table + " SET setting_value = ? WHERE setting_key = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setString(2, key);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                insert(key, value);
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /** ============================================================
     *  Insert setting
     *  ============================================================ */
    public void insert(String key, String value) {
        String table = tableExists("system_settings") ? "system_settings" :
                tableExists("settings")        ? "settings"        : null;

        if (table == null) {
            createSystemSettingsTable();
            table = "system_settings";
        }

        String sql = "INSERT INTO " + table + " (setting_key, setting_value) VALUES (?, ?)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /** ============================================================
     *  Ensure system_settings exists
     *  ============================================================ */
    public void createSystemSettingsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS system_settings (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "setting_key VARCHAR(100) NOT NULL UNIQUE, " +
                "setting_value VARCHAR(200), " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP " +
                "ON UPDATE CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection c = ds.getConnection();
             Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    /** ============================================================
     *  API expected by MaintenanceService
     *  ============================================================ */
    public boolean isMaintenanceOn() {
        String v = findByKey("maintenance_mode");
        return v != null && v.equalsIgnoreCase("ON");
    }

    public void setSetting(String key, String value) {
        if (findByKey(key) != null) updateValue(key, value);
        else insert(key, value);
    }

    // ============================================================
    // Enrollment Deadline Support
    // ============================================================

    public java.time.LocalDate getEnrollmentDeadline() {
        String v = findByKey("enrollment_deadline");
        if (v == null || v.isBlank()) return null;
        try {
            return java.time.LocalDate.parse(v);
        } catch (Exception ex) {
            return null;
        }
    }

    public void setEnrollmentDeadline(java.time.LocalDate date) {
        if (date == null) {
            setSetting("enrollment_deadline", null);
        } else {
            setSetting("enrollment_deadline", date.toString());
        }
    }

}
