package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByUserId(Long userId);
    List<Teacher> findBySchoolId(Long schoolId);
}
