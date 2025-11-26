package edu.univ.erp.ui;

import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.service.ErpService;
import edu.univ.erp.service.GradeService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * A panel to show student's grade breakdown across enrollments.
 * Usage: new StudentGradesPanel(loggedInUserId)
 */
public class StudentGradesPanel extends JPanel {
    private final ErpService erpService = new ErpService();
    private final GradeService gradeService = new GradeService();
    private final int userId;
    private final DefaultTableModel model;
    private final JTable table;

    public StudentGradesPanel(int userId) {
        this.userId = userId;
        setLayout(new MigLayout("fill", "[grow]", "[][grow]"));
        add(new JLabel("My Enrollments & Grades"), "wrap");

        model = new DefaultTableModel();
        table = new JTable(model);
        UIUtil.decorateTable(table); // style

        add(new JScrollPane(table), "grow, push");

        refresh();
    }

    public void refresh() {
        // We'll create columns: Section, component1, component2..., Total, Letter
        List<Enrollment> enrolls = erpService.getEnrollmentsForStudent(userId);
        // get component list (global) - but show only those used by section
        List<edu.univ.erp.domain.GradingComponent> comps = gradeService.listComponents();

        // Build columns dynamically: Section | <comp names>... | Total | Letter
        java.util.List<String> cols = new java.util.ArrayList<>();
        cols.add("Section");
        for (edu.univ.erp.domain.GradingComponent c : comps) cols.add(c.getName());
        cols.add("Total");
        cols.add("Grade");
        model.setColumnIdentifiers(cols.toArray());

        model.setRowCount(0);
        for (Enrollment en : enrolls) {
            java.util.List<Object> row = new java.util.ArrayList<>();
            String sectionLabel = "Section " + en.getSectionId();
            row.add(sectionLabel);
            Map<Integer, BigDecimal> marks = gradeService.getMarksForEnrollment(en.getEnrollmentId());
            for (edu.univ.erp.domain.GradingComponent c : comps) {
                BigDecimal m = marks.get(c.getComponentId());
                row.add(m == null ? "" : m.toPlainString());
            }
            BigDecimal total = gradeService.computeWeightedTotal(en.getEnrollmentId(), en.getSectionId());
            row.add(total.toPlainString());
            row.add(gradeService.numericToLetter(total));
            model.addRow(row.toArray());
        }
    }
    public JTable getTable() {
        return table;
    }

}
