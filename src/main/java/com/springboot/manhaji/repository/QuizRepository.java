package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByLessonId(Long lessonId);
    List<Quiz> findByLessonIdAndGamified(Long lessonId, Boolean gamified);

    /**
     * Direct join-table read so DataSeeder can check which questions are already
     * attached to a quiz without pulling the lazy collection (which requires an
     * open Hibernate session).
     */
    @Query("SELECT q.id FROM Quiz qu JOIN qu.questions q WHERE qu.id = :quizId")
    List<Long> findQuestionIdsByQuizId(@Param("quizId") Long quizId);
}
