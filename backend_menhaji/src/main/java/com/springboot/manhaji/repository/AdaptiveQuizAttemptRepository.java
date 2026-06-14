package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.AdaptiveQuizAttempt;
import com.springboot.manhaji.entity.enums.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdaptiveQuizAttemptRepository extends JpaRepository<AdaptiveQuizAttempt, Long> {

    Optional<AdaptiveQuizAttempt> findByStudentIdAndLessonIdAndStatus(
            Long studentId, Long lessonId, AttemptStatus status);

    List<AdaptiveQuizAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);

    List<AdaptiveQuizAttempt> findByStudentIdAndLessonId(Long studentId, Long lessonId);

    /**
     * Fetch attempt with a row-level write lock using a native SQL query.
     * MariaDB supports {@code FOR UPDATE} but not the JPQL-generated
     * {@code FOR UPDATE OF alias} syntax that Hibernate 6 emits with @Lock.
     */
    @Query(value = "SELECT * FROM adaptive_quiz_attempts WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<AdaptiveQuizAttempt> findByIdForUpdate(@Param("id") Long id);
}
