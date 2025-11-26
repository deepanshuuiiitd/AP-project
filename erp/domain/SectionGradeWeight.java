package edu.univ.erp.domain;

import java.math.BigDecimal;

public class SectionGradeWeight {
    private int id;
    private int sectionId;
    private int componentId;
    private BigDecimal weight; // percentage (0..100)

    public SectionGradeWeight() {}

    public SectionGradeWeight(int id, int sectionId, int componentId, BigDecimal weight) {
        this.id = id;
        this.sectionId = sectionId;
        this.componentId = componentId;
        this.weight = weight;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSectionId() { return sectionId; }
    public void setSectionId(int sectionId) { this.sectionId = sectionId; }

    public int getComponentId() { return componentId; }
    public void setComponentId(int componentId) { this.componentId = componentId; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
}
