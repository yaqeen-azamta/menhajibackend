package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.StudentQuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentQuestionAnswerRepository extends JpaRepository<StudentQuestionAnswer, Long> {

    // Spring Data resolves student.id, question.id, lesson.id via property traversal.

    List<StudentQuestionAnswer> findByStudentIdAndLessonId(Long studentId, Long lessonId);

    List<StudentQuestionAnswer> findByStudentId(Long studentId);

    long countByStudentIdAndLessonIdAndIsCorrectTrue(Long studentId, Long lessonId);

    boolean existsByStudentIdAndQuestionIdAndIsCorrectTrue(Long studentId, Long questionId);
}
