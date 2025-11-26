package edu.univ.erp.domain;

public class Grade {
    private final int gradeId;
    private final int enrollmentId;
    private final String grade;

    public Grade(int gradeId, int enrollmentId, String grade) {
        this.gradeId = gradeId;
        this.enrollmentId = enrollmentId;
        this.grade = grade;
    }

    public int getGradeId() { return gradeId; }
    public int getEnrollmentId() { return enrollmentId; }
    public String getGrade() { return grade; }

    @Override
    public String toString() { return grade; }
}
