package com.springboot.manhaji.repository;

import com.springboot.manhaji.entity.StudentSkillProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSkillProfileRepository extends JpaRepository<StudentSkillProfile, Long> {

    List<StudentSkillProfile> findByStudentIdAndLessonId(Long studentId, Long lessonId);

    List<StudentSkillProfile> findByStudentId(Long studentId);

    Optional<StudentSkillProfile> findByStudentIdAndLessonIdAndSubSkill(
            Long studentId, Long lessonId, String subSkill);
}
