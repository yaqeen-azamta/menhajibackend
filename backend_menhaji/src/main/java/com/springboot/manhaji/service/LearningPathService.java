package com.springboot.manhaji.service;

import com.springboot.manhaji.dto.response.LearningPathResponse;
import com.springboot.manhaji.entity.LearningPath;
import com.springboot.manhaji.entity.Lesson;
import com.springboot.manhaji.entity.Progress;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.entity.Subject;
import com.springboot.manhaji.entity.enums.CompletionStatus;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.LearningPathRepository;
import com.springboot.manhaji.repository.LessonRepository;
import com.springboot.manhaji.repository.ProgressRepository;
import com.springboot.manhaji.repository.StudentRepository;
import com.springboot.manhaji.repository.SubjectRepository;
import com.springboot.manhaji.service.ai.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningPathService {

    private final StudentRepository studentRepository;
    private final ProgressRepository progressRepository;
    private final SubjectRepository subjectRepository;
    private final LessonRepository lessonRepository;
    private final LearningPathRepository learningPathRepository;
    private final GeminiService geminiService;

    @Transactional
    public LearningPathResponse generatePath(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        String weakAreas = buildWeakAreas(student);
        String completedLessons = buildCompletedLessons(student);

        String aiResponse = geminiService.generateLearningPath(
                student.getStudentName(), student.getGradeLevel(), weakAreas, completedLessons);

        String recommendations = aiResponse != null ? aiResponse : buildFallbackRecommendations(student);

        LearningPath path = learningPathRepository.findByStudentId(student.getId())
                .orElse(new LearningPath());
        path.setStudent(student);
        path.setRecommendations(recommendations);
        path = learningPathRepository.save(path);

        return toResponse(path);
    }

    public LearningPathResponse getPath(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));
        LearningPath path = learningPathRepository.findByStudentId(student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("LearningPath for student", userId));
        return toResponse(path);
    }

    private String buildWeakAreas(Student student) {
        List<Progress> progressRecords = progressRepository.findByStudentId(student.getId());
        List<Subject> subjects = subjectRepository.findByGradeLevel(student.getGradeLevel());

        Map<Long, List<Progress>> bySubject = progressRecords.stream()
                .filter(p -> p.getLesson() != null && p.getLesson().getSubject() != null)
                .collect(Collectors.groupingBy(p -> p.getLesson().getSubject().getId()));

        StringBuilder sb = new StringBuilder();
        for (Subject subject : subjects) {
            List<Progress> sp = bySubject.getOrDefault(subject.getId(), List.of());
            double avg = sp.stream()
                    .mapToDouble(p -> p.getMasteryLevel() == null ? 0.0 : p.getMasteryLevel())
                    .average().orElse(0.0);
            if (avg < 60) {
                sb.append(String.format("%s (إتقان %.0f%%)\n", subject.getName(), avg));
            }
        }
        return sb.isEmpty() ? "لا توجد مواضيع ضعيفة" : sb.toString();
    }

    private String buildCompletedLessons(Student student) {
        List<Progress> completed = progressRepository
                .findByStudentIdAndCompletionStatus(student.getId(), CompletionStatus.COMPLETED);
        if (completed.isEmpty()) return "لا توجد دروس مكتملة بعد";

        return completed.stream()
                .filter(p -> p.getLesson() != null)
                .map(p -> p.getLesson().getTitle())
                .collect(Collectors.joining("، "));
    }

    private String buildFallbackRecommendations(Student student) {
        List<Progress> progressRecords = progressRepository.findByStudentId(student.getId());
        List<Lesson> allLessons = lessonRepository
                .findByGradeLevelOrderByOrderIndexAsc(student.getGradeLevel());

        List<Long> completedIds = progressRecords.stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.COMPLETED && p.getLesson() != null)
                .map(p -> p.getLesson().getId())
                .toList();

        List<String> pending = allLessons.stream()
                .filter(l -> !completedIds.contains(l.getId()))
                .limit(5)
                .map(Lesson::getTitle)
                .toList();

        return String.format("{\"reviewLessons\": [], \"activities\": [\"مراجعة الدروس السابقة\"], \"tips\": [\"استمر في التعلم!\"], \"pendingLessons\": %s}",
                pending);
    }

    private LearningPathResponse toResponse(LearningPath path) {
        return LearningPathResponse.builder()
                .id(path.getId())
                .studentId(path.getStudent().getUser().getId())
                .studentName(path.getStudent().getStudentName())
                .recommendations(path.getRecommendations())
                .generatedAt(path.getGeneratedAt())
                .build();
    }
}
