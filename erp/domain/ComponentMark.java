package edu.univ.erp.domain;

import java.math.BigDecimal;

public class ComponentMark {
    private int id;
    private int enrollmentId;
    private int componentId;
    private BigDecimal marks; // numeric marks (raw)

    public ComponentMark() {}

    public ComponentMark(int id, int enrollmentId, int componentId, BigDecimal marks) {
        this.id = id;
        this.enrollmentId = enrollmentId;
        this.componentId = componentId;
        this.marks = marks;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }

    public int getComponentId() { return componentId; }
    public void setComponentId(int componentId) { this.componentId = componentId; }

    public BigDecimal getMarks() { return marks; }
    public void setMarks(BigDecimal marks) { this.marks = marks; }
}
