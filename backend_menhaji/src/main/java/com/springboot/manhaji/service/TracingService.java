package com.springboot.manhaji.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.dto.request.TracingSubmitRequest;
import com.springboot.manhaji.dto.response.TracingHistoryEntry;
import com.springboot.manhaji.dto.response.TracingProgressResponse;
import com.springboot.manhaji.dto.response.TracingProgressResponse.CharacterProgress;
import com.springboot.manhaji.dto.response.TracingQuestionResponse;
import com.springboot.manhaji.dto.response.TracingResultResponse;
import com.springboot.manhaji.entity.Progress;
import com.springboot.manhaji.entity.Question;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.entity.TracingAnswer;
import com.springboot.manhaji.entity.TracingQuestion;
import com.springboot.manhaji.entity.enums.CompletionStatus;
import com.springboot.manhaji.entity.enums.QuestionType;
import com.springboot.manhaji.exception.BadRequestException;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.ProgressRepository;
import com.springboot.manhaji.repository.QuestionRepository;
import com.springboot.manhaji.repository.StudentRepository;
import com.springboot.manhaji.repository.TracingAnswerRepository;
import com.springboot.manhaji.repository.TracingQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TracingService {

    private static final double MAX_ACCEPTABLE_ERROR = 20.0;
    private static final double MASTERY_THRESHOLD = 80.0;

    private final TracingQuestionRepository tracingQuestionRepository;
    private final TracingAnswerRepository tracingAnswerRepository;
    private final QuestionRepository questionRepository;
    private final StudentRepository studentRepository;
    private final ProgressRepository progressRepository;
    private final ObjectMapper objectMapper;

    public TracingQuestionResponse getTracingQuestion(Long questionId) {
        TracingQuestion tq = tracingQuestionRepository.findByQuestionId(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("TracingQuestion", questionId));
        return toResponse(tq);
    }

    public List<TracingQuestionResponse> getTracingQuestionsByLesson(Long lessonId) {
        List<TracingQuestion> list =
                tracingQuestionRepository.findByLessonIdOrderByQuestionIdAsc(lessonId);
        if (list.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No tracing questions found for lesson id=" + lessonId);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // userId is users.id from JWT; translate to students.id for internal queries
    public TracingProgressResponse getStudentProgress(Long userId) {
        Long studentId = resolveStudentId(userId);

        long totalAttempts = tracingAnswerRepository.countByStudentId(studentId);
        long totalCorrect  = tracingAnswerRepository.countCorrectByStudentId(studentId);
        double overallAvg  = tracingAnswerRepository.findAverageAccuracyByStudentId(studentId)
                .orElse(0.0);

        List<Long> attemptedQuestionIds =
                tracingAnswerRepository.findAttemptedQuestionIdsByStudentId(studentId);

        List<CharacterProgress> perChar = buildCharacterProgress(studentId, attemptedQuestionIds);

        int totalStars   = perChar.stream().mapToInt(CharacterProgress::getBestStars).sum();
        int maxStars     = (int) totalAttempts * 3;
        long masteredCnt = perChar.stream().filter(CharacterProgress::isMastered).count();
        double masteryRate = perChar.isEmpty() ? 0.0
                : Math.round(masteredCnt * 1000.0 / perChar.size()) / 10.0;

        return TracingProgressResponse.builder()
                .studentId(userId)
                .totalAttempts((int) totalAttempts)
                .totalCorrect((int) totalCorrect)
                .overallAccuracy(round1(overallAvg))
                .totalStars(totalStars)
                .maxPossibleStars(maxStars)
                .masteryRate(masteryRate)
                .characterProgress(perChar)
                .build();
    }

    public Page<TracingHistoryEntry> getStudentHistory(Long userId, Pageable pageable) {
        Long studentId = resolveStudentId(userId);
        Page<TracingAnswer> page =
                tracingAnswerRepository.findByStudentIdOrderBySubmittedAtDesc(studentId, pageable);
        List<TracingHistoryEntry> entries = page.getContent().stream()
                .map(this::toHistoryEntry)
                .collect(Collectors.toList());
        return new PageImpl<>(entries, pageable, page.getTotalElements());
    }

    public List<TracingHistoryEntry> getStudentHistoryForQuestion(Long userId, Long questionId) {
        Long studentId = resolveStudentId(userId);
        return tracingAnswerRepository
                .findByStudentIdAndQuestionIdOrderBySubmittedAtDesc(studentId, questionId)
                .stream()
                .map(this::toHistoryEntry)
                .collect(Collectors.toList());
    }

    @Transactional
    public TracingResultResponse submitTracingAnswer(Long userId, TracingSubmitRequest request) {

        Question question = questionRepository
                .findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question", request.getQuestionId()));

        if (question.getType() != QuestionType.TRACING) {
            throw new BadRequestException(
                    "Question " + request.getQuestionId() + " is not a tracing question");
        }

        TracingQuestion tq = tracingQuestionRepository
                .findByQuestionId(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("TracingQuestion", request.getQuestionId()));

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        Long studentId = student.getId();

        boolean hasDrawing = request.getDrawingPoints() != null && !request.getDrawingPoints().isEmpty();

        double serverAccuracy  = hasDrawing ? 100.0 : 0.0;
        double clientAccuracy  = serverAccuracy;
        double finalAccuracy   = serverAccuracy;
        double requiredAccuracy = 0.0;
       boolean isCorrect = hasDrawing;
int stars = hasDrawing ? 3 : 0;

int score = hasDrawing
        ? getPointsByDifficulty(question.getDifficultyLevel())
        : 0;



        int attemptNumber = tracingAnswerRepository
                .countByStudentIdAndQuestionId(studentId, request.getQuestionId()) + 1;

        String feedback = hasDrawing ? "Great job! 🎉" : "Please try again";

        TracingAnswer answer = new TracingAnswer();
        answer.setStudentId(studentId);
        answer.setQuestion(question);
        answer.setDrawingPointsJson(serializeDrawingPoints(request.getDrawingPoints()));
        answer.setClientAccuracy(clientAccuracy);
        answer.setServerAccuracy(serverAccuracy);
        answer.setFinalAccuracy(finalAccuracy);
        answer.setScore(score);
        answer.setStars(stars);
        answer.setIsCorrect(isCorrect);
        answer.setAttemptNumber(attemptNumber);
        answer.setFeedback(feedback);
        answer.setSubmittedAt(LocalDateTime.now());

        TracingAnswer saved = tracingAnswerRepository.save(answer);

        if (isCorrect && attemptNumber == 1) {
            student.setTotalPoints(student.getTotalPoints() + score);
            studentRepository.save(student);
        }

        updateLessonProgress(student, question, finalAccuracy);

        return TracingResultResponse.builder()
                .success(true)
                .answerId(saved.getId())
                .questionId(question.getId())
                .character(tq.getCharacter())
                .serverAccuracy(serverAccuracy)
                .clientAccuracy(clientAccuracy)
                .finalAccuracy(finalAccuracy)
                .score(score)
                .stars(stars)
                .isCorrect(isCorrect)
                .attemptNumber(attemptNumber)
                .feedback(feedback)
                .requiredAccuracy(requiredAccuracy)
                .build();
    }

    double calculateServerAccuracy(List<TracingSubmitRequest.DrawingPoint> studentPoints,
                                   String expectedPointsJson) {
        try {
            List<Map<String, Double>> expected = objectMapper.readValue(
                    expectedPointsJson, new TypeReference<>() {});

            if (expected.isEmpty() || studentPoints.isEmpty()) return 0.0;

            double forwardSum = 0;
            for (Map<String, Double> ep : expected) {
                double ex = ep.get("x"), ey = ep.get("y");
                double minDist = Double.MAX_VALUE;
                for (TracingSubmitRequest.DrawingPoint sp : studentPoints) {
                    double d = euclidean(sp.getX(), sp.getY(), ex, ey);
                    if (d < minDist) minDist = d;
                }
                forwardSum += minDist;
            }
            double forwardAvg = forwardSum / expected.size();

            double backwardSum = 0;
            for (TracingSubmitRequest.DrawingPoint sp : studentPoints) {
                double minDist = Double.MAX_VALUE;
                for (Map<String, Double> ep : expected) {
                    double d = euclidean(sp.getX(), sp.getY(), ep.get("x"), ep.get("y"));
                    if (d < minDist) minDist = d;
                }
                backwardSum += minDist;
            }
            double backwardAvg = backwardSum / studentPoints.size();

            double avgError  = (forwardAvg + backwardAvg) / 2.0;
            double accuracy  = Math.max(0.0, 100.0 * (1.0 - avgError / MAX_ACCEPTABLE_ERROR));
            return round1(accuracy);

        } catch (Exception e) {
            log.warn("Server accuracy calculation failed: {}", e.getMessage());
            return -1.0;
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Long resolveStudentId(Long userId) {
        return studentRepository.findByUserId(userId)
                .map(Student::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));
    }

    private void updateLessonProgress(Student student, Question question, double finalAccuracy) {
        if (question.getLesson() == null) return;

        Optional<Progress> existingOpt = progressRepository.findByStudentIdAndLessonId(
                student.getId(), question.getLesson().getId());

        Progress progress = existingOpt.orElseGet(() -> {
            Progress p = new Progress();
            p.setStudent(student);
            p.setLesson(question.getLesson());
            p.setMasteryLevel(0.0);
            p.setCompletionStatus(CompletionStatus.IN_PROGRESS);
            return p;
        });

        double newMastery = Math.max(progress.getMasteryLevel(), finalAccuracy);
        progress.setMasteryLevel(round1(newMastery));
        progress.setLastAccessedAt(LocalDateTime.now());

        if (newMastery >= 80.0 && progress.getCompletionStatus() != CompletionStatus.COMPLETED) {
            progress.setCompletionStatus(CompletionStatus.COMPLETED);
            progress.setCompletedAt(LocalDateTime.now());
        } else if (progress.getCompletionStatus() == CompletionStatus.NOT_STARTED) {
            progress.setCompletionStatus(CompletionStatus.IN_PROGRESS);
        }

        progressRepository.save(progress);
    }

    private List<CharacterProgress> buildCharacterProgress(Long studentId, List<Long> questionIds) {
        List<CharacterProgress> result = new ArrayList<>();
        for (Long qid : questionIds) {
            int attempts = tracingAnswerRepository.countByStudentIdAndQuestionId(studentId, qid);
            double bestAcc = tracingAnswerRepository
                    .findBestAccuracyByStudentIdAndQuestionId(studentId, qid).orElse(0.0);
            int bestStars = tracingAnswerRepository
                    .findBestStarsByStudentIdAndQuestionId(studentId, qid).orElse(0);

            Optional<TracingAnswer> latest = tracingAnswerRepository
                    .findLatestByStudentIdAndQuestionId(studentId, qid);
            double latestAcc = latest.map(TracingAnswer::getFinalAccuracy).orElse(0.0);

            Optional<TracingQuestion> tqOpt = tracingQuestionRepository.findByQuestionId(qid);
            String character   = tqOpt.map(TracingQuestion::getCharacter).orElse("?");
            String displayName = tqOpt.map(TracingQuestion::getDisplayName).orElse(null);

            result.add(CharacterProgress.builder()
                    .questionId(qid)
                    .character(character)
                    .displayName(displayName)
                    .attempts(attempts)
                    .bestStars(bestStars)
                    .bestAccuracy(round1(bestAcc))
                    .latestAccuracy(round1(latestAcc))
                    .mastered(bestAcc >= MASTERY_THRESHOLD)
                    .build());
        }
        result.sort(Comparator.comparing(CharacterProgress::getCharacter));
        return result;
    }

    private TracingQuestionResponse toResponse(TracingQuestion tq) {
        Question q = tq.getQuestion();
        double effective = tq.getExpectedAccuracy()
                * (1.0 - tq.getTolerancePercentage() / 100.0);

        return TracingQuestionResponse.builder()
                .questionId(q.getId())
                .lessonId(q.getLesson() != null ? q.getLesson().getId() : null)
                .character(tq.getCharacter())
                .displayName(tq.getDisplayName())
                .language(tq.getLanguage())
                .characterType(tq.getCharacterType())
                .difficultyLevel(q.getDifficultyLevel())
                .svgPath(tq.getSvgPath())
                .expectedPointsJson(tq.getExpectedPointsJson())
                .expectedAccuracy(tq.getExpectedAccuracy())
                .tolerancePercentage(tq.getTolerancePercentage())
                .effectiveRequiredAccuracy(round1(effective))
                .strokeCount(tq.getStrokeCount())
                .strokeOrderJson(tq.getStrokeOrderJson())
                .guideImageUrl(tq.getGuideImageUrl())
                .audioUrl(q.getAudioUrl())
                .build();
    }

    private TracingHistoryEntry toHistoryEntry(TracingAnswer ta) {
        Question q = ta.getQuestion();
        Optional<TracingQuestion> tq = tracingQuestionRepository.findByQuestionId(q.getId());

        return TracingHistoryEntry.builder()
                .answerId(ta.getId())
                .questionId(q.getId())
                .character(tq.map(TracingQuestion::getCharacter).orElse("?"))
                .displayName(tq.map(TracingQuestion::getDisplayName).orElse(null))
                .finalAccuracy(ta.getFinalAccuracy())
                .serverAccuracy(ta.getServerAccuracy())
                .clientAccuracy(ta.getClientAccuracy())
                .score(ta.getScore())
                .stars(ta.getStars())
                .isCorrect(ta.getIsCorrect())
                .attemptNumber(ta.getAttemptNumber())
                .feedback(ta.getFeedback())
                .submittedAt(ta.getSubmittedAt())
                .build();
    }

    private String serializeDrawingPoints(List<TracingSubmitRequest.DrawingPoint> points) {
        if (points == null || points.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(points);
        } catch (Exception e) {
            log.warn("Could not serialize drawing points: {}", e.getMessage());
            return null;
        }
    }

    private static double euclidean(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
   private int getPointsByDifficulty(Integer difficulty) {

    if (difficulty == null) {
        return 5;
    }

    return switch (difficulty) {
        case 1 -> 5;
        case 2 -> 10;
        case 3 -> 15;
        case 4 -> 20;
        case 5 -> 25;
        default -> 5;
    };
}
}