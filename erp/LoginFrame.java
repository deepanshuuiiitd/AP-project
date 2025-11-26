package edu.univ.erp;

import edu.univ.erp.auth.AuthService;
import edu.univ.erp.service.SystemService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Login frame with maintenance-mode check.
 *
 * Assumes AuthService#authenticate(String,String) -> Optional<String> role
 * and AuthService#getUserIdByUsername(String) -> Integer
 */
public class LoginFrame extends JFrame {
    private final AuthService authService = new AuthService();
    private final SystemService systemService = new SystemService();

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JLabel statusLabel;

    public LoginFrame() {
        setTitle("Login - University ERP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(380, 200);
        setLocationRelativeTo(null);

        setLayout(new MigLayout("wrap 2", "[][grow]", "[][][grow][]"));

        add(new JLabel("Username:"));
        usernameField = new JTextField();
        add(usernameField, "growx, wrap");

        add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        add(passwordField, "growx, wrap");

        loginButton = new JButton("Login");
        add(loginButton, "span, split 2, align center");
        JButton exitBtn = new JButton("Exit");
        add(exitBtn, "wrap");

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.DARK_GRAY);
        add(statusLabel, "span, growx");

        // Action listeners
        loginButton.addActionListener(e -> doLogin());
        exitBtn.addActionListener(e -> System.exit(0));

        // Enter key on password triggers login
        passwordField.addActionListener(e -> doLogin());
    }

    private void setStatus(String t) {
        statusLabel.setText(t == null ? " " : t);
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String role;
        try {
            role = authService.authenticate(username, password);   // returns role OR null
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Login failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (role == null) {
            JOptionPane.showMessageDialog(this, "Invalid username or password",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check maintenance mode - allow ADMIN always
        // after successfully got 'role' from authService
// Check maintenance mode - allow ADMIN always
        boolean maintenanceOn;
        try {
            maintenanceOn = systemService.isMaintenanceMode();
        } catch (Exception ex) {
            // if settings read fails, be conservative and disallow non-admins
            maintenanceOn = true;
            ex.printStackTrace();
        }

        if (maintenanceOn && !"ADMIN".equalsIgnoreCase(role)) {
            JOptionPane.showMessageDialog(this,
                    "The system is currently in maintenance mode.\nWrite operations are disabled for non-admin users.\nYou may continue in read-only mode.",
                    "Maintenance Mode (Read-only)", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Maintenance mode active - read-only for non-admins");
            // DO NOT return — allow the user to proceed to their dashboard (read-only enforcement is done elsewhere)
        }

// proceed to role-specific main frame
        SwingUtilities.invokeLater(() -> {
            dispose(); // close login
            switch (role.toUpperCase()) {
                case "ADMIN":
                    new edu.univ.erp.ui.AdminMainFrame(username).setVisible(true);
                    break;
                case "INSTRUCTOR":
                    new edu.univ.erp.ui.InstructorMainFrame(username).setVisible(true);
                    break;
                case "STUDENT":
                    new edu.univ.erp.ui.StudentMainFrame(username).setVisible(true);
                    break;
                default:
                    JOptionPane.showMessageDialog(null, "Unknown role: " + role, "Error", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        });

    }
}
