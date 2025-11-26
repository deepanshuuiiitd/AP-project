package edu.univ.erp.ui;

import edu.univ.erp.domain.GradingComponent;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.service.ErpService;
import edu.univ.erp.service.InstructorService.SectionGradeRow;
import edu.univ.erp.data.ComponentMarksDao;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;
import edu.univ.erp.data.ComponentMarksDao;
import edu.univ.erp.domain.Grade;
import java.math.BigDecimal;


/**
 * GradesFrame (blue theme) — dynamic component columns and weights dialog.
 */
public class GradesFrame extends JFrame {
    private final int instructorId;
    private Integer selectedSectionId = null;
    private final InstructorService service = new InstructorService();
    private final ErpService erpService = new ErpService();

    private JTable table;
    private JComboBox<Section> sectionDropdown;
    // dynamic components list
    private List<GradingComponent> components = new ArrayList<>();

    // colors (blue theme)
    private final Color BLUE = new Color(10, 90, 200);
    private final Color BLUE_DARK = new Color(6, 60, 140);
    private final Color CONTENT_BG = new Color(245, 248, 255);

    public GradesFrame(int instructorId) {
        this.instructorId = instructorId;
        setTitle("Enter Grades");
        setSize(1100, 640);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        /* HEADER */
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setPreferredSize(new Dimension(1000, 64));
        topHeader.setBackground(BLUE);

        JLabel headerTitle = new JLabel("Enter Grades", SwingConstants.LEFT);
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerTitle.setForeground(Color.WHITE);
        headerTitle.setBorder(new EmptyBorder(0, 18, 0, 0));

        JButton exitBtn = new JButton("Exit");
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setBackground(BLUE_DARK);
        exitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        exitBtn.setBorder(new EmptyBorder(8, 16, 8, 16));
        exitBtn.setFocusPainted(false);
        exitBtn.addActionListener(e -> dispose());

        topHeader.add(headerTitle, BorderLayout.WEST);
        topHeader.add(exitBtn, BorderLayout.EAST);
        add(topHeader, BorderLayout.NORTH);

        /* SIDEBAR placeholder */
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(BLUE_DARK);
        add(sidebar, BorderLayout.WEST);

        /* CONTENT */
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        content.setBackground(CONTENT_BG);
        add(content, BorderLayout.CENTER);

        /* CONTROLS ROW */
        JPanel controlRow = new JPanel(new BorderLayout());
        controlRow.setOpaque(false);

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftControls.setOpaque(false);

        JLabel lblSelect = new JLabel("Select Section:");
        lblSelect.setFont(new Font("Segoe UI", Font.BOLD, 13));
        leftControls.add(lblSelect);

        sectionDropdown = new JComboBox<>();
        sectionDropdown.setPreferredSize(new Dimension(320, 28));
        sectionDropdown.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        leftControls.add(sectionDropdown);

        controlRow.add(leftControls, BorderLayout.WEST);

        // right side: compute weights button
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        rightControls.setOpaque(false);
        JButton btnWeights = styledButton("Set / Load Weights");
        rightControls.add(btnWeights);
        controlRow.add(rightControls, BorderLayout.EAST);

        content.add(controlRow, BorderLayout.NORTH);

        /* TABLE: columns will be built dynamically after loading components */
        table = new JTable();
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
        content.add(sp, BorderLayout.CENTER);

        /* BOTTOM buttons */
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottom.setOpaque(false);

        JButton btnLoad = styledButton("Load");
        JButton btnSave = styledButton("Save Components");
        JButton btnCompute = styledButton("Compute Finals");
        JButton btnExport = styledButton("Export CSV");
        JButton btnStats = styledButton("Stats");;
        JButton btnImport = styledButton("Import CSV");

        bottom.add(btnLoad);
        bottom.add(btnSave);
        bottom.add(btnCompute);
        bottom.add(btnExport);
        bottom.add(btnStats);
        bottom.add(btnImport);

        content.add(bottom, BorderLayout.SOUTH);

        // actions
        btnLoad.addActionListener(e -> loadSelectedSectionData());
        btnSave.addActionListener(e -> saveComponentScores());
        btnCompute.addActionListener(e -> computeFinals());
        btnStats.addActionListener(e -> new StatsDialog(this, selectedSectionId).setVisible(true));
        btnExport.addActionListener(this::exportCsvFromTable); // keep existing (basic)
        // after btnWeights is created (replace current no-op / absent wiring)
        btnWeights.addActionListener(e -> {
            // open weights-only dialog (do not compute automatically)
            openWeightsDialog();
        });

        btnExport.addActionListener(e -> { // also provide detailed component export dialog
            try {
                if (selectedSectionId == null) { JOptionPane.showMessageDialog(this, "Select section first"); return; }
                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(new java.io.File("section_" + selectedSectionId + "_component_marks.csv"));
                if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
                java.io.File f = fc.getSelectedFile();
                exportComponentMarksCsv(f, selectedSectionId);
                JOptionPane.showMessageDialog(this, "Exported component marks to " + f.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnImport.addActionListener(e -> {
            if (selectedSectionId == null) { JOptionPane.showMessageDialog(this, "Select a section first"); return; }
            JFileChooser fc = new JFileChooser();
            int rc = fc.showOpenDialog(this);
            if (rc != JFileChooser.APPROVE_OPTION) return;
            java.io.File f = fc.getSelectedFile();
            try {
                importComponentMarksCsv(f, selectedSectionId);
                JOptionPane.showMessageDialog(this, "Import completed successfully.");
                loadTable(); // refresh view to show updated marks / computed finals
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        loadSections();
        loadComponentsListAndBuildTableModel(); // create empty model with component columns
    }

    /**
     * Compute numeric percentage (0..100) for an enrollment given weights and component marks.
     * Returns null if no marks and no weights available.
     */
    /**
     * Compute numeric percentage (0..100) for an enrollment given weights and component marks.
     * Attempts to detect if stored marks are on 0..10 or 0..1 scale and rescales to 0..100.
     */

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(BLUE);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorder(new EmptyBorder(6, 14, 6, 14));
        b.setFocusPainted(false);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { b.setBackground(BLUE_DARK); }
            public void mouseExited(java.awt.event.MouseEvent evt) { b.setBackground(BLUE); }
        });
        return b;
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(26);
        t.setSelectionBackground(BLUE);
        t.setSelectionForeground(Color.WHITE);

        JTableHeader header = t.getTableHeader();
        header.setBackground(BLUE);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);

        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setCellRenderer(center);
    }

    private void loadSections() {
        try {
            sectionDropdown.removeAllItems();
            for (edu.univ.erp.domain.Section s : erpService.getSectionsForInstructor(instructorId)) {
                sectionDropdown.addItem(s);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load sections: " + ex.getMessage());
        }
    }

    private void loadSelectedSectionData() {
        Section sec = (Section) sectionDropdown.getSelectedItem();
        if (sec == null) return;

        selectedSectionId = sec.getSectionId();

        // IMPORTANT: rebuild column model with latest components
        loadComponentsListAndBuildTableModel();

        // Now load marks / finals into new table
        loadTable();
    }


    /** Build table model with dynamic component columns (EnrollmentID, StudentID, <components...>, Final CGPA, Final Grade) */
    private void loadComponentsListAndBuildTableModel() {
        components = service.listAllComponents();
        // build column names
        List<String> cols = new ArrayList<>();
        cols.add("EnrollmentID");
        cols.add("StudentID");
        for (GradingComponent c : components) cols.add(c.getName());
        cols.add("Final CGPA");
        cols.add("Final Grade");

        DefaultTableModel model = new DefaultTableModel(cols.toArray(), 0) {
            @Override public boolean isCellEditable(int row, int col) {
                // allow editing for component columns (indexes 2 .. 2+components.size()-1)
                return (col >= 2 && col < 2 + components.size());
            }

            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 1) return Integer.class;
                return Object.class;
            }
        };

        table.setModel(model);
        styleTable(table);
    }

    private String numericPercentToLetter(double percent) {
        if (percent >= 90.0) return "A";
        if (percent >= 80.0) return "B";
        if (percent >= 70.0) return "C";
        if (percent >= 60.0) return "D";
        return "F";
    }

    private void loadTable() {
        DefaultTableModel m = (DefaultTableModel) table.getModel();
        m.setRowCount(0);
        if (selectedSectionId == null) return;

        try {
            // ensure components list is current (optional)
            // components = service.listAllComponents(); // uncomment if components may change

            // Get weights and marks dao once
            Map<Integer, Integer> weights = service.getSectionWeightsMap(selectedSectionId);
            ComponentMarksDao marksDao = new ComponentMarksDao();

            // Use the service to build rows (enrollmentId + studentId + component map)
            List<SectionGradeRow> rows = service.sectionGrades(selectedSectionId);

            for (SectionGradeRow r : rows) {
                try {
                    // Build object row with the same order as the model: EnrollmentID, StudentID, <components...>, Final CGPA, Final Grade
                    Object[] row = new Object[2 + components.size() + 2];
                    row[0] = r.enrollmentId;
                    row[1] = r.studentId;

                    // Fill component cells: use marks from r.componentMarks if present, otherwise query marksDao
                    int idx = 2;
                    for (GradingComponent comp : components) {
                        Double v = null;
                        if (r.componentMarks != null && r.componentMarks.containsKey(comp.getComponentId())) {
                            v = r.componentMarks.get(comp.getComponentId());
                        } else {
                            // fallback: ask dao
                            try {
                                BigDecimal bd = marksDao.findByEnrollmentAsMap(r.enrollmentId).get(comp.getComponentId());
                                if (bd != null) v = bd.doubleValue();
                            } catch (Exception ignore) {}
                        }
                        row[idx++] = (v == null ? "" : v);
                    }

                    // Compute numeric percent (0..100) using marksDao + weights
                    Double numericPercent = null;
                    try {
                        numericPercent = computeNumericPercentForEnrollment(r.enrollmentId, weights, marksDao);
                    } catch (Exception ex) {
                        // continue with null numericPercent
                        ex.printStackTrace();
                    }

                    // final CGPA display (convert percent -> 0..10 scale and round)
                    if (numericPercent == null) {
                        row[idx++] = ""; // Final CGPA empty
                    } else {
                        double cgpa = numericPercent / 10.0;
                        // round to 2 decimals
                        double cgpaRounded = Math.round(cgpa * 100.0) / 100.0;
                        row[idx++] = cgpaRounded;
                    }

                    // final grade: prefer DB value if present else compute locally
                    String gradeStr = "";
                    try {
                        java.util.Optional<edu.univ.erp.domain.Grade> g = erpService.getGradeForEnrollment(r.enrollmentId);
                        if (g.isPresent() && g.get().getGrade() != null && !g.get().getGrade().trim().isEmpty()) {
                            gradeStr = g.get().getGrade();
                        } else if (numericPercent != null) {
                            gradeStr = numericPercentToLetter(numericPercent);
                        } else {
                            gradeStr = "";
                        }
                    } catch (Exception ex) {
                        // fallback compute
                        if (numericPercent != null) gradeStr = numericPercentToLetter(numericPercent);
                    }
                    row[idx] = gradeStr;

                    m.addRow(row);

                } catch (Exception rowEx) {
                    // Don't abort whole loading on one bad row — log and continue
                    rowEx.printStackTrace();
                }
            }

            // ensure table styling/renderers are applied
            styleTable(table);

        } catch (Exception ex) {
            // show a clear message and log the stack — this prevents silent failures
            JOptionPane.showMessageDialog(this, "Failed to load table: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

// ------------------------- helper computeNumericPercentForEnrollment -------------------------
    /**
     * Compute numeric percent (0..100) for enrollment using weights and marks DAO.
     * Uses auto-scaling heuristic (0..10 -> *10, 0..1 -> *100).
     */
    private Double computeNumericPercentForEnrollment(int enrollmentId, Map<Integer, Integer> weights, ComponentMarksDao marksDao) {
        try {
            if (weights == null || weights.isEmpty()) {
                return null;
            }
            Map<Integer, BigDecimal> marksMap = marksDao.findByEnrollmentAsMap(enrollmentId);
            if (marksMap == null) marksMap = new HashMap<>();

            BigDecimal total = BigDecimal.ZERO;
            for (Map.Entry<Integer, Integer> we : weights.entrySet()) {
                Integer compId = we.getKey();
                int wt = (we.getValue() == null) ? 0 : we.getValue();
                BigDecimal mark = marksMap.getOrDefault(compId, BigDecimal.ZERO);
                total = total.add(mark.multiply(BigDecimal.valueOf(wt)).divide(BigDecimal.valueOf(100)));
            }

            double numeric = total.doubleValue();

            // Auto-scale: if result looks like 0..10 treat as 0..10 scale -> *10, if <=1 treat as fraction -> *100
            if (numeric <= 1.0) numeric = numeric * 100.0;
            else if (numeric <= 10.0) numeric = numeric * 10.0;

            if (numeric < 0) numeric = 0;
            if (numeric > 100) numeric = 100;

            return numeric;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }


    private void saveComponentScores() {
        try {
            DefaultTableModel m = (DefaultTableModel) table.getModel();
            // for each row, collect component values into map and call service.saveComponentScoresGeneric
            for (int r = 0; r < m.getRowCount(); r++) {
                int enrollId = Integer.parseInt(m.getValueAt(r, 0).toString());
                Map<Integer, Double> compMap = new LinkedHashMap<>();
                for (int c = 0; c < components.size(); c++) {
                    Object val = m.getValueAt(r, 2 + c);
                    Double d = parse(val);
                    compMap.put(components.get(c).getComponentId(), d);
                }
                service.saveComponentScoresGeneric(enrollId, compMap);
            }
            JOptionPane.showMessageDialog(this, "Saved component marks.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private Double parse(Object v) {
        if (v == null) return null;
        try { return Double.parseDouble(v.toString()); }
        catch (Exception e) { return null; }
    }

    /** Open the weights dialog, get weights map and compute finals */
    private void openWeightsAndCompute() {
        if (selectedSectionId == null) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }
        Map<Integer, Integer> existing = service.getSectionWeightsMap(selectedSectionId);
        WeightsDialog dlg = new WeightsDialog(this, components, existing);
        dlg.setVisible(true);
        if (!dlg.saved) return;
        Map<Integer, Integer> weights = dlg.getWeightsMap();
        try {
            service.saveSectionWeightsMap(selectedSectionId, weights); // persist
            service.computeAndSaveFinalForSectionGeneric(selectedSectionId, weights);
            JOptionPane.showMessageDialog(this, "Finals computed.");
            loadTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Compute failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /** Open weights-only dialog (no compute) */
    private void openWeightsDialog() {
        if (selectedSectionId == null) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }
        Map<Integer, Integer> existing = service.getSectionWeightsMap(selectedSectionId);
        WeightsDialog dlg = new WeightsDialog(this, components, existing);
        dlg.setVisible(true);
        if (dlg.saved) {
            service.saveSectionWeightsMap(selectedSectionId, dlg.getWeightsMap());
            JOptionPane.showMessageDialog(this, "Weights saved.");
        }
    }

    private String computeLetter(double cg) {
        if (cg >= 9.5) return "A+";
        if (cg >= 9) return "A";
        if (cg >= 8) return "B";
        if (cg >= 7) return "B-";
        if (cg >= 6) return "C";
        if (cg >= 5) return "C-";
        if (cg >= 4) return "D+";
        if (cg >= 3) return "D";
        return "F";
    }

    private void exportCsvFromTable(ActionEvent e) {
        try {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("section_" + selectedSectionId + "_grades.csv"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

            File f = fc.getSelectedFile();
            TableModel m = table.getModel();

            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                for (int c = 0; c < m.getColumnCount(); c++)
                    pw.print((c > 0 ? "," : "") + m.getColumnName(c));
                pw.println();

                for (int r = 0; r < m.getRowCount(); r++) {
                    for (int c = 0; c < m.getColumnCount(); c++)
                        pw.print((c > 0 ? "," : "") + m.getValueAt(r, c));
                    pw.println();
                }
            }

            JOptionPane.showMessageDialog(this, "Exported!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }

    /**
     * Simple modal dialog that lists components and allows editing integer percent weights.
     * Uses JSpinner per component.
     */
    private static class WeightsDialog extends JDialog {
        private final List<GradingComponent> components;
        private final Map<Integer, JSpinner> spinners = new LinkedHashMap<>();
        boolean saved = false;

        WeightsDialog(Frame owner, List<GradingComponent> components, Map<Integer, Integer> initial) {
            super(owner, "Set Section Weights", true);
            this.components = components == null ? Collections.emptyList() : components;

            JPanel body = new JPanel(new BorderLayout(8,8));
            body.setBorder(new EmptyBorder(10,10,10,10));

            JPanel grid = new JPanel(new GridLayout(this.components.size(), 2, 8, 8));
            for (GradingComponent c : this.components) {
                grid.add(new JLabel(c.getName()));
                int val = (initial != null && initial.containsKey(c.getComponentId())) ? initial.get(c.getComponentId()) : 0;
                JSpinner sp = new JSpinner(new SpinnerNumberModel(val, 0, 100, 1));
                spinners.put(c.getComponentId(), sp);
                grid.add(sp);
            }
            body.add(grid, BorderLayout.CENTER);

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton bSave = new JButton("Save");
            JButton bCancel = new JButton("Cancel");
            btns.add(bCancel);
            btns.add(bSave);
            body.add(btns, BorderLayout.SOUTH);

            bSave.addActionListener(e -> {
                int sum = 0;
                for (JSpinner sp : spinners.values()) sum += (Integer) sp.getValue();
                if (sum != 100) {
                    int ok = JOptionPane.showConfirmDialog(this, "Total weights sum to " + sum + " (not 100). Continue?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (ok != JOptionPane.YES_OPTION) return;
                }
                saved = true;
                dispose();
            });

            bCancel.addActionListener(e -> {
                saved = false;
                dispose();
            });

            setContentPane(body);
            pack();
            setLocationRelativeTo(owner);
        }

        Map<Integer, Integer> getWeightsMap() {
            Map<Integer, Integer> out = new LinkedHashMap<>();
            for (Map.Entry<Integer, JSpinner> en : spinners.entrySet()) out.put(en.getKey(), (Integer) en.getValue().getValue());
            return out;
        }
    }

    // --- Helper methods: import/export component-level marks CSV ---

    /**
     * Export component-level marks for all enrollments in sectionId to CSV.
     * CSV columns: EnrollmentID, StudentID, RollNo, <componentName1>, <componentName2>, ...
     */
    private void exportComponentMarksCsv(java.io.File outFile, int sectionId) throws Exception {
        // load all grading components (master list)
        edu.univ.erp.data.GradingComponentDao compDao = new edu.univ.erp.data.GradingComponentDao();
        java.util.List<edu.univ.erp.domain.GradingComponent> components = compDao.listAll();

        // header
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(outFile))) {
            // Build header
            StringBuilder hdr = new StringBuilder();
            hdr.append("EnrollmentID,StudentID,RollNo");
            for (edu.univ.erp.domain.GradingComponent c : components) {
                hdr.append(",").append(c.getName());
            }
            pw.println(hdr.toString());

            // For each enrollment in section, fetch component marks
            edu.univ.erp.service.InstructorService svc = new edu.univ.erp.service.InstructorService();
            java.util.List<edu.univ.erp.service.InstructorService.SectionGradeRow> rows = svc.sectionGrades(sectionId);
            // sectionGrades should return one row per enrollment with enrollmentId + studentId + component fields (if present).
            // If your SectionGradeRow doesn't include dynamic components, we'll fall back to using ComponentMarksDao.

            // Fallback approach: use ComponentMarksDao to get marks map per enrollment
            edu.univ.erp.data.ComponentMarksDao marksDao = new edu.univ.erp.data.ComponentMarksDao();
            for (edu.univ.erp.service.InstructorService.SectionGradeRow r : rows) {
                int enrollId = r.enrollmentId;
                int studentId = r.studentId;
                // attempt roll no via ErpService
                String roll = "";
                try {
                    java.util.Optional<edu.univ.erp.domain.Student> st = new edu.univ.erp.service.ErpService().getStudentByUserId(studentId);
                    if (st.isPresent()) roll = st.get().getRollNo();
                } catch (Exception ignore) {}

                java.util.Map<Integer, java.math.BigDecimal> marksMap = marksDao.findByEnrollmentAsMap(enrollId);
                StringBuilder row = new StringBuilder();
                row.append(enrollId).append(",").append(studentId).append(",").append(escapeCsv(roll));
                for (edu.univ.erp.domain.GradingComponent c : components) {
                    java.math.BigDecimal val = marksMap.get(c.getComponentId());
                    row.append(",");
                    if (val != null) row.append(val.toPlainString());
                }
                pw.println(row.toString());
            }
        }
    }

    /**
     * Import component-level marks CSV.
     * Requirement: first column EnrollmentID (numeric). Columns after first three map to component names.
     * Any non-numeric mark will be ignored for that cell.
     */
    private void importComponentMarksCsv(java.io.File csvFile, int sectionId) throws Exception {
        // load components and map name->id
        edu.univ.erp.data.GradingComponentDao compDao = new edu.univ.erp.data.GradingComponentDao();
        java.util.List<edu.univ.erp.domain.GradingComponent> comps = compDao.listAll();
        java.util.Map<String, Integer> compNameToId = new java.util.HashMap<>();
        for (edu.univ.erp.domain.GradingComponent c : comps) compNameToId.put(c.getName().trim().toLowerCase(), c.getComponentId());

        edu.univ.erp.data.ComponentMarksDao marksDao = new edu.univ.erp.data.ComponentMarksDao();

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
            String header = br.readLine();
            if (header == null) throw new RuntimeException("CSV is empty");
            String[] cols = splitCsvLine(header);

            // Identify component columns and their component_id
            // We expect first three columns to be EnrollmentID, StudentID, RollNo (StudentID/RollNo optional)
            int colCount = cols.length;
            java.util.List<Integer> compColumnToId = new java.util.ArrayList<>();
            for (int i = 0; i < colCount; i++) {
                if (i <= 2) { compColumnToId.add(null); continue; } // placeholders
                String cname = cols[i].trim().toLowerCase();
                Integer cid = compNameToId.get(cname);
                compColumnToId.add(cid); // will be null if component name not found
            }

            String line;
            int lineNo = 1;
            java.util.List<String> errors = new java.util.ArrayList<>();
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                String[] values = splitCsvLine(line);
                if (values.length == 0) continue;
                // parse enrollment id
                String enrollStr = values[0].trim();
                int enrollmentId;
                try {
                    enrollmentId = Integer.parseInt(enrollStr);
                } catch (NumberFormatException nfe) {
                    errors.add("Line " + lineNo + ": invalid EnrollmentID '" + enrollStr + "'");
                    continue;
                }

                // optional: verify this enrollment belongs to the sectionId
                try {
                    boolean belongs = false;
                    java.util.List<edu.univ.erp.domain.Enrollment> enrollments = new edu.univ.erp.service.ErpService().getEnrollmentsForSection(sectionId);
                    for (edu.univ.erp.domain.Enrollment e : enrollments) {
                        if (e.getEnrollmentId() == enrollmentId) { belongs = true; break; }
                    }
                    if (!belongs) {
                        errors.add("Line " + lineNo + ": Enrollment " + enrollmentId + " does not belong to section " + sectionId);
                        continue;
                    }
                } catch (Exception ex) {
                    // if ErpService check fails, skip the check (still attempt import)
                }

                // iterate component columns
                for (int ci = 3; ci < compColumnToId.size() && ci < values.length; ci++) {
                    Integer compId = compColumnToId.get(ci);
                    if (compId == null) continue; // unknown component name — skip
                    String val = values[ci].trim();
                    if (val.isEmpty()) continue; // skip blank
                    try {
                        java.math.BigDecimal marks = new java.math.BigDecimal(val);
                        // upsert marks
                        marksDao.upsert(enrollmentId, compId, marks);
                    } catch (NumberFormatException nfe) {
                        errors.add("Line " + lineNo + ", col " + (ci+1) + ": invalid numeric '" + val + "'");
                    }
                }
            }

            if (!errors.isEmpty()) {
                // show aggregated errors to the user in a dialog
                StringBuilder sb = new StringBuilder();
                sb.append("Import completed with warnings/errors:\n");
                for (String err : errors) {
                    sb.append(err).append("\n");
                }
                JOptionPane.showMessageDialog(this, sb.toString(), "Import Warnings", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /** small CSV helpers (simple, handles commas, no quotes heavy parsing) */
    private String[] splitCsvLine(String line) {
        // naive splitting that preserves empty columns
        return line.split(",", -1);
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
    /**
     * Compute finals for the currently selected section using spinner weights.
     * This is the computeFinals() referenced by the buttons/listeners.
     */
    /**
     * Compute finals for the currently selected section.
     * Uses saved section weights (service.getSectionWeightsMap). If no weights are found,
     * prompts the user to open the weights dialog (openWeightsAndCompute()).
     */
    private void computeFinals() {
        if (selectedSectionId == null) {
            JOptionPane.showMessageDialog(this, "Select a section first", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            // 1) load current weights for the section (componentId -> percent)
            Map<Integer, Integer> weights = service.getSectionWeightsMap(selectedSectionId);

            // 2) if no weights found, prompt user to set them (open weights dialog)
            if (weights == null || weights.isEmpty()) {
                // open weights dialog with current components (dialog will let user save)
                WeightsDialog dlg = new WeightsDialog(this, components, weights);
                dlg.setVisible(true);
                if (!dlg.saved) {
                    // user cancelled -> abort compute
                    JOptionPane.showMessageDialog(this, "No weights defined. Compute cancelled.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                // save weights from dialog
                weights = dlg.getWeightsMap();
                service.saveSectionWeightsMap(selectedSectionId, weights);
            }

            // 3) validate sum of weights (service expects sum==100)
            int sum = 0;
            for (Integer w : weights.values()) sum += (w == null ? 0 : w);
            if (sum != 100) {
                int ok = JOptionPane.showConfirmDialog(this,
                        "Weights sum to " + sum + " (not 100). Continue and treat remaining as 0?",
                        "Confirm weight sum", JOptionPane.YES_NO_OPTION);
                if (ok != JOptionPane.YES_OPTION) {
                    return;
                }
                // If the service enforces sum==100 and throws, you may want to normalize or prevent calling it.
            }

            // 4) call the existing InstructorService compute method you have:
            service.computeAndSaveFinalForSectionGeneric(selectedSectionId, weights);

            // 5) reload UI from DB (sectionGrades returns component marks; gradeDao stores letter grade)
            loadTable();

            JOptionPane.showMessageDialog(this, "Finals computed and grades persisted.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ia) {
            JOptionPane.showMessageDialog(this, "Compute failed: " + ia.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Compute failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

}
