package com.springboot.manhaji.service;

import com.springboot.manhaji.dto.response.StudentDashboardResponse;
import com.springboot.manhaji.dto.response.SubjectResponse;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final LessonService lessonService;

    public StudentDashboardResponse getDashboard(Long userId) {

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student", userId));

        List<SubjectResponse> subjects =
                lessonService.getSubjectsByGrade(
                        student.getGradeLevel(),
                        student.getId()
                );

        return StudentDashboardResponse.builder()
                .studentId(student.getUser().getId())
                .fullName(student.getStudentName())
                .avatarId(student.getAvatarId())
                .gradeLevel(student.getGradeLevel())
                .currentStreak(student.getCurrentStreak())
                .totalPoints(student.getTotalPoints())
                .subjects(subjects)
                .build();
    }

    public void updateAvatar(Long userId, String avatarId) {

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student", userId));

        student.setAvatarId(avatarId);

        studentRepository.save(student);
    }
}
