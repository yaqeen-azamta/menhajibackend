package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.Question;
import com.springboot.manhaji.entity.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByLessonIdOrderByIdAsc(Long lessonId);
    List<Question> findByLessonId(Long lessonId);
    List<Question> findByLessonIdAndDifficultyLevel(Long lessonId, Integer difficultyLevel);
    List<Question> findByLessonIdAndType(Long lessonId, QuestionType type);

    @Query("SELECT q FROM Question q WHERE q.lesson.id = :lessonId AND q.type NOT IN :excludedTypes")
    List<Question> findByLessonIdAndTypeNotIn(@Param("lessonId") Long lessonId,
                                               @Param("excludedTypes") List<QuestionType> excludedTypes);

    @Query("SELECT q FROM Question q JOIN FETCH q.lesson l WHERE l.subject.id = :subjectId "
            + "ORDER BY l.orderIndex ASC, q.id ASC")
    List<Question> findAllBySubjectIdWithLesson(@Param("subjectId") Long subjectId);
}
