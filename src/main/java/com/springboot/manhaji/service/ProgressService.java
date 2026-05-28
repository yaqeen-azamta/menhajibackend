package com.springboot.manhaji.service;

import com.springboot.manhaji.dto.response.*;
import com.springboot.manhaji.entity.*;
import com.springboot.manhaji.entity.enums.AttemptStatus;
import com.springboot.manhaji.entity.enums.CompletionStatus;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final StudentRepository studentRepository;
    private final ProgressRepository progressRepository;
    private final AttemptRepository attemptRepository;
    private final SubjectRepository subjectRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public Map<String, Object> completeLesson(Long userId, Long lessonId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        Progress progress = progressRepository
                .findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElseGet(() -> {
                    Progress p = new Progress();
                    p.setStudent(student);
                    p.setLesson(lesson);
                    p.setMasteryLevel(0.0);
                    p.setCompletionStatus(CompletionStatus.IN_PROGRESS);
                    return p;
                });

        progress.setLastAccessedAt(LocalDateTime.now());

        if (progress.getCompletionStatus() != CompletionStatus.COMPLETED
                && progress.getCompletionStatus() != CompletionStatus.MASTERED) {
            progress.setCompletionStatus(CompletionStatus.COMPLETED);
            progress.setCompletedAt(LocalDateTime.now());
            progress.setMasteryLevel(Math.max(progress.getMasteryLevel(), 100.0));

            studentRepository.save(student);
        }

        progressRepository.save(progress);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lessonId", lessonId);
        result.put("completionStatus", progress.getCompletionStatus().name());
        result.put("masteryLevel", progress.getMasteryLevel());
        result.put("totalPoints", student.getTotalPoints());
        return result;
    }

    public ProgressSummaryResponse getProgressSummary(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        List<Progress> allProgress = progressRepository.findByStudentId(student.getId());
        List<Attempt> allAttempts = attemptRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());

        int totalLessonsInGrade = lessonRepository.findByGradeLevelOrderByOrderIndexAsc(student.getGradeLevel()).size();
        int completedLessons = (int) allProgress.stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.COMPLETED ||
                             p.getCompletionStatus() == CompletionStatus.MASTERED)
                .count();
        int masteredLessons = (int) allProgress.stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.MASTERED)
                .count();
        int inProgressLessons = (int) allProgress.stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.IN_PROGRESS)
                .count();

        double overallMastery = allProgress.isEmpty() ? 0.0 :
                allProgress.stream().mapToDouble(Progress::getMasteryLevel).average().orElse(0.0);

        List<Attempt> gradedAttempts = allAttempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.GRADED)
                .toList();
        double averageQuizScore = gradedAttempts.isEmpty() ? 0.0 :
                gradedAttempts.stream().mapToDouble(a -> a.getScore() != null ? a.getScore() : 0).average().orElse(0.0);

        List<Subject> subjects = subjectRepository.findByGradeLevel(student.getGradeLevel());
        List<SubjectProgressResponse> subjectProgress = subjects.stream()
                .map(subject -> buildSubjectProgress(subject, allProgress))
                .toList();

        List<RecentActivityResponse> recentActivity = buildRecentActivity(allProgress, gradedAttempts);

        return ProgressSummaryResponse.builder()
                .totalLessons(totalLessonsInGrade)
                .completedLessons(completedLessons)
                .masteredLessons(masteredLessons)
                .inProgressLessons(inProgressLessons)
                .overallMastery(Math.round(overallMastery * 10.0) / 10.0)
                .totalPoints(student.getTotalPoints())
                .currentStreak(student.getCurrentStreak())
                .totalQuizzesTaken(gradedAttempts.size())
                .averageQuizScore(Math.round(averageQuizScore * 10.0) / 10.0)
                .subjectProgress(subjectProgress)
                .recentActivity(recentActivity)
                .build();
    }

    public List<LeaderboardEntryResponse> getLeaderboard(Long currentUserId, Integer gradeLevel) {
        List<Student> students;
        if (gradeLevel != null) {
            students = studentRepository.findTopByGradeLevelOrderByPointsDesc(gradeLevel);
        } else {
            students = studentRepository.findAllOrderByTotalPointsDesc();
        }

        if (students.size() > 50) {
            students = students.subList(0, 50);
        }

        List<LeaderboardEntryResponse> leaderboard = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            List<Progress> progress = progressRepository.findByStudentId(s.getId());
            int completed = (int) progress.stream()
                    .filter(p -> p.getCompletionStatus() == CompletionStatus.COMPLETED ||
                                 p.getCompletionStatus() == CompletionStatus.MASTERED)
                    .count();

            leaderboard.add(LeaderboardEntryResponse.builder()
                    .rank(i + 1)
                    .studentId(s.getUser().getId())
                    .studentName(s.getStudentName())
                    .avatarId(s.getAvatarId())
                    .totalPoints(s.getTotalPoints())
                    .completedLessons(completed)
                    .isCurrentUser(s.getUser().getId().equals(currentUserId))
                    .build());
        }
        return leaderboard;
    }

    private SubjectProgressResponse buildSubjectProgress(Subject subject, List<Progress> allProgress) {
        List<Lesson> subjectLessons = lessonRepository.findBySubjectIdOrderByOrderIndexAsc(subject.getId());
        Set<Long> subjectLessonIds = subjectLessons.stream().map(Lesson::getId).collect(Collectors.toSet());

        List<Progress> subjectProgress = allProgress.stream()
                .filter(p -> subjectLessonIds.contains(p.getLesson().getId()))
                .toList();

        int completed = (int) subjectProgress.stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.COMPLETED ||
                             p.getCompletionStatus() == CompletionStatus.MASTERED)
                .count();

        double mastery = subjectProgress.isEmpty() ? 0.0 :
                subjectProgress.stream().mapToDouble(Progress::getMasteryLevel).average().orElse(0.0);

        return SubjectProgressResponse.builder()
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .totalLessons(subjectLessons.size())
                .completedLessons(completed)
                .masteryPercent(Math.round(mastery * 10.0) / 10.0)
                .build();
    }

    private List<RecentActivityResponse> buildRecentActivity(List<Progress> allProgress, List<Attempt> gradedAttempts) {
        List<RecentActivityResponse> activities = new ArrayList<>();

        allProgress.stream()
                .filter(p -> p.getLastAccessedAt() != null)
                .sorted(Comparator.comparing(Progress::getLastAccessedAt).reversed())
                .limit(5)
                .forEach(p -> activities.add(RecentActivityResponse.builder()
                        .type("LESSON_VIEWED")
                        .title(p.getLesson().getTitle())
                        .subjectName(p.getLesson().getSubject().getName())
                        .timestamp(p.getLastAccessedAt())
                        .build()));

        gradedAttempts.stream()
                .limit(5)
                .forEach(a -> activities.add(RecentActivityResponse.builder()
                        .type("QUIZ_COMPLETED")
                        .title(a.getQuiz().getTitle())
                        .subjectName(a.getQuiz().getLesson().getSubject().getName())
                        .score(a.getScore())
                        .timestamp(a.getSubmittedAt())
                        .build()));

        activities.sort(Comparator.comparing(RecentActivityResponse::getTimestamp,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return activities.size() > 10 ? activities.subList(0, 10) : activities;
    }
}
