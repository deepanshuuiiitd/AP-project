package edu.univ.erp.ui;

import edu.univ.erp.domain.Course;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class CourseFormDialog extends JDialog {
    private final JTextField codeField = new JTextField(10);
    private final JTextField titleField = new JTextField(30);
    private final JSpinner creditsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
    private boolean saved = false;

    public CourseFormDialog(JFrame owner, String title) {
        super(owner, title, true);
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap 2", "[][grow]", "[][][]"));
        add(new JLabel("Code:"));
        add(codeField, "growx");
        add(new JLabel("Title:"));
        add(titleField, "growx");
        add(new JLabel("Credits:"));
        add(creditsSpinner, "growx");

        JPanel btns = new JPanel();
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        btns.add(save);
        btns.add(cancel);
        add(btns, "span, center");

        save.addActionListener(e -> {
            if (codeField.getText().trim().isEmpty() || titleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Code and Title are required", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            saved = true;
            setVisible(false);
        });
        cancel.addActionListener(e -> {
            saved = false;
            setVisible(false);
        });

        pack();
        setLocationRelativeTo(getOwner());
    }

    public void setCourse(Course c) {
        if (c == null) return;
        codeField.setText(c.getCode());
        titleField.setText(c.getTitle());
        creditsSpinner.setValue(c.getCredits());
    }

    public String getCode() { return codeField.getText().trim(); }
    public String getTitle() { return titleField.getText().trim(); }
    public int getCredits() { return (Integer) creditsSpinner.getValue(); }
    public boolean isSaved() { return saved; }
}
