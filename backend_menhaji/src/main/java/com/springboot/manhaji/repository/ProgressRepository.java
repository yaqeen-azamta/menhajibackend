package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.Progress;
import com.springboot.manhaji.entity.enums.CompletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Long> {
    Optional<Progress> findByStudentIdAndLessonId(Long studentId, Long lessonId);
    List<Progress> findByStudentId(Long studentId);
    List<Progress> findByStudentIdAndCompletionStatus(Long studentId, CompletionStatus completionStatus);
}
