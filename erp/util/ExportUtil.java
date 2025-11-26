package edu.univ.erp.util;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Student;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Export utilities for CSV and PDF. Uses commons-csv and OpenPDF (librepdf).
 */
public final class ExportUtil {
    private ExportUtil() {}

    /* --------------------- CSV --------------------- */

    /**
     * Export CSV. Caller passes rows where first row may be header.
     * @param out OutputStream (will be closed by caller)
     * @param rows List of String[] rows
     */
    public static void writeCsv(OutputStream out, List<String[]> rows) throws IOException {
        try (OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(osw);
             CSVPrinter printer = new CSVPrinter(bw, CSVFormat.DEFAULT)) {

            // If first row looks like a header (contains "Enroll" or columns), print it as header
            int start = 0;
            if (!rows.isEmpty()) {
                String first0 = rows.get(0).length > 0 && rows.get(0)[0] != null ? rows.get(0)[0].toLowerCase() : "";
                if (first0.contains("enroll") || first0.contains("id") || first0.contains("student")) {
                    printer.printRecord((Object[]) rows.get(0));
                    start = 1;
                }
            }

            for (int i = start; i < rows.size(); i++) {
                printer.printRecord((Object[]) rows.get(i));
            }
            printer.flush();
        }
    }

    /* --------------------- PDF --------------------- */

    public static void writePdf(OutputStream out, String title, String[] header, List<String[]> rows) throws Exception {
        Document doc = new Document();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont = new Font(Font.HELVETICA, 14f, Font.BOLD);
        Paragraph p = new Paragraph(title, titleFont);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(12f);
        doc.add(p);

        PdfPTable table = new PdfPTable(header.length);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(6f);
        table.setSpacingAfter(6f);

        Font hdrFont = new Font(Font.HELVETICA, 10f, Font.BOLD);
        for (String h : header) {
            PdfPCell cell = new PdfPCell(new Paragraph(h == null ? "" : h, hdrFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        Font cellFont = new Font(Font.HELVETICA, 10f, Font.NORMAL);
        for (String[] row : rows) {
            for (String col : row) {
                PdfPCell cell = new PdfPCell(new Paragraph(col == null ? "" : col, cellFont));
                table.addCell(cell);
            }
        }

        doc.add(table);
        doc.close();
    }

    /* --------------------- Helpers to build rows for enrollments --------------------- */

    public static List<String[]> buildEnrollmentRows(List<Enrollment> enrollments,
                                                     Function<Integer, Optional<Course>> courseFetcher,
                                                     Function<Integer, Optional<Section>> sectionFetcher,
                                                     Function<Integer, Optional<Student>> studentFetcher,
                                                     Function<Integer, Optional<Grade>> gradeFetcher) {
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
        java.util.ArrayList<String[]> rows = new java.util.ArrayList<>();
        String[] header = new String[]{"Enroll ID", "Student ID", "Student Roll", "Course Code", "Course Title", "Section ID", "Instructor ID", "Enrolled", "Grade"};
        rows.add(header);
        for (Enrollment e : enrollments) {
            Optional<Section> sec = sectionFetcher.apply(e.getSectionId());
            Optional<Course> course = sec.isPresent() ? courseFetcher.apply(sec.get().getCourseId()) : Optional.empty();
            Optional<Student> stu = studentFetcher.apply(e.getStudentId());
            Optional<Grade> gr = gradeFetcher.apply(e.getEnrollmentId());
            String enrolled = e.getEnrollmentDate() == null ? "" : e.getEnrollmentDate().toString();
            String courseCode = course.map(Course::getCode).orElse("");
            String courseTitle = course.map(Course::getTitle).orElse("");
            String roll = stu.map(Student::getRollNo).orElse("");
            String grade = gr.map(Grade::getGrade).orElse("");
            String[] row = new String[]{
                    String.valueOf(e.getEnrollmentId()),
                    String.valueOf(e.getStudentId()),
                    roll,
                    courseCode,
                    courseTitle,
                    String.valueOf(e.getSectionId()),
                    sec.map(s -> String.valueOf(s.getInstructorId())).orElse(""),
                    enrolled,
                    grade
            };
            rows.add(row);
        }
        return rows;
    }
}
