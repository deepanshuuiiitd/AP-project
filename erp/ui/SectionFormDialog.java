package edu.univ.erp.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class SectionFormDialog extends JDialog {
    private final JTextField courseIdField = new JTextField(6);
    private final JTextField instructorIdField = new JTextField(6);
    private final JTextField semesterField = new JTextField(10);
    private final JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(2023, 2000, 2100, 1));
    private boolean saved = false;

    public SectionFormDialog(JFrame owner, String title) {
        super(owner, title, true);
        init();
    }

    private void init() {
        setLayout(new MigLayout("wrap 2", "[][grow]", "[][][]"));
        add(new JLabel("Course ID:"));
        add(courseIdField, "growx");
        add(new JLabel("Instructor User ID:"));
        add(instructorIdField, "growx");
        add(new JLabel("Semester:"));
        add(semesterField, "growx");
        add(new JLabel("Year:"));
        add(yearSpinner, "growx");

        JPanel btns = new JPanel();
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        btns.add(save);
        btns.add(cancel);
        add(btns, "span, center");

        save.addActionListener(e -> {
            if (courseIdField.getText().trim().isEmpty() || instructorIdField.getText().trim().isEmpty()
                    || semesterField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Course ID, Instructor ID and Semester are required", "Validation", JOptionPane.WARNING_MESSAGE);
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

    public void setSectionValues(int courseId, int instructorId, String semester, int year) {
        courseIdField.setText(String.valueOf(courseId));
        instructorIdField.setText(String.valueOf(instructorId));
        semesterField.setText(semester);
        yearSpinner.setValue(year);
    }

    public int getCourseId() { return Integer.parseInt(courseIdField.getText().trim()); }
    public int getInstructorId() { return Integer.parseInt(instructorIdField.getText().trim()); }
    public String getSemester() { return semesterField.getText().trim(); }
    public int getYear() { return (Integer) yearSpinner.getValue(); }
    public boolean isSaved() { return saved; }
    public JTextField getCourseIdField() { return courseIdField; }
}
