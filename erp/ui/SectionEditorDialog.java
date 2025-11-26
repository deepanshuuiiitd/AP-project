package edu.univ.erp.ui;

import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.ErpService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SectionEditorDialog extends JDialog {

    private final ErpService erp = new ErpService();
    private final JComboBox<User> instructorCombo;
    private final JTextField semesterField = new JTextField(10);
    private final JSpinner yearSpinner =
            new JSpinner(new SpinnerNumberModel(2023, 2000, 2100, 1));

    private boolean saved = false;
    private final Section existing;
    private final int courseId;

    public SectionEditorDialog(Frame owner, Section existing, int courseId, List<User> instructors) {
        super(owner, existing == null ? "Add Section" : "Edit Section", true);
        this.existing = existing;
        this.courseId = courseId;
        this.instructorCombo = new JComboBox<>(instructors.toArray(new User[0]));
        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new MigLayout("wrap 2", "[right][grow]"));

        add(new JLabel("Instructor:"));
        add(instructorCombo, "growx");

        add(new JLabel("Semester:"));
        add(semesterField, "growx");

        add(new JLabel("Year:"));
        add(yearSpinner, "growx");

        if (existing != null) {
            semesterField.setText(existing.getSemester());
            yearSpinner.setValue(existing.getYear());

            // pre-select instructor
            for (int i = 0; i < instructorCombo.getItemCount(); i++) {
                if (instructorCombo.getItemAt(i).getUserId() == existing.getInstructorId()) {
                    instructorCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        JButton save = UIUtil.createRoundedButton("Save");
        JButton cancel = UIUtil.createGhostButton("Cancel");
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.add(save);
        p.add(cancel);
        add(p, "span");

        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());
    }

    private void onSave() {
        User sel = (User) instructorCombo.getSelectedItem();
        if (sel == null) {
            JOptionPane.showMessageDialog(this, "Select instructor");
            return;
        }
        String sem = semesterField.getText().trim();
        int year = (int) yearSpinner.getValue();

        if (sem.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter semester");
            return;
        }

        try {
            if (existing == null) {
                erp.addSection(courseId, sel.getUserId(), sem, year);
            } else {
                erp.updateSection(existing.getSectionId(),
                        courseId, sel.getUserId(), sem, year);
            }
            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }

    public boolean isSaved() { return saved; }
}
