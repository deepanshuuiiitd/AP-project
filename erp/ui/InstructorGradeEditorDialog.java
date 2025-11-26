package edu.univ.erp.ui;

import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.GradingComponent;
import edu.univ.erp.domain.SectionGradeWeight;
import edu.univ.erp.service.ErpService;
import edu.univ.erp.service.GradeService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Dialog for instructors to set weights and enter marks for a section.
 * Updated to avoid calling Enrollment.getStudent() (not present in user's Enrollment class).
 */
public class InstructorGradeEditorDialog extends JDialog {
    private final GradeService gradeService = new GradeService();
    private final ErpService erpService = new ErpService(); // to fetch enrollments
    private final int sectionId;
    private final String sectionLabel; // display

    private final DefaultTableModel weightsModel;
    private final JTable weightsTable;

    // Marks table
    private final DefaultTableModel marksModel;
    private final JTable marksTable;

    public InstructorGradeEditorDialog(Frame owner, int sectionId, String sectionLabel) {
        super(owner, "Manage Grading — " + sectionLabel, true);
        this.sectionId = sectionId;
        this.sectionLabel = sectionLabel;
        setSize(900, 600);
        setLocationRelativeTo(owner);
        setLayout(new MigLayout("wrap 1", "[grow]", "[][grow][]"));

        // Top: weights editor
        JPanel wpanel = new JPanel(new BorderLayout(6,6));
        wpanel.setBorder(BorderFactory.createTitledBorder("Component Weights (sum must be 100%)"));

        weightsModel = new DefaultTableModel(new String[]{"Component ID", "Component", "Weight (%)"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return col == 2; }
        };
        weightsTable = new JTable(weightsModel);
        wpanel.add(new JScrollPane(weightsTable), BorderLayout.CENTER);

        JButton saveWeights = new JButton("Save Weights");
        wpanel.add(saveWeights, BorderLayout.SOUTH);

        add(wpanel, "growx");

        // Middle: marks editor
        JPanel mpanel = new JPanel(new BorderLayout(6,6));
        mpanel.setBorder(BorderFactory.createTitledBorder("Enter Student Marks (per component)"));
        marksModel = new DefaultTableModel();
        marksTable = new JTable(marksModel);
        mpanel.add(new JScrollPane(marksTable), BorderLayout.CENTER);

        JPanel mbtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveMarksBtn = new JButton("Save Marks");
        mbtnPanel.add(saveMarksBtn);
        mpanel.add(mbtnPanel, BorderLayout.SOUTH);

        add(mpanel, "grow, push");

        // Load data
        loadWeights();
        loadMarksTable();

        // Save actions
        saveWeights.addActionListener(e -> {
            try {
                BigDecimal sum = BigDecimal.ZERO;
                for (int r=0;r<weightsModel.getRowCount();r++) {
                    Object val = weightsModel.getValueAt(r,2);
                    BigDecimal w = new BigDecimal(val == null ? "0" : val.toString());
                    sum = sum.add(w);
                }
                if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
                    JOptionPane.showMessageDialog(this, "Total weight must equal 100 (currently " + sum + ")", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // persist each row
                for (int r=0;r<weightsModel.getRowCount();r++) {
                    int compId = Integer.parseInt(weightsModel.getValueAt(r,0).toString());
                    BigDecimal w = new BigDecimal(weightsModel.getValueAt(r,2).toString());
                    gradeService.saveWeight(sectionId, compId, w);
                }
                JOptionPane.showMessageDialog(this, "Weights saved", "Saved", JOptionPane.INFORMATION_MESSAGE);
                loadMarksTable(); // refresh marks table to reflect component columns
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        saveMarksBtn.addActionListener(e -> {
            try {
                // marksModel columns: EnrollmentId, Student (label), compId...
                for (int r=0;r<marksModel.getRowCount();r++) {
                    Integer enrollId = (Integer) marksModel.getValueAt(r,0);
                    for (int c=2;c<marksModel.getColumnCount();c++) {
                        String colName = marksModel.getColumnName(c); // componentId as string
                        int compId = Integer.parseInt(colName);
                        Object val = marksModel.getValueAt(r,c);
                        BigDecimal mark = null;
                        if (val != null && !val.toString().trim().isEmpty()) mark = new BigDecimal(val.toString());
                        gradeService.saveMark(enrollId, compId, mark);
                    }
                }
                JOptionPane.showMessageDialog(this, "Marks saved", "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to save marks: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void loadWeights() {
        weightsModel.setRowCount(0);
        List<GradingComponent> comps = gradeService.listComponents();
        List<SectionGradeWeight> weights = gradeService.getWeightsForSection(sectionId);
        Map<Integer, SectionGradeWeight> map = new java.util.HashMap<>();
        for (SectionGradeWeight w : weights) map.put(w.getComponentId(), w);

        for (GradingComponent gc : comps) {
            SectionGradeWeight sw = map.get(gc.getComponentId());
            String weight = sw == null ? "0" : sw.getWeight().toPlainString();
            weightsModel.addRow(new Object[]{gc.getComponentId(), gc.getName(), weight});
        }
    }

    private void loadMarksTable() {
        // columns: enrollment_id, student label, then one column per component used by this section
        List<SectionGradeWeight> weights = gradeService.getWeightsForSection(sectionId);
        // if no weights set, still show components
        List<GradingComponent> comps = gradeService.listComponents();

        // Determine columns (use all components for now)
        java.util.List<Integer> compIds = new java.util.ArrayList<>();
        for (GradingComponent g : comps) compIds.add(g.getComponentId());

        // Setup model
        marksModel.setRowCount(0);
        Vector<String> cols = new Vector<>();
        cols.add("EnrollmentId");
        cols.add("Student");
        for (Integer id : compIds) cols.add(id.toString()); // use componentId as column name
        marksModel.setColumnIdentifiers(cols);

        // Fetch enrollments for this section
        List<Enrollment> enrolls = erpService.getEnrollmentsForSection(sectionId);
        for (Enrollment en : enrolls) {
            Vector<Object> row = new Vector<>();
            row.add(en.getEnrollmentId());

            // Use only studentId because Enrollment doesn't expose Student object
            String studentLabel = "Student " + en.getStudentId();
            row.add(studentLabel);

            Map<Integer, BigDecimal> marks = gradeService.getMarksForEnrollment(en.getEnrollmentId());
            for (Integer cid : compIds) {
                BigDecimal m = marks.get(cid);
                row.add(m == null ? null : m);
            }
            marksModel.addRow(row);
        }

        // Optional: small column sizing
        if (marksTable.getColumnModel().getColumnCount() > 0) {
            marksTable.getColumnModel().getColumn(0).setMaxWidth(100);
            marksTable.getColumnModel().getColumn(1).setMinWidth(180);
        }
    }
}
