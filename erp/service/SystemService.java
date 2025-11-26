package edu.univ.erp.service;

import edu.univ.erp.data.SettingsDao;

import java.util.Optional;

public class SystemService {

    private final SettingsDao settingsDao = new SettingsDao();

    public boolean isMaintenanceMode() {
        try {
            return Optional.ofNullable(settingsDao.findByKey("maintenance"))
                    .map(v -> "ON".equalsIgnoreCase(v))
                    .orElse(false);

        } catch (Exception ex) {
            ex.printStackTrace();
            // If DB reading fails, be conservative and allow only admin — but return true so login will be blocked for non-admins.
            return true;
        }
    }

    public boolean setMaintenanceMode(boolean on) {
        try {
            if (settingsDao.findByKey("maintenance") != null) {
                settingsDao.updateValue("maintenance", on ? "ON" : "OFF");
            } else {
                settingsDao.insert("maintenance", on ? "ON" : "OFF");
            }

            // verify by re-reading
            String val = settingsDao.findByKey("maintenance");
            boolean actual = "ON".equalsIgnoreCase(val);

            System.out.println("[SystemService] requested -> " + (on ? "ON" : "OFF")
                    + " ; actual DB -> " + (actual ? "ON" : "OFF"));

            return actual;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to set maintenance mode: " + ex.getMessage(), ex);
        }
    }


}
