package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);
    List<Student> findByGradeLevel(Integer gradeLevel);
    List<Student> findBySchoolId(Long schoolId);
    List<Student> findBySchoolIdAndGradeLevel(Long schoolId, Integer gradeLevel);

    @Query("SELECT s FROM Student s WHERE s.gradeLevel = :gradeLevel ORDER BY s.totalPoints DESC")
    List<Student> findTopByGradeLevelOrderByPointsDesc(Integer gradeLevel);

    @Query("SELECT s FROM Student s ORDER BY s.totalPoints DESC")
    List<Student> findAllOrderByTotalPointsDesc();
}
