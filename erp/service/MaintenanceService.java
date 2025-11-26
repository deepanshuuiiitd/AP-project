package edu.univ.erp.service;

import edu.univ.erp.data.SettingsDao;

public class MaintenanceService {
    private final SettingsDao settingsDao = new SettingsDao();

    public boolean isMaintenanceOn() {
        return settingsDao.isMaintenanceOn();
    }

    public void setMaintenance(boolean on) {
        try {
            settingsDao.setSetting("maintenance_mode", on ? "ON" : "OFF");
        } catch (Exception ex) {
            throw new RuntimeException("Failed to set maintenance flag: " + ex.getMessage(), ex);
        }
    }

    // ============================================================
    // Enrollment Deadline APIs
    // ============================================================

    public java.time.LocalDate getEnrollmentDeadline() {
        return settingsDao.getEnrollmentDeadline();
    }

    public void setEnrollmentDeadline(java.time.LocalDate d) {
        settingsDao.setEnrollmentDeadline(d);
    }

}
