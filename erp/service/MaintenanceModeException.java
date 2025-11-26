package edu.univ.erp.service;

/**
 * Thrown when a mutating action is attempted while system is in maintenance mode.
 */
public class MaintenanceModeException extends RuntimeException {
    public MaintenanceModeException(String message) {
        super(message);
    }
}
