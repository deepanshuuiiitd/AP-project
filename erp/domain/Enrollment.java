package edu.univ.erp.domain;

import java.sql.Date;

public class Enrollment {
    private final int enrollmentId;
    private final int studentId;
    private final int sectionId;
    private final Date enrollmentDate;

    public Enrollment(int enrollmentId, int studentId, int sectionId, Date enrollmentDate) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.sectionId = sectionId;
        this.enrollmentDate = enrollmentDate;
    }

    public int getEnrollmentId() { return enrollmentId; }
    public int getStudentId() { return studentId; }
    public int getSectionId() { return sectionId; }
    public Date getEnrollmentDate() { return enrollmentDate; }

    @Override
    public String toString() {
        return "Enrollment " + enrollmentId + " student:" + studentId + " section:" + sectionId;
    }
}
