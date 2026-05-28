package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.ParentStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParentStudentRepository extends JpaRepository<ParentStudent, Long> {
    List<ParentStudent> findByParentId(Long parentId);
    boolean existsByParentIdAndStudentId(Long parentId, Long studentId);
}
