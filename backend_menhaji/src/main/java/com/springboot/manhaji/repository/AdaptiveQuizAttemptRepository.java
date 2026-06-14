package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.AdaptiveQuizAttempt;
import com.springboot.manhaji.entity.enums.AttemptStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
     * Fetch attempt with a row-level write lock.
     * Used by hint rate-limiting to make the check-and-increment atomic
     * across concurrent requests.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdaptiveQuizAttempt a WHERE a.id = :id")
    Optional<AdaptiveQuizAttempt> findByIdForUpdate(@Param("id") Long id);
}
