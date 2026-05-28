package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.TracingAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TracingAnswerRepository extends JpaRepository<TracingAnswer, Long> {

    /** How many times has this student attempted this question. */
    int countByStudentIdAndQuestionId(Long studentId, Long questionId);

    /** All answers for a student, newest first. */
    Page<TracingAnswer> findByStudentIdOrderBySubmittedAtDesc(Long studentId, Pageable pageable);

    /** All answers for a student on one specific question, newest first. */
    List<TracingAnswer> findByStudentIdAndQuestionIdOrderBySubmittedAtDesc(
            Long studentId, Long questionId);

    /** Best (highest) finalAccuracy for a student on a question. */
    @Query("SELECT MAX(ta.finalAccuracy) FROM TracingAnswer ta " +
           "WHERE ta.studentId = :studentId AND ta.question.id = :questionId")
    Optional<Double> findBestAccuracyByStudentIdAndQuestionId(
            @Param("studentId") Long studentId,
            @Param("questionId") Long questionId);

    /** Best stars earned for a student on a question. */
    @Query("SELECT MAX(ta.stars) FROM TracingAnswer ta " +
           "WHERE ta.studentId = :studentId AND ta.question.id = :questionId")
    Optional<Integer> findBestStarsByStudentIdAndQuestionId(
            @Param("studentId") Long studentId,
            @Param("questionId") Long questionId);

    /** Total attempts made by a student across all tracing questions. */
    long countByStudentId(Long studentId);

    /** All distinct question IDs attempted by a student. */
    @Query("SELECT DISTINCT ta.question.id FROM TracingAnswer ta WHERE ta.studentId = :studentId")
    List<Long> findAttemptedQuestionIdsByStudentId(@Param("studentId") Long studentId);

    /** Summary stats per question for progress dashboard. */
    @Query("SELECT ta.question.id, COUNT(ta), MAX(ta.finalAccuracy), MAX(ta.stars), " +
           "ta.question.id " +
           "FROM TracingAnswer ta " +
           "WHERE ta.studentId = :studentId " +
           "GROUP BY ta.question.id")
    List<Object[]> findProgressSummaryByStudentId(@Param("studentId") Long studentId);

    /** Total correct answers for a student. */
    @Query("SELECT COUNT(ta) FROM TracingAnswer ta " +
           "WHERE ta.studentId = :studentId AND ta.isCorrect = true")
    long countCorrectByStudentId(@Param("studentId") Long studentId);

    /** Average finalAccuracy for a student. */
    @Query("SELECT AVG(ta.finalAccuracy) FROM TracingAnswer ta WHERE ta.studentId = :studentId")
    Optional<Double> findAverageAccuracyByStudentId(@Param("studentId") Long studentId);

    /** Most recent answer for a student on a question. */
    @Query("SELECT ta FROM TracingAnswer ta " +
           "WHERE ta.studentId = :studentId AND ta.question.id = :questionId " +
           "ORDER BY ta.submittedAt DESC")
    Optional<TracingAnswer> findLatestByStudentIdAndQuestionId(
            @Param("studentId") Long studentId,
            @Param("questionId") Long questionId);
}
