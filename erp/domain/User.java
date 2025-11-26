package edu.univ.erp.domain;

public class User {
    private final Integer userId; // null for new user until inserted
    private String username;
    private String passwordHash; // stored hash
    private String role; // ADMIN, INSTRUCTOR, STUDENT

    public User(Integer userId, String username, String passwordHash, String role) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Integer getUserId() { return userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return (userId == null ? "(new)" : userId) + " - " + username + " [" + role + "]";
    }
}
