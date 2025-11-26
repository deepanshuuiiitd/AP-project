package edu.univ.erp.ui;

import edu.univ.erp.domain.Course;
import edu.univ.erp.service.ErpService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog to add or edit a course (code, title, credits)
 */
public class CourseEditorDialog extends JDialog {
    private final ErpService erp = new ErpService();
    private final JTextField codeField = new JTextField(12);
    private final JTextField titleField = new JTextField(24);
    private final JSpinner creditsSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 10, 1));
    private boolean saved = false;
    private final Course existing;

    public CourseEditorDialog(Frame owner, Course existing) {
        super(owner, existing == null ? "Add Course" : "Edit Course", true);
        this.existing = existing;
        initUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        setLayout(new MigLayout("wrap 2", "[right][grow]"));

        add(new JLabel("Code:"));
        add(codeField, "growx");

        add(new JLabel("Title:"));
        add(titleField, "growx");

        add(new JLabel("Credits:"));
        add(creditsSpinner, "growx");

        if (existing != null) {
            codeField.setText(existing.getCode());
            titleField.setText(existing.getTitle());
            creditsSpinner.setValue(existing.getCredits());
        }

        JButton save = UIUtil.createRoundedButton("Save");
        JButton cancel = UIUtil.createGhostButton("Cancel");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(save);
        btns.add(cancel);
        add(btns, "span");

        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> onCancel());
    }

    private void onSave() {
        String code = codeField.getText().trim();
        String title = titleField.getText().trim();
        int credits = (Integer) creditsSpinner.getValue();

        if (code.isEmpty() || title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill required fields", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (existing == null) {
                erp.addCourse(code, title, credits);
            } else {
                erp.updateCourse(existing.getCourseId(), code, title, credits);
            }
            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() { saved = false; dispose(); }
    public boolean isSaved() { return saved; }
}
