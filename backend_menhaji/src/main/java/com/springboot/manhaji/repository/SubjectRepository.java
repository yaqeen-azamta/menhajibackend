package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByGradeLevel(Integer gradeLevel);
    Optional<Subject> findByNameAndGradeLevel(String name, Integer gradeLevel);
}
