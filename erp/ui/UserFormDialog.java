package edu.univ.erp.ui;

import edu.univ.erp.domain.User;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class UserFormDialog extends JDialog {
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{"ADMIN", "INSTRUCTOR", "STUDENT"});
    private boolean saved = false;

    public UserFormDialog(Frame owner, String title) {
        super(owner, title, true);
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap 2", "[][grow]", "[][][]"));
        add(new JLabel("Username:"));
        add(usernameField, "growx");

        add(new JLabel("Password:"));
        add(passwordField, "growx, wrap");
        add(new JLabel("<html><small>(leave blank to keep current password when editing)</small></html>"), "span, wrap");

        add(new JLabel("Role:"));
        add(roleCombo, "growx, wrap");

        JPanel buttons = new JPanel();
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        buttons.add(save);
        buttons.add(cancel);
        add(buttons, "span, center");

        save.addActionListener(e -> {
            if (usernameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username is required", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            saved = true;
            setVisible(false);
        });

        cancel.addActionListener(e -> {
            saved = false;
            setVisible(false);
        });

        pack();
        setLocationRelativeTo(getOwner());
    }

    public void setUser(User u) {
        if (u == null) return;
        usernameField.setText(u.getUsername());
        roleCombo.setSelectedItem(u.getRole());
        // password left blank intentionally
    }

    public String getUsername() { return usernameField.getText().trim(); }
    public String getPassword() { return new String(passwordField.getPassword()); }
    public String getRole() { return (String) roleCombo.getSelectedItem(); }
    public boolean isSaved() { return saved; }
}
