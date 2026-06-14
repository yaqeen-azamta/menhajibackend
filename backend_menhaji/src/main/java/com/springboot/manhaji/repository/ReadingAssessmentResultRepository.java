package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.ReadingAssessmentResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingAssessmentResultRepository extends JpaRepository<ReadingAssessmentResult, Long> {

    // ─── History ───────────────────────────────────────────────────────────────

    /** Paginated attempt history for a student, newest first. Used by GET /api/reading/history. */
    Page<ReadingAssessmentResult> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);

    /** All attempts by a student on one lesson, newest first. Used by GET /api/reading/history/lesson/{id}. */
    List<ReadingAssessmentResult> findByStudentIdAndLessonIdOrderByCreatedAtDesc(Long studentId, Long lessonId);

    /** The single most-recent attempt by a student on a lesson. */
    Optional<ReadingAssessmentResult> findTopByStudentIdAndLessonIdOrderByCreatedAtDesc(Long studentId, Long lessonId);

    // ─── Counts ────────────────────────────────────────────────────────────────

    /** Total reading attempts across all lessons for one student. */
    int countByStudentId(Long studentId);

    /** Attempts where accuracy met or exceeded a threshold — used for mastery tracking. */
    int countByStudentIdAndAccuracyGreaterThanEqual(Long studentId, int minAccuracy);

    // ─── Analytics — average accuracy ──────────────────────────────────────────

    /**
     * Average accuracy across all attempts by a student.
     * Returns null when the student has no attempts.
     * Used for student progress dashboards.
     */
    @Query("SELECT AVG(r.accuracy) FROM ReadingAssessmentResult r WHERE r.studentId = :studentId")
    Double findAvgAccuracyByStudentId(@Param("studentId") Long studentId);

    /**
     * Average accuracy for a student on one specific lesson.
     * Returns null when no attempts exist for this student+lesson pair.
     * Used for lesson-level progress charts and teacher reports.
     */
    @Query("SELECT AVG(r.accuracy) FROM ReadingAssessmentResult r " +
           "WHERE r.studentId = :studentId AND r.lesson.id = :lessonId")
    Double findAvgAccuracyByStudentIdAndLessonId(@Param("studentId") Long studentId,
                                                  @Param("lessonId")  Long lessonId);

    // ─── Teacher / lesson-level views ──────────────────────────────────────────

    /**
     * All attempts on a specific lesson across all students, newest first.
     * Used for teacher lesson-statistics dashboards.
     */
    Page<ReadingAssessmentResult> findByLessonIdOrderByCreatedAtDesc(Long lessonId, Pageable pageable);

    /**
     * Average accuracy across all students on a lesson.
     * Returns null when no attempts exist.
     * Used for lesson-level difficulty analysis.
     */
    @Query("SELECT AVG(r.accuracy) FROM ReadingAssessmentResult r WHERE r.lesson.id = :lessonId")
    Double findAvgAccuracyByLessonId(@Param("lessonId") Long lessonId);

    // ─── Progress over time ─────────────────────────────────────────────────────

    /**
     * All attempts by a student in ascending chronological order.
     * Used to render reading-improvement-over-time charts (oldest attempt first).
     */
    List<ReadingAssessmentResult> findByStudentIdOrderByCreatedAtAsc(Long studentId);

    /**
     * All attempts by a student on one lesson in ascending chronological order.
     * Shows reading improvement across multiple tries on the same lesson.
     */
    List<ReadingAssessmentResult> findByStudentIdAndLessonIdOrderByCreatedAtAsc(Long studentId, Long lessonId);
}
