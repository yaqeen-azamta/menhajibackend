package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.TracingQuestion;
import com.springboot.manhaji.entity.enums.TracingCharacterType;
import com.springboot.manhaji.entity.enums.TracingLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TracingQuestionRepository extends JpaRepository<TracingQuestion, Long> {

    /** Find by the linked question's ID (same as TracingQuestion.id due to @MapsId). */
    Optional<TracingQuestion> findByQuestionId(Long questionId);

    /** All tracing questions for a lesson, ordered by question id. */
    @Query("SELECT tq FROM TracingQuestion tq " +
           "JOIN FETCH tq.question q " +
           "WHERE q.lesson.id = :lessonId " +
           "ORDER BY q.id ASC")
    List<TracingQuestion> findByLessonIdOrderByQuestionIdAsc(@Param("lessonId") Long lessonId);

    List<TracingQuestion> findByLanguage(TracingLanguage language);

    List<TracingQuestion> findByCharacterType(TracingCharacterType characterType);

    List<TracingQuestion> findByLanguageAndCharacterType(
            TracingLanguage language, TracingCharacterType characterType);

    boolean existsByQuestionId(Long questionId);
}
