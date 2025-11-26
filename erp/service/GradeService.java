package edu.univ.erp.service;

import edu.univ.erp.data.ComponentMarksDao;
import edu.univ.erp.data.GradingComponentDao;
import edu.univ.erp.data.SectionWeightDao;
import edu.univ.erp.domain.ComponentMark;
import edu.univ.erp.domain.GradingComponent;
import edu.univ.erp.domain.SectionGradeWeight;
import edu.univ.erp.util.AccessChecker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * GradeService: compute weighted totals and return breakdowns.
 */
public class GradeService {
    private final GradingComponentDao componentDao = new GradingComponentDao();
    private final SectionWeightDao weightDao = new SectionWeightDao();
    private final ComponentMarksDao marksDao = new ComponentMarksDao();

    public List<GradingComponent> listComponents() { return componentDao.listAll(); }

    public List<SectionGradeWeight> getWeightsForSection(int sectionId) { return weightDao.findBySection(sectionId); }

    public void saveWeight(int sectionId, int componentId, BigDecimal weight) { weightDao.upsert(sectionId, componentId, weight); }

    public Map<Integer, BigDecimal> getMarksForEnrollment(int enrollmentId) {
        AccessChecker.checkWritableOrThrow();
        Map<Integer, BigDecimal> out = new HashMap<>();
        List<ComponentMark> marks = marksDao.findByEnrollment(enrollmentId);
        for (ComponentMark m : marks) out.put(m.getComponentId(), m.getMarks());
        return out;
    }

    public void saveMark(int enrollmentId, int componentId, BigDecimal marks) {
        AccessChecker.checkWritableOrThrow();
        marksDao.upsert(enrollmentId, componentId, marks);
    }

    /**
     * Compute weighted numeric total in 0..100 scale for a given enrollment.
     * If weight for a component exists but marks are null -> treat as 0 (instructor may want null handling).
     */
    public BigDecimal computeWeightedTotal(int enrollmentId, int sectionId) {
        AccessChecker.checkWritableOrThrow();
        List<SectionGradeWeight> weights = weightDao.findBySection(sectionId);
        if (weights.isEmpty()) return BigDecimal.ZERO;
        Map<Integer, BigDecimal> marks = getMarksForEnrollment(enrollmentId);
        BigDecimal total = BigDecimal.ZERO;
        for (SectionGradeWeight w : weights) {
            BigDecimal weightPct = w.getWeight() == null ? BigDecimal.ZERO : w.getWeight();
            BigDecimal mark = marks.getOrDefault(w.getComponentId(), BigDecimal.ZERO);
            // weighted contribution = mark * weightPct / 100
            BigDecimal contrib = mark.multiply(weightPct).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            total = total.add(contrib);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Map numeric to letter grade using a simple scale (adjust as needed).
     */
    public String numericToLetter(BigDecimal num) {
        if (num == null) return "N/A";
        double v = num.doubleValue();
        if (v >= 90) return "A";
        if (v >= 80) return "B";
        if (v >= 70) return "C";
        if (v >= 60) return "D";
        return "F";
    }
}
