package com.springboot.manhaji.service.support;

import com.springboot.manhaji.dto.response.SubjectMasterySummary;
import com.springboot.manhaji.entity.Attempt;
import com.springboot.manhaji.entity.Lesson;
import com.springboot.manhaji.entity.Progress;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.entity.Subject;
import com.springboot.manhaji.entity.enums.AttemptStatus;
import com.springboot.manhaji.entity.enums.CompletionStatus;
import com.springboot.manhaji.repository.LessonRepository;
import com.springboot.manhaji.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared aggregation helpers for student progress and attempt statistics.
 * Used by TeacherService, ParentService, and ProgressReportService to avoid
 * duplicating completion counts, mastery/score averages, and subject
 * breakdowns.
 */
@Component
@RequiredArgsConstructor
public class ProgressMetrics {

    private final SubjectRepository subjectRepository;
    private final LessonRepository lessonRepository;

    public int countCompleted(List<Progress> records) {
        return (int) records.stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.COMPLETED)
                .count();
    }

    public int countInProgress(List<Progress> records) {
        return (int) records.stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.IN_PROGRESS)
                .count();
    }

    public double averageMastery(List<Progress> records) {
        return records.stream()
                .mapToDouble(p -> p.getMasteryLevel() == null ? 0.0 : p.getMasteryLevel())
                .average()
                .orElse(0.0);
    }

    public double averageGradedScore(List<Attempt> attempts) {
        return attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.GRADED && a.getScore() != null)
                .mapToDouble(Attempt::getScore)
                .average()
                .orElse(0.0);
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public List<SubjectMasterySummary> buildSubjectBreakdown(Student student, List<Progress> progressRecords) {
        Map<Long, List<Progress>> bySubject = progressRecords.stream()
                .filter(p -> p.getLesson() != null && p.getLesson().getSubject() != null)
                .collect(Collectors.groupingBy(p -> p.getLesson().getSubject().getId()));

        List<Subject> subjects = subjectRepository.findByGradeLevel(student.getGradeLevel());
        List<SubjectMasterySummary> result = new ArrayList<>();
        for (Subject subject : subjects) {
            List<Lesson> lessons = lessonRepository
                    .findBySubjectIdAndGradeLevelOrderByOrderIndexAsc(subject.getId(), student.getGradeLevel());
            List<Progress> subjectProgress = bySubject.getOrDefault(subject.getId(), List.of());

            result.add(SubjectMasterySummary.builder()
                    .subjectId(subject.getId())
                    .subjectName(subject.getName())
                    .totalLessons(lessons.size())
                    .lessonsCompleted(countCompleted(subjectProgress))
                    .averageMastery(round2(averageMastery(subjectProgress)))
                    .build());
        }
        return result;
    }
}
