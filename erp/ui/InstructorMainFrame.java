package edu.univ.erp.ui;

import edu.univ.erp.auth.AuthService;
import edu.univ.erp.data.GradeDao;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Student;
import edu.univ.erp.service.ErpService;
import edu.univ.erp.service.MaintenanceModeException;
import edu.univ.erp.service.MaintenanceService;
import edu.univ.erp.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Optional;

/**
 * InstructorMainFrame with inline editable Grade column.
 * - Grade column uses JComboBox editor with allowed grade options.
 * - Edits are persisted immediately via ErpService.upsertGradeForEnrollment.
 */
public class InstructorMainFrame extends BaseMainFrame {
    private final ErpService erpService = new ErpService();
    private final AuthService authService = new AuthService();
    private final GradeDao gradeDao = new GradeDao();

    // Section list model
    private final DefaultListModel<Section> sectionListModel = new DefaultListModel<>();
    private final JList<Section> sectionList = new JList<>(sectionListModel);

    // Students table model and table
    private final DefaultTableModel studentsTableModel = new DefaultTableModel(
            new String[]{"Enroll ID", "Student ID", "Roll No", "Program", "Enrolled", "Grade"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            // Only grade column editable (last column)
            return col == 5;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0 || columnIndex == 1) return Integer.class;
            return String.class;
        }
    };
    private final JTable studentsTable = new JTable(studentsTableModel);

    // allowed grades (editable dropdown)
    private final String[] GRADE_OPTIONS = new String[]{
            "A", "A-", "B+", "B", "B-", "C+", "C", "D", "F"
    };

    // simple single-step undo stack (stores last change)
    private static class GradeChange {
        final int enrollmentId;
        final String oldGrade;
        final String newGrade;
        GradeChange(int enrollmentId, String oldGrade, String newGrade) {
            this.enrollmentId = enrollmentId; this.oldGrade = oldGrade; this.newGrade = newGrade;
        }
    }
    private final Deque<GradeChange> undoStack = new ArrayDeque<>();

    // mutating UI controls
    private final JButton undoBtn = UIUtil.createRoundedButton("Undo Last Change");
    private JButton enterGradeBtn;
    private JButton refreshBtn;

    public InstructorMainFrame(String username) {
        super(username, "INSTRUCTOR");
        initUI();
        loadSectionsForUser(username);
        // add shared status bar
        getContentPane().add(getStatusBar(), BorderLayout.SOUTH);
        // initial maintenance application
        applyMaintenanceStateSafe();
    }

    private void initUI() {

        if (!edu.univ.erp.util.AccessChecker.isWritable()) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(InstructorMainFrame.this, "System is in Maintenance mode. Cannot save grades now.", "Read-only", JOptionPane.INFORMATION_MESSAGE);
                // reload the table/model from DB to revert unsaved edits
                reloadCurrentSectionStudents(); // replace with your method to refresh the table
            });
            return;
        }

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        // left panel: sections
        JPanel left = new JPanel(new BorderLayout(6, 6));
        left.setBorder(BorderFactory.createTitledBorder("My Sections"));
        sectionList.setFixedCellHeight(28);
        sectionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        left.add(new JScrollPane(sectionList), BorderLayout.CENTER);

        // right: students table
        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createTitledBorder("Students & Grades"));
        UIUtil.decorateTable(studentsTable);
        studentsTable.setRowHeight(26);
        studentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        right.add(new JScrollPane(studentsTable), BorderLayout.CENTER);

        // set up grade editor (JComboBox)
        JComboBox<String> gradeCombo = new JComboBox<>(GRADE_OPTIONS);
        studentsTable.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(gradeCombo));

        // buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        refreshBtn = UIUtil.createGhostButton("Refresh");
        enterGradeBtn = UIUtil.createRoundedButton("Enter Grade");
        undoBtn.setEnabled(false);
        btnRow.add(refreshBtn);
        btnRow.add(enterGradeBtn);
        btnRow.add(undoBtn);
        right.add(btnRow, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(320);
        root.add(split, BorderLayout.CENTER);
        setCenterPanelSafely(root);

        // listeners
        sectionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Section s = sectionList.getSelectedValue();
                if (s != null) loadStudentsForSection(s.getSectionId());
            }
        });

        refreshBtn.addActionListener(e -> {
            Section s = sectionList.getSelectedValue();
            if (s != null) loadStudentsForSection(s.getSectionId());
        });

        enterGradeBtn.addActionListener(e -> {
            int r = studentsTable.getSelectedRow();
            if (r >= 0) {
                studentsTable.editCellAt(r, 5);
                Component editor = studentsTable.getEditorComponent();
                if (editor != null) editor.requestFocusInWindow();
            } else {
                JOptionPane.showMessageDialog(this, "Select a student row to edit grade", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // handle actual persisting when table model changes (user edits a cell)
        studentsTableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                // only act on updates (not insert/delete) and only on grade column
                if (e.getType() != TableModelEvent.UPDATE) return;
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (col != 5) return; // only grade column
                try {
                    Integer enrollmentId = (Integer) studentsTableModel.getValueAt(row, 0);
                    String newGradeRaw = String.valueOf(studentsTableModel.getValueAt(row, col));
                    String newGrade = "N/A".equals(newGradeRaw) ? "" : newGradeRaw;
                    // fetch existing grade from DB (to push on undo stack)
                    Optional<Grade> existing = gradeDao.findByEnrollmentId(enrollmentId);
                    String oldGrade = existing.map(Grade::getGrade).orElse("");
                    // persist via service wrapper (guards maintenance)
                    boolean changed = erpService.upsertGradeForEnrollment(enrollmentId, newGrade, InstructorMainFrame.this.role);
                    if (changed) {
                        // record to undo stack
                        undoStack.push(new GradeChange(enrollmentId, oldGrade == null ? "" : oldGrade, newGrade));
                        undoBtn.setEnabled(true);
                        setStatus("Grade saved for enrollment " + enrollmentId);
                    } else {
                        setStatus("No change persisted for enrollment " + enrollmentId);
                    }
                } catch (MaintenanceModeException mm) {
                    JOptionPane.showMessageDialog(InstructorMainFrame.this, mm.getMessage(), "Maintenance mode", JOptionPane.WARNING_MESSAGE);
                    // refresh UI from DB to revert any local change
                    Section s = sectionList.getSelectedValue();
                    if (s != null) loadStudentsForSection(s.getSectionId());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(InstructorMainFrame.this, "Error saving grade: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        // Undo action: revert last change
        undoBtn.addActionListener(e -> {
            if (undoStack.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nothing to undo", "Info", JOptionPane.INFORMATION_MESSAGE);
                undoBtn.setEnabled(false);
                return;
            }
            GradeChange last = undoStack.pop();
            try {
                if (last.oldGrade == null || last.oldGrade.isEmpty()) {
                    gradeDao.deleteByEnrollmentId(last.enrollmentId);
                } else {
                    gradeDao.upsertByEnrollmentId(last.enrollmentId, last.oldGrade);
                }
                for (int r = 0; r < studentsTableModel.getRowCount(); r++) {
                    Integer eid = (Integer) studentsTableModel.getValueAt(r, 0);
                    if (eid != null && eid == last.enrollmentId) {
                        String display = (last.oldGrade == null || last.oldGrade.isEmpty()) ? "N/A" : last.oldGrade;
                        studentsTableModel.setValueAt(display, r, 5);
                        break;
                    }
                }
                setStatus("Reverted grade for enrollment " + last.enrollmentId);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Undo failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } finally {
                undoBtn.setEnabled(!undoStack.isEmpty());
            }
        });

        MaintenanceService maintenanceService = new MaintenanceService();
        boolean writable = !maintenanceService.isMaintenanceOn();
        if (enterGradeBtn != null) enterGradeBtn.setEnabled(writable);
        if (undoBtn != null) undoBtn.setEnabled(writable && !undoStack.isEmpty()); // adjust if you use a different undo mechanism
        updateMaintenanceBanner(!writable);

    }

    // helper to set center panel (similar to StudentMainFrame)
    private void setCenterPanelSafely(JComponent c) {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(c, BorderLayout.CENTER);
    }

    private void loadSectionsForUser(String username) {
        Integer userId = authService.getUserIdByUsername(username);
        if (userId == null) {
            JOptionPane.showMessageDialog(this, "Unable to determine instructor id for " + username, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<Section> sections = erpService.getSectionsForInstructor(userId);
        sectionListModel.clear();
        for (Section s : sections) sectionListModel.addElement(s);
        if (!sections.isEmpty()) sectionList.setSelectedIndex(0);
    }

    private void loadStudentsForSection(int sectionId) {
        studentsTableModel.setRowCount(0);
        List<Enrollment> enrollments = erpService.getEnrollmentsForSection(sectionId);
        for (Enrollment en : enrollments) {
            int enrollmentId = en.getEnrollmentId();
            Optional<Student> st = erpService.getStudentByUserId(en.getStudentId());
            String roll = st.map(Student::getRollNo).orElse("N/A");
            String prog = st.map(Student::getProgram).orElse("N/A");
            String date = en.getEnrollmentDate() == null ? "N/A" : en.getEnrollmentDate().toString();
            String grade = erpService.getGradeForEnrollment(enrollmentId).map(Grade::getGrade).orElse("N/A");
            studentsTableModel.addRow(new Object[]{enrollmentId, en.getStudentId(), roll, prog, date, grade});
        }
    }

    protected void applyMaintenanceState(boolean maintenanceOn, boolean writableForThisUser) {
        boolean writable = writableForThisUser;
        enterGradeBtn.setEnabled(writable);
        // undo is only enabled if writable and stack non-empty
        undoBtn.setEnabled(writable && !undoStack.isEmpty());
        refreshBtn.setEnabled(true);
    }

    // --- Add to InstructorMainFrame.java ---

// If you don't already have a status label, add one as a field near top of class:
// private JLabel statusLabel = new JLabel();

    public JLabel getStatusBar() {
        // Return the status label used by the frame (create statusLabel if missing)
        try {
            return (JLabel) this.getClass().getDeclaredField("statusLabel").get(this);
        } catch (Exception e) {
            // fallback: create a simple label and return
            JLabel fallback = new JLabel();
            return fallback;
        }
    }

    /** Apply maintenance-state changes safely (UI-level: enable/disable write controls) */
    public void applyMaintenanceStateSafe() {
        boolean writable = edu.univ.erp.util.AccessChecker.isWritable();
        // disable or enable grade entry buttons
        if (this.enterGradeBtn != null) this.enterGradeBtn.setEnabled(writable);
        if (this.undoBtn != null) this.undoBtn.setEnabled(writable);
        // update the banner in BaseMainFrame
        if (this instanceof edu.univ.erp.ui.BaseMainFrame) {
            ((edu.univ.erp.ui.BaseMainFrame) this).updateMaintenanceBanner(!writable);
        }
    }

    /** Reload the current section's students into the table */
    public void reloadCurrentSectionStudents() {
        // If you have a loader method like loadStudentsForSection(sectionId) call it.
        // Attempt to call a presumed method; if not present, leave this as a TODO.
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("loadStudentsForSection", int.class);
            // obtain selected section id via existing field or selection
            int sectionId = -1;
            // try to fetch selected section id if you have a variable 'currentSectionId'
            try {
                java.lang.reflect.Field f = this.getClass().getDeclaredField("currentSectionId");
                f.setAccessible(true);
                sectionId = f.getInt(this);
            } catch (Exception ex) {
                // no currentSectionId field; skip
            }
            if (sectionId > 0) m.invoke(this, sectionId);
        } catch (NoSuchMethodException ex) {
            // fallback: do nothing; developer should implement reload logic here
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
