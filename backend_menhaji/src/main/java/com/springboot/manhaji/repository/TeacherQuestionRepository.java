package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.TeacherQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherQuestionRepository extends JpaRepository<TeacherQuestion, Long> {

    List<TeacherQuestion> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<TeacherQuestion> findByTeacherIdAndGradeLevelOrderByCreatedAtDesc(Long teacherId, Integer gradeLevel);

    List<TeacherQuestion> findByTeacherIdAndSubjectIdOrderByCreatedAtDesc(Long teacherId, Long subjectId);

    List<TeacherQuestion> findByTeacherIdAndLessonIdOrderByCreatedAtDesc(Long teacherId, Long lessonId);

    Optional<TeacherQuestion> findByIdAndTeacherId(Long id, Long teacherId);
}
