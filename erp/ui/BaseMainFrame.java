package edu.univ.erp.ui;

import edu.univ.erp.LoginFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * BaseMainFrame
 * <p>
 * - Provides common window chrome (title, menu, theme toggle)
 * - Provides a central area where subclasses call setCenterPanel(component)
 * - Adds a small status bar and consistent padding
 */
public class BaseMainFrame extends JFrame {
    protected final String username;
    protected final String role;

    // Center container where child frames place their main UI
    private final JPanel centerContainer = new JPanel(new BorderLayout());

    // Simple status label at the bottom
    private final JLabel statusLabel = new JLabel("Ready");

    private final JLabel maintenanceLabel = new JLabel();
    private final JPanel maintenanceBanner = new JPanel(new BorderLayout());


    public BaseMainFrame(String username, String role) {
        super();
        this.username = username;
        this.role = role;

        // Window title
        setTitle(buildTitle());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);

        // Apply uniform padding to content area
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        getContentPane().setLayout(new BorderLayout(8, 8));

        // Menu bar with file menu and theme toggle
        setJMenuBar(createMenuBar());

        // Setup maintenance banner (initially hidden)
        maintenanceBanner.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        maintenanceBanner.add(maintenanceLabel, BorderLayout.CENTER);
        maintenanceBanner.setVisible(false);
        maintenanceBanner.setBackground(new java.awt.Color(0xFFEE99)); // pale yellow
        getContentPane().add(maintenanceBanner, BorderLayout.NORTH);

        // set initial banner state according to DB
        boolean maintenanceOn = new edu.univ.erp.service.MaintenanceService().isMaintenanceOn();
        updateMaintenanceBanner(maintenanceOn);


        // Center: placeholder panel (subclasses use setCenterPanel)
        centerContainer.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        getContentPane().add(centerContainer, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        statusBar.add(statusLabel, BorderLayout.WEST);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        // When window closes, update UI or perform cleanup if needed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // default behaviour - nothing special right now
            }

            @Override
            public void windowClosed(WindowEvent e) {
                // On close, ensure other windows remain working
            }
        });
    }

    /** Construct dynamic title including logged-in user & role */
    private String buildTitle() {
        String base = "University ERP";
        if (username != null && !username.isEmpty()) {
            base += " - " + username + " (" + role + ")";
        } else {
            base += " - " + role;
        }
        return base;
    }

    /**
     * Convenience for child frames to set their central UI.
     * Replaces existing center content with the given component.
     */
    protected void setCenterPanel(JComponent comp) {
        centerContainer.removeAll();
        centerContainer.add(comp, BorderLayout.CENTER);
        // revalidate + repaint to ensure layout updates immediately
        centerContainer.revalidate();
        centerContainer.repaint();
    }

    /**
     * Update text shown in the status bar.
     */
    protected void setStatus(String text) {
        statusLabel.setText(text == null ? "" : text);
    }

    /**
     * Creates the application menu bar containing:
     * - File -> Logout / Exit
     * - Theme toggle (right aligned) to switch Light/Dark
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem logoutItem = new JMenuItem("Logout");
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(logoutItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // Actions
        logoutItem.addActionListener(e -> doLogout());
        exitItem.addActionListener(e -> doExit());

        // Right-aligned panel for theme toggle and small info
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);

        // Theme toggle button (selected = dark)
        JToggleButton themeToggle = new JToggleButton("Dark");
        themeToggle.setToolTipText("Toggle Dark / Light theme");
        themeToggle.setFocusable(false);
        themeToggle.setSelected(false); // default light
        themeToggle.addActionListener(e -> {
            boolean dark = themeToggle.isSelected();
            // Preserve current default font
            Font defaultFont = UIManager.getFont("defaultFont");
            String fontName = defaultFont != null ? defaultFont.getName() : "Segoe UI";
            int fontSize = defaultFont != null ? defaultFont.getSize() : 14;
            UIUtil.initLookAndFeel(dark, fontName, fontSize);

            // update UI for all open windows
            for (Window w : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(w);
            }

            // re-decorate tables so renderers pick up new colors
            UIUtil.refreshAllTables();
        });

        // small user label next to toggle
        JLabel userLabel = new JLabel(username == null ? "" : username);
        userLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        userLabel.setForeground(UIManager.getColor("Label.foreground"));

        JPanel tiny = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        tiny.setOpaque(false);
        tiny.add(userLabel);
        tiny.add(themeToggle);

        // Glue to push to right
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(tiny);

        return menuBar;
    }

    /** Logout: close this frame and open LoginFrame again */
    private void doLogout() {
        int ok = JOptionPane.showConfirmDialog(this, "Logout and return to login screen?", "Logout", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        dispose();
        // show login frame
        SwingUtilities.invokeLater(() -> {
            LoginFrame lf = new LoginFrame();
            lf.setVisible(true);
        });
    }

    /** Exit the application (confirm then exit) */
    private void doExit() {
        int ok = JOptionPane.showConfirmDialog(this, "Exit application?", "Exit", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        // exit gracefully
        Window[] wins = Window.getWindows();
        for (Window w : wins) {
            w.dispose();
        }
        System.exit(0);
    }

    protected void updateMaintenanceBanner(boolean on) {
        if (on) {
            maintenanceLabel.setText("MAINTENANCE MODE — system is read-only for students and instructors.");
            maintenanceBanner.setVisible(true);
        } else {
            maintenanceBanner.setVisible(false);
        }
        maintenanceBanner.revalidate();
        maintenanceBanner.repaint();
    }

}
