package edu.univ.erp.ui;

import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.ErpService;
import edu.univ.erp.service.MaintenanceService;
import edu.univ.erp.service.SystemService;
import edu.univ.erp.util.ExportUtil;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * AdminMainFrame (restored):
 *  - Top-level maintenance toggle (green/red status) outside tabs
 *  - Users tab (Add/Edit/Delete)
 *  - Courses tab (Course -> Sections)
 *  - Reports tab (exports) restored
 *
 * Refer to spec: file:///mnt/data/AP_M25_Project-v1.pdf
 */
public class AdminMainFrame extends BaseMainFrame {

    // --- Add to AdminMainFrame.java (class fields) ---
    private javax.swing.JToolBar toolbar = new javax.swing.JToolBar();

    private final ErpService erp = new ErpService();
    private final SystemService systemService = new SystemService();

    // Top-level maintenance UI
    private final JToggleButton maintenanceToggle = new JToggleButton("Maintenance");
    private final JLabel maintenanceStatusLabel = new JLabel();

    // Users tab components
    private final DefaultListModel<User> usersModel = new DefaultListModel<>();
    private final JList<User> usersList = new JList<>(usersModel);

    // Courses tab components
    private final DefaultListModel<Course> coursesModel = new DefaultListModel<>();
    private final JList<Course> coursesList = new JList<>(coursesModel);

    private final DefaultTableModel sectionsModel = new DefaultTableModel(
            new String[]{"Section ID", "Course ID", "Instructor ID", "Instructor", "Semester", "Year"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable sectionsTable = new JTable(sectionsModel);

    public AdminMainFrame(String username) {
        super(username, "ADMIN");
        initUI();
        loadAllUsers();
        loadAllCourses();
        refreshMaintenanceUI(); // set toggle and label based on DB value
    }

    private void initUI() {
        // Top container: title + maintenance toggle/status
        JPanel topBar = new JPanel(new BorderLayout(8,8));
        topBar.setBorder(new EmptyBorder(8,12,8,12));

        JLabel title = new JLabel("Admin Console");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        topBar.add(title, BorderLayout.WEST);

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        maintenanceToggle.setToolTipText("Toggle maintenance mode (ON = maintenance, OFF = running)");
        maintenanceToggle.addActionListener(e -> toggleMaintenance());
        maintenanceStatusLabel.setOpaque(true);
        maintenanceStatusLabel.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        topRight.add(maintenanceToggle);
        topRight.add(maintenanceStatusLabel);
        topBar.add(topRight, BorderLayout.EAST);

        // Create the tabs below the topBar
        JTabbedPane tabs = new JTabbedPane();

        // ---- Users Tab ----
        JPanel usersPanel = new JPanel(new BorderLayout(8,8));
        usersPanel.setBorder(new EmptyBorder(12,12,12,12));

        JPanel usersTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshUsersBtn = UIUtil.createGhostButton("Refresh");
        JButton addUserBtn = UIUtil.createRoundedButton("Add User");
        JButton editUserBtn = UIUtil.createRoundedButton("Edit User");
        JButton deleteUserBtn = UIUtil.createRoundedButton("Delete User");
        usersTop.add(refreshUsersBtn);
        usersTop.add(addUserBtn);
        usersTop.add(editUserBtn);
        usersTop.add(deleteUserBtn);
        usersPanel.add(usersTop, BorderLayout.NORTH);

        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersPanel.add(new JScrollPane(usersList), BorderLayout.CENTER);
        tabs.addTab("Users", usersPanel);

        refreshUsersBtn.addActionListener(e -> loadAllUsers());
        addUserBtn.addActionListener(e -> doAddUser());
        editUserBtn.addActionListener(e -> doEditUser());
        deleteUserBtn.addActionListener(e -> doDeleteUser());

        // ---- Courses Tab ----
        JPanel coursesPanel = new JPanel(new BorderLayout(8,8));
        coursesPanel.setBorder(new EmptyBorder(12,12,12,12));

        JPanel leftCourses = new JPanel(new BorderLayout(6,6));
        leftCourses.setBorder(BorderFactory.createTitledBorder("Courses"));
        coursesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leftCourses.add(new JScrollPane(coursesList), BorderLayout.CENTER);

        JPanel courseBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addCourseBtn = UIUtil.createRoundedButton("Add Course");
        JButton editCourseBtn = UIUtil.createRoundedButton("Edit Course");
        JButton deleteCourseBtn = UIUtil.createRoundedButton("Delete Course");
        courseBtns.add(addCourseBtn);
        courseBtns.add(editCourseBtn);
        courseBtns.add(deleteCourseBtn);
        leftCourses.add(courseBtns, BorderLayout.SOUTH);

        JPanel rightSections = new JPanel(new BorderLayout(6,6));
        rightSections.setBorder(BorderFactory.createTitledBorder("Sections for selected course"));
        rightSections.add(new JScrollPane(sectionsTable), BorderLayout.CENTER);

        JPanel sectionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addSectionBtn = UIUtil.createRoundedButton("Add Section");
        JButton editSectionBtn = UIUtil.createRoundedButton("Edit Section");
        JButton deleteSectionBtn = UIUtil.createRoundedButton("Delete Section");
        JButton manageSectionGradesBtn = UIUtil.createRoundedButton("Manage Grades");
        sectionBtns.add(addSectionBtn);
        sectionBtns.add(editSectionBtn);
        sectionBtns.add(deleteSectionBtn);
        sectionBtns.add(manageSectionGradesBtn);
        rightSections.add(sectionBtns, BorderLayout.SOUTH);

        JSplitPane courseSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCourses, rightSections);
        courseSplit.setDividerLocation(300);
        coursesPanel.add(courseSplit, BorderLayout.CENTER);
        tabs.addTab("Courses", coursesPanel);

        // courses actions
        coursesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Course selected = coursesList.getSelectedValue();
                if (selected != null) loadSectionsForCourse(selected.getCourseId());
                else sectionsModel.setRowCount(0);
            }
        });

        addCourseBtn.addActionListener(e -> doAddCourse());
        editCourseBtn.addActionListener(e -> doEditCourse());
        deleteCourseBtn.addActionListener(e -> doDeleteCourse());

        addSectionBtn.addActionListener(e -> doAddSection());
        editSectionBtn.addActionListener(e -> doEditSection());
        deleteSectionBtn.addActionListener(e -> doDeleteSection());
        manageSectionGradesBtn.addActionListener(e -> openGradeEditorForSelectedSection());

        // double-click grade-editor
        sectionsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = sectionsTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        int sid = (Integer) sectionsModel.getValueAt(row, 0);
                        new InstructorGradeEditorDialog(AdminMainFrame.this, sid, "Section " + sid).setVisible(true);
                    }
                }
            }
        });

        // ---- Reports Tab (restored) ----
        JPanel reportsPanel = new JPanel(new BorderLayout(8,8));
        reportsPanel.setBorder(new EmptyBorder(12,12,12,12));

        JPanel reportsTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshReportsBtn = UIUtil.createGhostButton("Refresh");
        JButton exportEnrollmentsBtn = UIUtil.createRoundedButton("Export Enrollments (CSV)");
        JButton exportUsersBtn = UIUtil.createRoundedButton("Export Users (CSV)");
        reportsTop.add(refreshReportsBtn);
        reportsTop.add(exportEnrollmentsBtn);
        reportsTop.add(exportUsersBtn);
        reportsPanel.add(reportsTop, BorderLayout.NORTH);

        JTextArea reportOutput = new JTextArea();
        reportOutput.setEditable(false);
        reportOutput.setBorder(BorderFactory.createTitledBorder("Report Output"));
        reportsPanel.add(new JScrollPane(reportOutput), BorderLayout.CENTER);

        // reports actions
        refreshReportsBtn.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append("System Summary\n");
            sb.append("--------------\n");
            sb.append("Users: ").append(erp.listUsers().size()).append("\n");
            sb.append("Courses: ").append(erp.listCourses().size()).append("\n");
            sb.append("Sections: ").append(erp.listSections().size()).append("\n");
            reportOutput.setText(sb.toString());
        });

        exportUsersBtn.addActionListener(e -> {
            try {
                java.util.List<User> users = erp.listUsers();
                java.util.List<String[]> rows = new java.util.ArrayList<>();
                rows.add(new String[] {"UserId", "Username", "Role"});
                for (User u : users) rows.add(new String[] { String.valueOf(u.getUserId()), u.getUsername(), u.getRole()});
                java.io.File f = new java.io.File("users_report.csv");
                try (java.io.OutputStream os = new java.io.FileOutputStream(f)) {
                    ExportUtil.writeCsv(os, rows);
                }
                JOptionPane.showMessageDialog(this, "Exported users to " + f.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed export: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        exportEnrollmentsBtn.addActionListener(e -> {
            try {
                java.util.List<edu.univ.erp.domain.Enrollment> allRows = new java.util.ArrayList<>();
                for (Section s : erp.listSections()) {
                    allRows.addAll(erp.getEnrollmentsForSection(s.getSectionId()));
                }
                java.util.List<String[]> rows = ExportUtil.buildEnrollmentRows(
                        allRows,
                        id -> erp.getCourseById(id),
                        id -> erp.getSectionById(id),
                        id -> erp.getStudentByUserId(id),
                        id -> erp.getGradeForEnrollment(id)
                );
                java.io.File f = new java.io.File("all_enrollments_report.csv");
                try (java.io.OutputStream os = new java.io.FileOutputStream(f)) {
                    ExportUtil.writeCsv(os, rows);
                }
                JOptionPane.showMessageDialog(this, "Exported enrollments to " + f.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed export: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Maintenance toggle (Admin-only)
// Use the existing `maintenanceToggle` field and `systemService` for DB updates.
// Add a small separator and the toggle to the toolbar.
        toolbar.addSeparator(new Dimension(12, 0));

// initialize toggle text/state from systemService
        maintenanceToggle.setText(systemService.isMaintenanceMode() ? "Maintenance ON" : "Maintenance OFF");
        maintenanceToggle.setSelected(systemService.isMaintenanceMode());
        maintenanceToggle.setToolTipText("Toggle system maintenance mode (read-only for instructors & students)");
        toolbar.add(maintenanceToggle);

// When admin toggles, use your existing toggleMaintenance() method to perform the change.
// Then update maintenance banners on all open BaseMainFrame windows so everyone sees the new state.
        maintenanceToggle.addActionListener(e -> {
            // Perform the existing toggle logic (which will update DB via systemService)
            toggleMaintenance();

            // After toggleMaintenance() runs, re-sync the toggle text to the DB's authoritative value
            boolean currentOn = systemService.isMaintenanceMode();
            maintenanceToggle.setText(currentOn ? "Maintenance ON" : "Maintenance OFF");
            maintenanceToggle.setSelected(currentOn);

            // Update all open BaseMainFrame windows so their banners reflect the new state
            for (Window w : Window.getWindows()) {
                if (w instanceof edu.univ.erp.ui.BaseMainFrame) {
                    ((edu.univ.erp.ui.BaseMainFrame) w).updateMaintenanceBanner(currentOn);
                }
            }
        });



        tabs.addTab("Reports", reportsPanel);

        // Combine topBar + tabs into main center panel of BaseMainFrame
        JPanel center = new JPanel(new BorderLayout(6,6));
        center.add(topBar, BorderLayout.NORTH);
        center.add(tabs, BorderLayout.CENTER);
        setCenterPanel(center);

        getContentPane().add(toolbar, BorderLayout.NORTH);
    }

    /* ------------------- Users actions ------------------- */

    private void loadAllUsers() {
        usersModel.clear();
        List<User> users = erp.listUsers();
        for (User u : users) usersModel.addElement(u);
        if (!usersModel.isEmpty()) usersList.setSelectedIndex(0);
    }

    private void doAddUser() {
        UserEditorDialog dlg = new UserEditorDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadAllUsers();
    }

    private void doEditUser() {
        User sel = usersList.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select a user to edit", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        UserEditorDialog dlg = new UserEditorDialog(this, sel);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadAllUsers();
    }

    private void doDeleteUser() {
        User sel = usersList.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select a user to delete", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        int ok = JOptionPane.showConfirmDialog(this, "Delete user " + sel.getUsername() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        erp.deleteUser(sel.getUserId());
        loadAllUsers();
    }

    /* ------------------- Courses / Sections actions ------------------- */

    private void loadAllCourses() {
        coursesModel.clear();
        List<Course> courses = erp.listCourses();
        for (Course c : courses) coursesModel.addElement(c);
        if (!coursesModel.isEmpty()) coursesList.setSelectedIndex(0);
    }

    private void loadSectionsForCourse(int courseId) {
        sectionsModel.setRowCount(0);
        List<Section> secs = erp.listSectionsForCourse(courseId);
        for (Section s : secs) {
            String instrName = "ID:" + s.getInstructorId();
            Optional<User> ui = erp.getUserById(s.getInstructorId());
            if (ui.isPresent()) instrName = ui.get().getUsername();
            sectionsModel.addRow(new Object[]{
                    s.getSectionId(), s.getCourseId(), s.getInstructorId(), instrName,
                    s.getSemester(), s.getYear()
            });
        }
    }

    private void doAddCourse() {
        CourseEditorDialog dlg = new CourseEditorDialog(this, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadAllCourses();
    }

    private void doEditCourse() {
        Course sel = coursesList.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select a course", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        CourseEditorDialog dlg = new CourseEditorDialog(this, sel);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadAllCourses();
    }

    private void doDeleteCourse() {
        Course sel = coursesList.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select a course", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        int ok = JOptionPane.showConfirmDialog(this, "Delete course " + sel.getCode() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        erp.deleteCourse(sel.getCourseId());
        loadAllCourses();
    }

    private void doAddSection() {
        Course sel = coursesList.getSelectedValue();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select a course first", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        List<User> instructors = erp.listInstructors();
        SectionEditorDialog dlg = new SectionEditorDialog(this, null, sel.getCourseId(), instructors);
        dlg.setVisible(true);
        if (dlg.isSaved()) loadSectionsForCourse(sel.getCourseId());
    }

    private void doEditSection() {
        int row = sectionsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a section first", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        int sid = (Integer) sectionsModel.getValueAt(row, 0);
        Optional<Section> opt = erp.getSectionById(sid);
        if (!opt.isPresent()) { JOptionPane.showMessageDialog(this, "Section not found", "Error", JOptionPane.ERROR_MESSAGE); return; }
        SectionEditorDialog dlg = new SectionEditorDialog(this, opt.get(), opt.get().getCourseId(), erp.listInstructors());
        dlg.setVisible(true);
        if (dlg.isSaved()) loadSectionsForCourse(opt.get().getCourseId());
    }

    private void doDeleteSection() {
        int row = sectionsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a section first", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        int sid = (Integer) sectionsModel.getValueAt(row, 0);
        int ok = JOptionPane.showConfirmDialog(this, "Delete section " + sid + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        erp.deleteSection(sid);
        Course sel = coursesList.getSelectedValue();
        if (sel != null) loadSectionsForCourse(sel.getCourseId());
    }

    private void openGradeEditorForSelectedSection() {
        int row = sectionsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a section first", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        int sid = (Integer) sectionsModel.getValueAt(row, 0);
        new InstructorGradeEditorDialog(this, sid, "Section " + sid).setVisible(true);
    }

    /* ----------------- Maintenance helpers ----------------- */

    private void toggleMaintenance() {
        boolean requestedOn = maintenanceToggle.isSelected();
        try {
            boolean actualOn = systemService.setMaintenanceMode(requestedOn);
            // If actual value doesn't match requested, notify and set toggle accordingly
            if (actualOn != requestedOn) {
                JOptionPane.showMessageDialog(this,
                        "Requested maintenance = " + (requestedOn ? "ON" : "OFF") +
                                " but DB saved = " + (actualOn ? "ON" : "OFF"),
                        "Warning", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to update maintenance: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            // always re-sync UI with DB value (defensive)
            refreshMaintenanceUI();
        }
    }

    private void refreshMaintenanceUI() {
        boolean on = systemService.isMaintenanceMode();
        maintenanceToggle.setSelected(on);
        if (on) {
            maintenanceStatusLabel.setText("MAINTENANCE");
            maintenanceStatusLabel.setBackground(new Color(0xD9534F)); // red
            maintenanceStatusLabel.setForeground(Color.WHITE);
        } else {
            maintenanceStatusLabel.setText("RUNNING");
            maintenanceStatusLabel.setBackground(new Color(0x5CB85C)); // green
            maintenanceStatusLabel.setForeground(Color.WHITE);
        }
    }
}
