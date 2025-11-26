package edu.univ.erp.ui;

import edu.univ.erp.auth.AuthService;
import edu.univ.erp.data.EnrollmentDao;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.service.ErpService;
import edu.univ.erp.service.MaintenanceService;
import edu.univ.erp.util.ExportUtil;
import edu.univ.erp.ui.UIUtil;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

/**
 * StudentMainFrame with Export CSV / PDF buttons and "My Grades" tab.
 */
public class StudentMainFrame extends BaseMainFrame {
    private final ErpService erpService = new ErpService();
    private final AuthService authService = new AuthService();
    private final EnrollmentDao enrollmentDao = new EnrollmentDao();

    private final DefaultTableModel enrollTableModel = new DefaultTableModel(
            new String[]{"Enroll ID", "Section ID", "Course", "Instructor ID", "Enrolled", "Grade"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable enrollTable = new JTable(enrollTableModel);

    private final DefaultTableModel availTableModel = new DefaultTableModel(
            new String[]{"Section ID", "Course", "Instructor ID", "Semester", "Year", "Enrolled"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable availTable = new JTable(availTableModel);

    private int loggedInUserId;
    private StudentGradesPanel gradesPanel; // the new grades panel

    public StudentMainFrame(String username) {
        super(username, "STUDENT");
        initUI();
        Integer uid = authService.getUserIdByUsername(username);
        if (uid == null) {
            JOptionPane.showMessageDialog(this, "Cannot determine user id for " + username, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.loggedInUserId = uid;
        loadEnrollmentData();
        loadAvailableSections();
        if (gradesPanel != null) gradesPanel.refresh();
    }

    private void initUI() {

        if (!edu.univ.erp.util.AccessChecker.isWritable()) {
            JOptionPane.showMessageDialog(this, "System is in Maintenance mode. You can view data but not change it.", "Read-only", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // We'll use a tabbed pane: "Enrollments" (your current split) and "My Grades"
        JTabbedPane tabs = new JTabbedPane();

        // Original enrollments UI (as a panel)
        JPanel enrollmentsPanel = new JPanel(new BorderLayout(10,10));
        enrollmentsPanel.setBorder(new EmptyBorder(12,12,12,12));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.5);

        // Top: current enrollments + export buttons
        JPanel top = new JPanel(new BorderLayout(6,6));
        top.setBorder(BorderFactory.createTitledBorder("My Enrollments"));
        UIUtil.decorateTable(enrollTable);
        top.add(new JScrollPane(enrollTable), BorderLayout.CENTER);
        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton exportCsvBtn = UIUtil.createRoundedButton("Export CSV");
        JButton exportPdfBtn = UIUtil.createRoundedButton("Export PDF");
        JButton dropBtn = UIUtil.createRoundedButton("Drop Enrollment");
        topButtons.add(exportCsvBtn);
        topButtons.add(exportPdfBtn);
        topButtons.add(Box.createHorizontalStrut(12));
        topButtons.add(dropBtn);
        top.add(topButtons, BorderLayout.SOUTH);

        // Bottom: available sections + enroll button
        JPanel bottom = new JPanel(new BorderLayout(6,6));
        bottom.setBorder(BorderFactory.createTitledBorder("Available Sections"));
        UIUtil.decorateTable(availTable);
        bottom.add(new JScrollPane(availTable), BorderLayout.CENTER);
        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton enrollBtn = UIUtil.createRoundedButton("Enroll");
        bottomButtons.add(enrollBtn);
        bottom.add(bottomButtons, BorderLayout.SOUTH);

        split.setTopComponent(top);
        split.setBottomComponent(bottom);
        enrollmentsPanel.add(split, BorderLayout.CENTER);

        // wire actions
        enrollBtn.addActionListener(e -> doEnrollSelectedSection());
        dropBtn.addActionListener(e -> doDropSelectedEnrollment());
        exportCsvBtn.addActionListener(e -> doExportCsv());
        exportPdfBtn.addActionListener(e -> doExportPdf());

        // Add enrollments panel as first tab
        tabs.addTab("Enrollments", enrollmentsPanel);

        // Grades tab
        gradesPanel = new StudentGradesPanel(loggedInUserId);
        tabs.addTab("My Grades", gradesPanel);

        // set tabs into center
        setCenterPanel(tabs);

        MaintenanceService maintenanceService = new MaintenanceService();
        boolean writable = !maintenanceService.isMaintenanceOn();
        if (enrollBtn != null) enrollBtn.setEnabled(writable);
        if (dropBtn != null) dropBtn.setEnabled(writable);
        updateMaintenanceBanner(!writable); // show banner if not writable

    }

    private void loadEnrollmentData() {
        enrollTableModel.setRowCount(0);
        List<Enrollment> enrollments = erpService.getEnrollmentsForStudent(loggedInUserId);
        for (Enrollment en : enrollments) {
            Section s = erpService.getSectionById(en.getSectionId()).orElse(null);
            Course c = s == null ? null : erpService.getCourseById(s.getCourseId()).orElse(null);
            String courseLabel = c == null ? ("Section " + en.getSectionId()) : (c.getCode() + " - " + c.getTitle());
            String enrolledDate = en.getEnrollmentDate() == null ? "N/A" : en.getEnrollmentDate().toString();
            String grade = erpService.getGradeForEnrollment(en.getEnrollmentId()).map(Grade::getGrade).orElse("N/A");
            enrollTableModel.addRow(new Object[]{
                    en.getEnrollmentId(),
                    en.getSectionId(),
                    courseLabel,
                    s == null ? "N/A" : s.getInstructorId(),
                    enrolledDate,
                    grade
            });
        }
    }

    private void loadAvailableSections() {
        availTableModel.setRowCount(0);
        List<Section> sections = erpService.listSections(); // all sections
        for (Section s : sections) {
            Optional<Course> oc = erpService.getCourseById(s.getCourseId());
            String courseLabel = oc.map(c -> c.getCode() + " - " + c.getTitle()).orElse("Section " + s.getSectionId());
            int enrolled = enrollmentDao.countBySectionId(s.getSectionId());
            availTableModel.addRow(new Object[]{
                    s.getSectionId(),
                    courseLabel,
                    s.getInstructorId(),
                    s.getSemester(),
                    s.getYear(),
                    enrolled
            });
        }
    }

    private void doEnrollSelectedSection() {
        int r = availTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select a section to enroll in", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        int sectionId = (Integer) availTableModel.getValueAt(r, 0);
        int ok = JOptionPane.showConfirmDialog(this, "Enroll in section " + sectionId + "?", "Confirm Enroll", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            int newId = erpService.enrollStudentInSection(loggedInUserId, sectionId);
            JOptionPane.showMessageDialog(this, "Enrolled (id=" + newId + ")", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadEnrollmentData();
            loadAvailableSections();
            if (gradesPanel != null) gradesPanel.refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error enrolling: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doDropSelectedEnrollment() {
        int r = enrollTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select an enrollment to drop", "Info", JOptionPane.INFORMATION_MESSAGE); return; }
        int enrollmentId = (Integer) enrollTableModel.getValueAt(r, 0);
        int ok = JOptionPane.showConfirmDialog(this, "Drop enrollment id " + enrollmentId + "?", "Confirm Drop", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            erpService.dropEnrollment(enrollmentId);
            JOptionPane.showMessageDialog(this, "Enrollment dropped", "Dropped", JOptionPane.INFORMATION_MESSAGE);
            loadEnrollmentData();
            loadAvailableSections();
            if (gradesPanel != null) gradesPanel.refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error dropping enrollment: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ----------------------- Export handlers ----------------------- */

    private void doExportCsv() {
        List<Enrollment> enrollments = erpService.getEnrollmentsForStudent(loggedInUserId);
        if (enrollments.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No enrollments to export", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save enrollments as CSV");
        fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        int rc = fc.showSaveDialog(this);
        if (rc != JFileChooser.APPROVE_OPTION) return;

        java.io.File chosen = fc.getSelectedFile();
        String fname = chosen.getAbsolutePath();
        if (!fname.toLowerCase().endsWith(".csv")) fname += ".csv";

        try (OutputStream os = new FileOutputStream(fname)) {
            // build rows using ExportUtil
            List<String[]> rows = ExportUtil.buildEnrollmentRows(
                    enrollments,
                    id -> erpService.getCourseById(id),
                    id -> erpService.getSectionById(id),
                    id -> erpService.getStudentByUserId(id),
                    id -> erpService.getGradeForEnrollment(id)
            );
            ExportUtil.writeCsv(os, rows);
            JOptionPane.showMessageDialog(this, "CSV exported: " + fname, "Exported", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error exporting CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doExportPdf() {
        List<Enrollment> enrollments = erpService.getEnrollmentsForStudent(loggedInUserId);
        if (enrollments.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No enrollments to export", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save enrollments as PDF");
        fc.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));
        int rc = fc.showSaveDialog(this);
        if (rc != JFileChooser.APPROVE_OPTION) return;

        java.io.File chosen = fc.getSelectedFile();
        String fname = chosen.getAbsolutePath();
        if (!fname.toLowerCase().endsWith(".pdf")) fname += ".pdf";

        try (OutputStream os = new FileOutputStream(fname)) {
            List<String[]> rows = ExportUtil.buildEnrollmentRows(
                    enrollments,
                    id -> erpService.getCourseById(id),
                    id -> erpService.getSectionById(id),
                    id -> erpService.getStudentByUserId(id),
                    id -> erpService.getGradeForEnrollment(id)
            );
            // first element is header row
            String[] header = rows.get(0);
            List<String[]> dataRows = rows.subList(1, rows.size());
            ExportUtil.writePdf(os, "My Enrollments", header, dataRows);
            JOptionPane.showMessageDialog(this, "PDF exported: " + fname, "Exported", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error exporting PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
