package edu.univ.erp.domain;

public class Section {
    private final int sectionId;
    private final int courseId;
    private final int instructorId;
    private final String semester;
    private final int year;

    public Section(int sectionId, int courseId, int instructorId, String semester, int year) {
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.instructorId = instructorId;
        this.semester = semester;
        this.year = year;
    }

    public int getSectionId() { return sectionId; }
    public int getCourseId() { return courseId; }
    public int getInstructorId() { return instructorId; }
    public String getSemester() { return semester; }
    public int getYear() { return year; }

    @Override
    public String toString() {
        return "Section " + sectionId + " (" + semester + " " + year + ")";
    }
}
