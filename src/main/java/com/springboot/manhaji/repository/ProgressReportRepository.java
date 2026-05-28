package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.ProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgressReportRepository extends JpaRepository<ProgressReport, Long> {
    List<ProgressReport> findByStudentIdOrderByGeneratedAtDesc(Long studentId);
}
