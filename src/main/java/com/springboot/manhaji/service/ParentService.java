package com.springboot.manhaji.service;

import com.springboot.manhaji.dto.response.ChildSummaryResponse;
import com.springboot.manhaji.dto.response.ParentDashboardResponse;
import com.springboot.manhaji.dto.response.StudentDetailResponse;
import com.springboot.manhaji.dto.response.SubjectMasterySummary;
import com.springboot.manhaji.entity.Attempt;
import com.springboot.manhaji.entity.Parent;
import com.springboot.manhaji.entity.ParentStudent;
import com.springboot.manhaji.entity.Progress;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.exception.UnauthorizedException;
import com.springboot.manhaji.repository.AttemptRepository;
import com.springboot.manhaji.repository.LessonRepository;
import com.springboot.manhaji.repository.ParentRepository;
import com.springboot.manhaji.repository.ParentStudentRepository;
import com.springboot.manhaji.repository.ProgressRepository;
import com.springboot.manhaji.repository.StudentRepository;
import com.springboot.manhaji.service.support.ProgressMetrics;
import com.springboot.manhaji.support.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentService {

    private final ParentRepository parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final StudentRepository studentRepository;
    private final ProgressRepository progressRepository;
    private final AttemptRepository attemptRepository;
    private final LessonRepository lessonRepository;
    private final ProgressMetrics metrics;
    private final Messages messages;

    public ParentDashboardResponse getDashboard(Long userId) {
        Parent parent = parentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", userId));

        List<Student> children = parentStudentRepository.findByParentId(parent.getId())
                .stream()
                .map(ParentStudent::getStudent)
                .toList();

        List<ChildSummaryResponse> childSummaries = children.stream()
                .map(this::buildChildSummary)
                .toList();

        return ParentDashboardResponse.builder()
                .parentId(parent.getUser().getId())
                .fullName(parent.getParentName())
                .children(childSummaries)
                .build();
    }

    public StudentDetailResponse getChildDetail(Long userId, Long childUserId) {
        Parent parent = parentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", userId));

        Student child = studentRepository.findByUserId(childUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", childUserId));

        if (!parentStudentRepository.existsByParentIdAndStudentId(parent.getId(), child.getId())) {
            throw new UnauthorizedException(messages.get("error.parent.childNotLinked"));
        }

        List<Progress> progressRecords = progressRepository.findByStudentId(child.getId());
        List<Attempt> attempts = attemptRepository.findByStudentIdOrderByCreatedAtDesc(child.getId());

        List<SubjectMasterySummary> subjectBreakdown = metrics.buildSubjectBreakdown(child, progressRecords);

        return StudentDetailResponse.builder()
                .studentId(child.getUser().getId())
                .fullName(child.getStudentName())
                .email(child.getUser().getEmail())
                .phone(child.getUser().getPhone())
                .gradeLevel(child.getGradeLevel())
                .totalPoints(child.getTotalPoints())
                .currentStreak(child.getCurrentStreak())
                .lastLoginAt(child.getUser().getLastLoginAt())
                .createdAt(child.getUser().getCreatedAt())
                .lessonsCompleted(metrics.countCompleted(progressRecords))
                .lessonsInProgress(metrics.countInProgress(progressRecords))
                .overallMastery(ProgressMetrics.round2(metrics.averageMastery(progressRecords)))
                .totalAttempts(attempts.size())
                .averageScore(ProgressMetrics.round2(metrics.averageGradedScore(attempts)))
                .subjectBreakdown(subjectBreakdown)
                .build();
    }

    private ChildSummaryResponse buildChildSummary(Student student) {
        List<Progress> progressRecords = progressRepository.findByStudentId(student.getId());

        int totalLessons = lessonRepository
                .findByGradeLevelOrderByOrderIndexAsc(student.getGradeLevel())
                .size();

        return ChildSummaryResponse.builder()
                .studentId(student.getUser().getId())
                .fullName(student.getStudentName())
                .avatarId(student.getAvatarId())
                .gradeLevel(student.getGradeLevel())
                .totalPoints(student.getTotalPoints())
                .currentStreak(student.getCurrentStreak())
                .lessonsCompleted(metrics.countCompleted(progressRecords))
                .totalLessons(totalLessons)
                .overallMastery(ProgressMetrics.round2(metrics.averageMastery(progressRecords)))
                .lastLoginAt(student.getUser().getLastLoginAt())
                .build();
    }
}
