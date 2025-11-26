package edu.univ.erp.domain;

public class GradingComponent {
    private int componentId;
    private String name;

    public GradingComponent() {}

    public GradingComponent(int componentId, String name) {
        this.componentId = componentId;
        this.name = name;
    }

    public int getComponentId() { return componentId; }
    public void setComponentId(int componentId) { this.componentId = componentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() { return name; }
}
