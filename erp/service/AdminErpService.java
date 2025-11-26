package edu.univ.erp.service;

import edu.univ.erp.data.CourseDao;
import edu.univ.erp.data.SectionDao;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Section;
import edu.univ.erp.util.AccessChecker;

import java.util.List;
import java.util.Optional;

public class AdminErpService {
    private final CourseDao courseDao = new CourseDao();
    private final SectionDao sectionDao = new SectionDao();

    // Courses
    public List<Course> listCourses() { return courseDao.findAll(); }
    public Optional<Course> getCourse(int id) { return courseDao.findById(id); }
    public int createCourse(String code, String title, int credits) {
        // insert directly via SQL in CourseDao - we will add a simple method via reflection here:
        return courseDao.insert(code, title, credits);
    }
    public void updateCourse(int id, String code, String title, int credits) { courseDao.update(id, code, title, credits); }
    public void deleteCourse(int id) { courseDao.delete(id); }

    // Sections
    public List<Section> getSectionsForInstructor(int instructorId) { return sectionDao.findByInstructorId(instructorId); }
    public List<Section> listSections() { return sectionDao.findAll(); }
    public int createSection(int courseId, int instructorId, String semester, int year) {
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
}
