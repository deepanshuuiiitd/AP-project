package edu.univ.erp.ui;

import edu.univ.erp.domain.User;
import edu.univ.erp.service.ErpService;
import net.miginfocom.swing.MigLayout;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;

/**
 * Simple dialog to add/edit a user. Uses BCrypt to hash password when provided.
 */
public class UserEditorDialog extends JDialog {
    private final ErpService erp = new ErpService();
    private final JTextField usernameField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final JComboBox<String> roleCombo =
            new JComboBox<>(new String[]{"ADMIN", "INSTRUCTOR", "STUDENT"});

    private boolean saved = false;
    private final User existing;

    public UserEditorDialog(Frame owner, User existing) {
        super(owner, existing == null ? "Add User" : "Edit User", true);
        this.existing = existing;
        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new MigLayout("wrap 2", "[right][grow]"));

        add(new JLabel("Username:"));
        add(usernameField, "growx");

        add(new JLabel("Password:"));
        add(passwordField, "growx");

        add(new JLabel("Role:"));
        add(roleCombo, "growx");

        if (existing != null) {
            usernameField.setText(existing.getUsername());
            roleCombo.setSelectedItem(existing.getRole());
            // Do not prefill password
        }

        JButton save = UIUtil.createRoundedButton("Save");
        JButton cancel = UIUtil.createGhostButton("Cancel");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(save);
        btns.add(cancel);
        add(btns, "span");

        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> onCancel());
    }

    private void onSave() {
        String username = usernameField.getText().trim();
        String pw = new String(passwordField.getPassword()).trim();
        String role = (String) roleCombo.getSelectedItem();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username required", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // If password provided, hash it; otherwise keep null to indicate no-change
            String hashed = null;
            if (!pw.isEmpty()) {
                hashed = BCrypt.hashpw(pw, BCrypt.gensalt(12));
            }

            if (existing == null) {
                // add new user
                erp.addUser(username, hashed, role);
            } else {
                // update existing user
                erp.updateUser(existing.getUserId(), username, hashed, role);
            }
            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void onCancel() {
        saved = false;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }
}
