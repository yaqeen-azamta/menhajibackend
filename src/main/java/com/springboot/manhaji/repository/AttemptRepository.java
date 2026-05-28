package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.Attempt;
import com.springboot.manhaji.entity.enums.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Attempt> findByStudentIdAndQuizId(Long studentId, Long quizId);
    Optional<Attempt> findByStudentIdAndQuizIdAndStatus(Long studentId, Long quizId, AttemptStatus status);
}
