package com.springboot.manhaji.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.dto.response.ProgressReportResponse;
import com.springboot.manhaji.entity.Attempt;
import com.springboot.manhaji.entity.Progress;
import com.springboot.manhaji.entity.ProgressReport;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.entity.Subject;
import com.springboot.manhaji.entity.enums.RiskLevel;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.AttemptRepository;
import com.springboot.manhaji.repository.ProgressReportRepository;
import com.springboot.manhaji.repository.ProgressRepository;
import com.springboot.manhaji.repository.StudentRepository;
import com.springboot.manhaji.repository.SubjectRepository;
import com.springboot.manhaji.service.ai.GeminiService;
import com.springboot.manhaji.service.support.ProgressMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProgressReportService {

    private final StudentRepository studentRepository;
    private final ProgressRepository progressRepository;
    private final AttemptRepository attemptRepository;
    private final SubjectRepository subjectRepository;
    private final ProgressReportRepository reportRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    private final ProgressMetrics metrics;

    @Transactional
    public ProgressReportResponse generateReport(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        String performanceData = buildPerformanceData(student);

        String aiResponse = geminiService.generateProgressReport(
                student.getStudentName(), student.getGradeLevel(), performanceData);

        String summary;
        RiskLevel riskLevel = RiskLevel.LOW;

        if (aiResponse != null) {
            try {
                JsonNode json = objectMapper.readTree(aiResponse);
                summary = json.has("summary") ? json.get("summary").asText() : aiResponse;
                if (json.has("riskLevel")) {
                    riskLevel = RiskLevel.valueOf(json.get("riskLevel").asText("LOW"));
                }
            } catch (Exception e) {
                summary = aiResponse;
                log.warn("Could not parse AI report as JSON, using raw text");
            }
        } else {
            summary = buildFallbackSummary(student);
            riskLevel = determineFallbackRisk(student);
        }

        ProgressReport report = new ProgressReport();
        report.setStudent(student);
        report.setPeriodStart(LocalDate.now().minusDays(30));
        report.setPeriodEnd(LocalDate.now());
        report.setSummary(summary);
        report.setRiskLevel(riskLevel);
        report = reportRepository.save(report);

        return toResponse(report);
    }

    public List<ProgressReportResponse> getReports(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));
        return reportRepository.findByStudentIdOrderByGeneratedAtDesc(student.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String buildPerformanceData(Student student) {
        List<Progress> progressRecords = progressRepository.findByStudentId(student.getId());
        List<Attempt> attempts = attemptRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        List<Subject> subjects = subjectRepository.findByGradeLevel(student.getGradeLevel());

        int completed = metrics.countCompleted(progressRecords);
        double avgMastery = metrics.averageMastery(progressRecords);
        double avgScore = metrics.averageGradedScore(attempts);

        Map<Long, List<Progress>> bySubject = progressRecords.stream()
                .filter(p -> p.getLesson() != null && p.getLesson().getSubject() != null)
                .collect(Collectors.groupingBy(p -> p.getLesson().getSubject().getId()));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("النقاط: %d, السلسلة: %d\n", student.getTotalPoints(), student.getCurrentStreak()));
        sb.append(String.format("دروس مكتملة: %d, متوسط الإتقان: %.1f%%, متوسط الدرجات: %.1f%%\n",
                completed, avgMastery, avgScore));

        for (Subject subject : subjects) {
            List<Progress> sp = bySubject.getOrDefault(subject.getId(), List.of());
            double subMastery = metrics.averageMastery(sp);
            int subCompleted = metrics.countCompleted(sp);
            sb.append(String.format("%s: إتقان %.0f%%, مكتمل %d\n", subject.getName(), subMastery, subCompleted));
        }
        return sb.toString();
    }

    private String buildFallbackSummary(Student student) {
        List<Progress> progress = progressRepository.findByStudentId(student.getId());
        int completed = metrics.countCompleted(progress);
        double avg = metrics.averageMastery(progress);
        return String.format("أكمل الطالب %s عدد %d درس بمتوسط إتقان %.0f%%.",
                student.getStudentName(), completed, avg);
    }

    private RiskLevel determineFallbackRisk(Student student) {
        List<Progress> progress = progressRepository.findByStudentId(student.getId());
        double avg = metrics.averageMastery(progress);
        if (avg >= 70) return RiskLevel.LOW;
        if (avg >= 40) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }

    private ProgressReportResponse toResponse(ProgressReport report) {
        return ProgressReportResponse.builder()
                .id(report.getId())
                .studentId(report.getStudent().getUser().getId())
                .studentName(report.getStudent().getStudentName())
                .periodStart(report.getPeriodStart())
                .periodEnd(report.getPeriodEnd())
                .summary(report.getSummary())
                .riskLevel(report.getRiskLevel())
                .generatedAt(report.getGeneratedAt())
                .build();
    }
}
