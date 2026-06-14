package com.springboot.manhaji.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.dto.response.LessonResponse;
import com.springboot.manhaji.dto.response.LessonSummaryResponse;
import com.springboot.manhaji.dto.response.RecommendedLessonResponse;
import com.springboot.manhaji.dto.response.SubjectResponse;
import com.springboot.manhaji.entity.Lesson;
import com.springboot.manhaji.entity.Progress;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.entity.Subject;
import com.springboot.manhaji.entity.enums.CompletionStatus;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.LessonRepository;
import com.springboot.manhaji.repository.ProgressRepository;
import com.springboot.manhaji.repository.StudentRepository;
import com.springboot.manhaji.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    private final SubjectRepository subjectRepository;
    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper;

    // studentId here is students.id (internal PK) — callers translate from userId first
    public List<SubjectResponse> getSubjectsByGrade(Integer gradeLevel, Long studentId) {
Student student = studentRepository.findById(studentId)
        .orElseThrow(() ->
                new ResourceNotFoundException("Student", studentId));

Long realStudentId = student.getId();        List<Subject> subjects = subjectRepository.findByGradeLevel(gradeLevel);
        log.debug("[Subjects] gradeLevel={} → {} subjects found", gradeLevel, subjects.size());
        return subjects.stream().map(subject -> {
            log.debug("[Subjects] id={}, name={}, cover_image={}", subject.getId(), subject.getName(), subject.getCoverImage());
            List<Lesson> lessons = lessonRepository.findBySubjectIdOrderByOrderIndexAsc(subject.getId());
            long completed = lessons.stream()
                    .filter(lesson -> {
                       Optional<Progress> p = progressRepository.findByStudentIdAndLessonId(
        realStudentId,
        lesson.getId());
                        return p.isPresent() && (p.get().getCompletionStatus() == CompletionStatus.COMPLETED
                                || p.get().getCompletionStatus() == CompletionStatus.MASTERED);
                    })
                    .count();
            return SubjectResponse.builder()
                    .id(subject.getId())
                    .name(subject.getName())
                    .gradeLevel(subject.getGradeLevel())
                    .totalLessons(lessons.size())
                    .completedLessons((int) completed)
                    .coverImage(subject.getCoverImage())
                    .build();
        }).toList();
    }

    public List<LessonSummaryResponse> getLessonsBySubject(Long subjectId, Long studentId) {
        List<Lesson> lessons = lessonRepository.findBySubjectIdOrderByOrderIndexAsc(subjectId);
        return lessons.stream().map(lesson -> {
            Optional<Progress> progress = progressRepository.findByStudentIdAndLessonId(studentId, lesson.getId());
            return LessonSummaryResponse.builder()
                    .id(lesson.getId())
                    .title(lesson.getTitle())
                    .orderIndex(lesson.getOrderIndex())
                    .semesterNumber(lesson.getSemesterNumber() != null ? lesson.getSemesterNumber() : 1)
                    .completionStatus(progress.map(Progress::getCompletionStatus).orElse(CompletionStatus.NOT_STARTED))
                    .masteryLevel(progress.map(Progress::getMasteryLevel).orElse(0.0))
                    .build();
        }).toList();
    }

    public LessonResponse getLessonDetail(Long lessonId, Long studentId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        Progress progress = progressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                .orElseGet(() -> {
                    Progress p = new Progress();
                    p.setStudent(student);
                    p.setLesson(lesson);
                    p.setCompletionStatus(CompletionStatus.IN_PROGRESS);
                    return p;
                });
        progress.setLastAccessedAt(LocalDateTime.now());
        if (progress.getCompletionStatus() == CompletionStatus.NOT_STARTED) {
            progress.setCompletionStatus(CompletionStatus.IN_PROGRESS);
        }
        progressRepository.save(progress);

        List<String> imageUrlList = parseImageUrls(lesson.getImageUrls());

        return LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .content(lesson.getContent())
                .audioUrl(lesson.getAudioUrl())
                .imageUrls(imageUrlList)
                .objectives(lesson.getObjectives())
                .orderIndex(lesson.getOrderIndex())
                .semesterNumber(lesson.getSemesterNumber() != null ? lesson.getSemesterNumber() : 1)
                .subjectId(lesson.getSubject().getId())
                .subjectName(lesson.getSubject().getName())
                .gradeLevel(lesson.getGradeLevel())
                .totalQuestions(lesson.getQuestions().size())
                .build();
    }

    /**
     * Returns the recommended next lesson using a subject-balancing algorithm.
     *
     * Algorithm:
     * 1. Load all student progress from DB (one query — no in-memory state).
     * 2. For each subject, compute completionRate = completed / total.
     * 3. Collect subjects that still have unfinished lessons.
     * 4. Pick the subject with the lowest completionRate; ties broken by lowest subject ID.
     * 5. Within that subject, prefer a lesson already IN_PROGRESS; otherwise pick
     *    the first NOT_STARTED lesson by orderIndex.
     *
     * Both selection steps are fully deterministic so the result only changes
     * when persisted progress data changes.
     */
    public RecommendedLessonResponse getRecommendedLesson(Integer gradeLevel, Long studentId) {
        List<Subject> subjects = subjectRepository.findByGradeLevel(gradeLevel);
        if (subjects.isEmpty()) return null;

        List<Progress> allProgress = progressRepository.findByStudentId(studentId);

        long completedCount = allProgress.stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.COMPLETED
                          || p.getCompletionStatus() == CompletionStatus.MASTERED)
                .count();
        long inProgressCount = allProgress.stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.IN_PROGRESS)
                .count();

        log.info("[Recommendation] studentId={} completedLessons={} inProgressLessons={} recommendedLesson=pending — begin calculation",
                studentId, completedCount, inProgressCount);

        Set<Long> completedIds = allProgress.stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.COMPLETED
                          || p.getCompletionStatus() == CompletionStatus.MASTERED)
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toSet());

        Set<Long> inProgressIds = allProgress.stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.IN_PROGRESS)
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toSet());

        record SubjectCandidate(Subject subject, double completionRate, List<Lesson> unfinished) {}

        List<SubjectCandidate> candidates = new ArrayList<>();
        for (Subject subject : subjects) {
            List<Lesson> lessons = lessonRepository.findBySubjectIdOrderByOrderIndexAsc(subject.getId());
            if (lessons.isEmpty()) continue;

            List<Lesson> unfinished = lessons.stream()
                    .filter(l -> !completedIds.contains(l.getId()))
                    .toList();

            if (!unfinished.isEmpty()) {
                double rate = (double)(lessons.size() - unfinished.size()) / lessons.size();
                candidates.add(new SubjectCandidate(subject, rate, unfinished));
            }
        }

        if (candidates.isEmpty()) return null;

        double minRate = candidates.stream()
                .mapToDouble(SubjectCandidate::completionRate)
                .min()
                .orElse(0);

        // Deterministic tie-breaking: lowest subject ID wins — no random shuffle
        SubjectCandidate chosen = candidates.stream()
                .filter(c -> Math.abs(c.completionRate() - minRate) < 0.001)
                .min(Comparator.comparingLong(c -> c.subject().getId()))
                .orElseThrow();

        // Prefer a lesson already IN_PROGRESS; otherwise take the first by orderIndex
        // (query is already sorted by orderIndex, so unfinished list is ordered)
        List<Lesson> unfinished = chosen.unfinished();
        Lesson recommended = unfinished.stream()
                .filter(l -> inProgressIds.contains(l.getId()))
                .findFirst()
                .orElse(unfinished.get(0));

        CompletionStatus status = allProgress.stream()
                .filter(p -> p.getLesson().getId().equals(recommended.getId()))
                .map(Progress::getCompletionStatus)
                .findFirst()
                .orElse(CompletionStatus.NOT_STARTED);

        log.info("[Recommendation] studentId={} recommendedLesson=lessonId={} title='{}' subjectId={} status={}",
                studentId, recommended.getId(), recommended.getTitle(),
                chosen.subject().getId(), status);

        return RecommendedLessonResponse.builder()
                .lessonId(recommended.getId())
                .title(recommended.getTitle())
                .subjectId(chosen.subject().getId())
                .subjectName(chosen.subject().getName())
                .orderIndex(recommended.getOrderIndex())
                .completionStatus(status)
                .build();
    }

    private List<String> parseImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(imageUrlsJson, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
