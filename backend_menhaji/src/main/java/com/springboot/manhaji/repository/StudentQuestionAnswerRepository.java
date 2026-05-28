package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.StudentQuestionAnswer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentQuestionAnswerRepository
        extends JpaRepository<StudentQuestionAnswer, Long> {

    Optional<StudentQuestionAnswer>
    findByStudentIdAndQuestionId(
            Long studentId,
            Long questionId
    );
}