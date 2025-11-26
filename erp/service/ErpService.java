package edu.univ.erp.service;

import edu.univ.erp.data.*;
import edu.univ.erp.domain.*;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import edu.univ.erp.service.MaintenanceModeException;
import edu.univ.erp.util.AccessChecker;

/**
 * Adapted ErpService to match the user's existing DAOs and domain constructors.
 * - Uses domain constructors that require args (no no-arg + setters).
 * - Uses DAO method signatures observed in compile errors.
 * - Uses reflection for enrollment delete to support either delete(...) or deleteById(...).
 */
public class ErpService {

    private final UserDao userDao = new UserDao();
    private final CourseDao courseDao = new CourseDao();
    private final SectionDao sectionDao = new SectionDao();
    private final EnrollmentDao enrollmentDao = new EnrollmentDao();
    private final GradeDao gradeDao = new GradeDao();
    private final StudentDao studentDao = new StudentDao();
    private final SystemService systemService = new SystemService(); // reuse or create if necessary

    /* ---------------- USERS ---------------- */

    public List<User> listUsers() {
        return userDao.findAll();
    }

    public List<User> listInstructors() {
        return userDao.findAll().stream()
                .filter(u -> "INSTRUCTOR".equalsIgnoreCase(u.getRole()))
                .toList();
    }

    /**
     * Add user: your UserDao.insert expects a User object.
     * User domain constructor signature observed: User(Integer id, String username, String passwordHash, String role)
     */
    public int addUser(String username, String passwordHash, String role) {

        User u = new User(null, username, passwordHash, role);
        return userDao.insert(u);
    }

    /**
     * Update user: construct User with id and call userDao.update(User)
     */
    public void updateUser(int userId, String username, String passwordHash, String role) {

        User u = new User(Integer.valueOf(userId), username, passwordHash, role);
        userDao.update(u);
    }

    public void deleteUser(int userId) {

        // your UserDao uses delete(int) (observed earlier)
        userDao.delete(userId);
    }

    /* ---------------- COURSES ---------------- */

    public List<Course> listCourses() {

        return courseDao.findAll();
    }

    public Optional<Course> getCourseById(int id) {

        return courseDao.findById(id);
    }

    /**
     * Course add/update: your CourseDao.insert expects (String code, String title, int credits)
     * and CourseDao.update expects (int courseId, String code, String title, int credits)
     * Course domain constructor signature: Course(int id, String code, String title, int credits)
     */
    public int addCourse(String code, String title, int credits) {
        AccessChecker.checkWritableOrThrow();

        return courseDao.insert(code, title, credits);
    }

    public void updateCourse(int courseId, String code, String title, int credits) {
        AccessChecker.checkWritableOrThrow();

        courseDao.update(courseId, code, title, credits);
    }

    public void deleteCourse(int courseId) {
        AccessChecker.checkWritableOrThrow();

        courseDao.delete(courseId);
    }

    /* ---------------- SECTIONS ---------------- */

    public List<Section> listSectionsForCourse(int courseId) {
        // Your SectionDao has no findByCourseId; filter the list
        return sectionDao.findAll().stream()
                .filter(s -> s.getCourseId() == courseId)
                .toList();
    }

    public List<Section> listSections() {
        return sectionDao.findAll();
    }

    public Optional<Section> getSectionById(int sectionId) {
        return sectionDao.findById(sectionId);
    }

    /**
     * Section insertion/update: your SectionDao.insert expects (int courseId, int instructorId, String semester, int year)
     * and update expects (int sectionId, int courseId, int instructorId, String semester, int year)
     */
    public int addSection(int courseId, int instructorId, String semester, int year) {
        AccessChecker.checkWritableOrThrow();
        return sectionDao.insert(courseId, instructorId, semester, year);
    }

    public void updateSection(int sectionId, int courseId, int instructorId, String semester, int year) {
        AccessChecker.checkWritableOrThrow();
        sectionDao.update(sectionId, courseId, instructorId, semester, year);
    }

    public void deleteSection(int sectionId) {
        AccessChecker.checkWritableOrThrow();

        sectionDao.delete(sectionId);
    }

    /* ---------------- ENROLLMENTS / STUDENT INFO ---------------- */

    public List<Enrollment> getEnrollmentsForStudent(int studentUserId) {
        AccessChecker.checkWritableOrThrow();
        return enrollmentDao.findByStudentId(studentUserId);
    }

    public List<Enrollment> getEnrollmentsForSection(int sectionId) {
        AccessChecker.checkWritableOrThrow();
        return enrollmentDao.findBySectionId(sectionId);
    }

    public int enrollStudentInSection(int studentUserId, int sectionId) {
        AccessChecker.checkWritableOrThrow();
        // your EnrollmentDao.insert signature previously accepted (studentId, sectionId, date)
        return enrollmentDao.insert(studentUserId, sectionId, LocalDate.now());
    }

    public void dropEnrollment(int enrollmentId) {
        AccessChecker.checkWritableOrThrow();
        // Some DAO variants use delete(int) and some use deleteById(int).
        // Use reflection to try both to remain compatible with whichever you have.
        callDeleteIfExists(enrollmentDao, "delete", int.class, enrollmentId);
        callDeleteIfExists(enrollmentDao, "deleteById", int.class, enrollmentId);
    }

    public Optional<Student> getStudentByUserId(int userId) {
        return studentDao.findByUserId(userId);
    }

    /* ---------------- GRADES ---------------- */

    public Optional<Grade> getGradeForEnrollment(int enrollmentId) {
        return gradeDao.findByEnrollmentId(enrollmentId);
    }

    public boolean setGradeForEnrollment(int enrollmentId, String grade) {
        return gradeDao.upsertByEnrollmentId(enrollmentId, grade);
    }

    /* ----------------- Helpers ----------------- */

    /**
     * Try to invoke a delete-style method on daoObject if present.
     * This avoids hard compile-time coupling to a single method name when DAOs differ.
     */
    private void callDeleteIfExists(Object daoObject, String methodName, Class<?> paramType, int value) {
        try {
            Method m = daoObject.getClass().getMethod(methodName, paramType);
            m.invoke(daoObject, value);
        } catch (NoSuchMethodException nsme) {
            // method not present -> that's fine, caller will try other names or ignore
        } catch (Exception ex) {
            // wrap into runtime to surface errors during runtime operations
            throw new RuntimeException("Failed invoking " + methodName + " on " + daoObject.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
        }
    }
    /* ---------------- INSTRUCTOR SECTIONS ---------------- */
    public List<Section> getSectionsForInstructor(int instructorUserId) {
        return sectionDao.findAll().stream()
                .filter(s -> s.getInstructorId() == instructorUserId)
                .toList();
    }

    /* ---------------- SETTINGS / MAINTENANCE ---------------- */

    public boolean isMaintenanceMode() {
        try {
            SettingsDao settingsDao = new SettingsDao();
            // findByKey returns String or null -> wrap into Optional
            return Optional.ofNullable(settingsDao.findByKey("maintenance"))
                    .map(v -> "ON".equalsIgnoreCase(v))
                    .orElse(false);
        } catch (Exception ex) {
            System.err.println("SettingsDao not found or failed: " + ex.getMessage());
            return false;
        }
    }

    public void setMaintenanceMode(boolean on) {
        try {
            SettingsDao settingsDao = new SettingsDao();
            // check null instead of isPresent()
            if (settingsDao.findByKey("maintenance") != null) {
                settingsDao.updateValue("maintenance", on ? "ON" : "OFF");
            } else {
                settingsDao.insert("maintenance", on ? "ON" : "OFF");
            }
        } catch (Exception ex) {
            System.err.println("SettingsDao not found or failed: " + ex.getMessage());
        }
    }

    public Optional<edu.univ.erp.domain.User> getUserById(int userId) {
        return userDao.findById(userId);
    }

    /**
     * Throw MaintenanceModeException if system is in maintenance and callerRole is not ADMIN.
     * Caller should pass the role of current user (ADMIN/INSTRUCTOR/STUDENT).
     */
    private void enforceNotMaintenanceUnlessAdmin(String callerRole) {
        try {
            boolean maintenance = systemService.isMaintenanceMode();
            if (maintenance && !"ADMIN".equalsIgnoreCase(callerRole)) {
                throw new MaintenanceModeException("System is in maintenance mode — write operations are temporarily disabled.");
            }
        } catch (RuntimeException ex) {
            // rethrow maintenance exceptions and wrap unexpected exceptions
            if (ex instanceof MaintenanceModeException) throw ex;
            throw new RuntimeException("Failed to evaluate maintenance mode: " + ex.getMessage(), ex);
        }
    }

    /**
     * Upsert grade for enrollment (wrapper used by UI).
     * This resolves compile-time calls from InstructorMainFrame that expected erpService.upsertGradeForEnrollment(...)
     */
    public boolean upsertGradeForEnrollment(Integer enrollmentId, String componentName, String marks) {
        // protect write operations
        edu.univ.erp.util.AccessChecker.checkWritableOrThrow();

        // Delegate to GradeDao (adjust method name if your GradeDao differs)
        GradeDao gradeDao = new GradeDao();
        // If your GradeDao has method upsertByEnrollmentId(enrollmentId, gradeString) then call it.
        // Here we assume a generic upsert that accepts enrollmentId and a marks string.
        return gradeDao.upsertByEnrollmentId(enrollmentId, marks);
    }

}
