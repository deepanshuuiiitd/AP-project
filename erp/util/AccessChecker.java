package edu.univ.erp.util;

import edu.univ.erp.domain.User;
import edu.univ.erp.service.MaintenanceModeException;
import edu.univ.erp.service.MaintenanceService;

/**
 * Centralized access checks.
 *
 * - Keeps existing backward-compatible static methods used by older code.
 * - Adds new APIs that accept the current User so admins can still perform writes during maintenance.
 */
public final class AccessChecker {

    // re-use your existing service; ok for simple single-node desktop app
    private static final MaintenanceService maintenanceService = new MaintenanceService();

    /**
     * Legacy API (keeps previous behavior for existing callers).
     * Throws if maintenance is ON (no user context available so admin can't be allowed).
     */
    public static void checkWritableOrThrow() {
        if (maintenanceService.isMaintenanceOn()) {
            // prefer throwing the project's MaintenanceModeException for clearer handling
            throw new MaintenanceModeException("System is in Maintenance mode. Only Admin can perform write operations.");
        }
    }

    /**
     * Legacy boolean check.
     */
    public static boolean isWritable() {
        return !maintenanceService.isMaintenanceOn();
    }

    // -------------------------
    // New recommended APIs
    // -------------------------

    /**
     * Call this at the start of any mutating operation and pass the User performing the action.
     * If maintenance is ON and the user is not ADMIN, this throws MaintenanceModeException.
     *
     * Example usage:
     *    AccessChecker.checkWriteAllowed(currentUser);
     */
    public static void checkWriteAllowed(User user) {
        if (maintenanceService.isMaintenanceOn()) {
            if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
                throw new MaintenanceModeException("System is under maintenance. Write actions are disabled for non-admin users.");
            }
        }
    }

    /**
     * Boolean version: can this user perform write actions now?
     */
    public static boolean canWrite(User user) {
        return !maintenanceService.isMaintenanceOn() || (user != null && "ADMIN".equalsIgnoreCase(user.getRole()));
    }
}
