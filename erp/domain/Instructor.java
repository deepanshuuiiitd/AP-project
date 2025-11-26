package edu.univ.erp.domain;

public class Instructor {
    private final int userId;
    private final String department;

    public Instructor(int userId, String department) {
        this.userId = userId;
        this.department = department;
    }

    public int getUserId() { return userId; }
    public String getDepartment() { return department; }

    @Override
    public String toString() { return "Instructor " + userId + " (" + department + ")"; }
}
