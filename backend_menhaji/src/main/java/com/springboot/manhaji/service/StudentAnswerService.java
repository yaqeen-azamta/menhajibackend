package com.springboot.manhaji.service;

import com.springboot.manhaji.dto.request.SaveAnswerRequest;
import com.springboot.manhaji.dto.response.SaveAnswerResponse;
import com.springboot.manhaji.entity.Lesson;
import com.springboot.manhaji.entity.Question;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.entity.StudentQuestionAnswer;
import com.springboot.manhaji.entity.enums.QuestionType;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.LessonRepository;
import com.springboot.manhaji.repository.QuestionRepository;
import com.springboot.manhaji.repository.StudentQuestionAnswerRepository;
import com.springboot.manhaji.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentAnswerService {

    private final StudentRepository studentRepository;
    private final QuestionRepository questionRepository;
    private final LessonRepository lessonRepository;
    private final StudentQuestionAnswerRepository answerRepository;

private int getPointsByDifficulty(Integer difficulty) {
    if (difficulty == null) return 5;

    return switch (difficulty) {
        case 1 -> 5;
        case 2 -> 10;
        case 3 -> 15;
        case 4 -> 20;
        case 5 -> 25;
        default -> 5;
    };
}

    // Non-transactional by design: each repository call runs in its own
    // auto-transaction, so a save failure does not mark the session rollback-only
    // and break subsequent operations such as the student points update.
    public SaveAnswerResponse saveAnswer(Long userId, SaveAnswerRequest request) {
        log.info("saveAnswer → userId={}, questionId={}, lessonId={}",
                userId, request.getQuestionId(), request.getLessonId());

        // userId from JWT is User.id; resolve to the Student profile.
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question", request.getQuestionId()));

        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", request.getLessonId()));

        String answer = request.getAnswer() != null ? request.getAnswer().trim() : "";
        boolean isCorrect = evaluateAnswer(question, answer);
        double score = isCorrect ? 100.0 : 0.0;
        String feedback;
        String correctAnswerForClient = null;

        if (isCorrect) {
            feedback = "أحسنت! إجابة صحيحة 🌟";
        } else {
            feedback = "إجابة خاطئة. الإجابة الصحيحة هي: " + question.getCorrectAnswer();
            correctAnswerForClient = question.getCorrectAnswer();
        }

        // Capture dedup flag BEFORE inserting the new record so the points guard
        // in step 2 below counts only pre-existing correct answers, not the one
        // we are about to save.
        boolean alreadyAnsweredCorrectly =
                answerRepository.existsByStudentIdAndQuestionIdAndIsCorrectTrue(
                        student.getId(), question.getId());

        try {
            StudentQuestionAnswer record = new StudentQuestionAnswer();
            record.setStudent(student);
            record.setQuestion(question);
            record.setLesson(lesson);
            record.setAnswerText(answer);
            record.setIsCorrect(isCorrect);
            record.setScore(score);
            record.setAnsweredAt(LocalDateTime.now());
            record.setFeedback(feedback);
            answerRepository.save(record);
        } catch (Exception e) {
            log.error("Failed to persist student answer (non-fatal): {}", e.getMessage(), e);
        }

        int pointsAwarded = 0;
        if (isCorrect && !alreadyAnsweredCorrectly) {
            try {
                // Re-load to avoid stale state from the session above.
                Student fresh = studentRepository.findByUserId(userId).orElse(null);
                if (fresh != null) {
                    int earnedPoints =
        getPointsByDifficulty(question.getDifficultyLevel());

fresh.setTotalPoints(
    fresh.getTotalPoints() + earnedPoints
);
studentRepository.save(fresh);

pointsAwarded = earnedPoints;
                    log.info("Points awarded → userId={}, +{}pts, total={}",
                            userId, earnedPoints, fresh.getTotalPoints());
                }
            } catch (Exception e) {
                log.warn("Failed to update student points (non-fatal): {}", e.getMessage());
            }
        }

        return SaveAnswerResponse.builder()
                .isCorrect(isCorrect)
                .score(score)
                .feedback(feedback)
                .correctAnswer(correctAnswerForClient)
                .pointsAwarded(pointsAwarded)
                .build();
    }

    private boolean evaluateAnswer(Question question, String studentAnswer) {
        // TRACING and READING are scored client-side / via dedicated endpoints.
        if (question.getType() == QuestionType.TRACING
                || question.getType() == QuestionType.READING) {
            return true;
        }

        String correctAnswer = question.getCorrectAnswer() != null
                ? question.getCorrectAnswer().trim() : "";

        if (studentAnswer.isEmpty()) return false;

        switch (question.getType()) {
            case MCQ, TRUE_FALSE, IMAGE_MCQ -> {
                return correctAnswer.equalsIgnoreCase(studentAnswer);
            }
            case FILL_BLANK, ORDERING, SHORT_ANSWER -> {
                String nc = normalizeArabic(correctAnswer);
                String ns = normalizeArabic(studentAnswer);
                return nc.equals(ns) || ns.contains(nc) || nc.contains(ns);
            }
            default -> {
                return correctAnswer.equalsIgnoreCase(studentAnswer);
            }
        }
    }

    private String normalizeArabic(String text) {
        if (text == null) return "";
        return text
                .replaceAll("[\\u064B-\\u065F\\u0670]", "")
                .replaceAll("[آأإ]", "ا")
                .replace('ة', 'ه')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }
}
