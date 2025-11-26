package edu.univ.erp.auth;

import edu.univ.erp.data.DataSourceProvider;
import org.mindrot.jbcrypt.BCrypt;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {
    // returns role string if success, otherwise null
    public String authenticate(String username, String plainPassword) {
        DataSource ds = DataSourceProvider.authDataSource();
        String sql = "SELECT password_hash, role FROM users WHERE username = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    String role = rs.getString("role");
                    // bcrypt check
                    if (BCrypt.checkpw(plainPassword, hash)) {
                        return role;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error during authentication", e);
        }
    }

    public Integer getUserIdByUsername(String username) {
        DataSource ds = DataSourceProvider.authDataSource();
        String sql = "SELECT user_id FROM users WHERE username = ?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB error while fetching user id", e);
        }
        return null;
    }
}
