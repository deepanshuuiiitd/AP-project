package edu.univ.erp;

import edu.univ.erp.ui.UIUtil;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // choose default: light theme; you can toggle later from the UI
        UIUtil.initLookAndFeel(false, "Segoe UI", 14);

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
