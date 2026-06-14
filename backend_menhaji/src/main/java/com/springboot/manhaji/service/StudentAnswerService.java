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
import org.springframework.transaction.annotation.Transactional;

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

    // @Transactional ensures question and lesson loaded below are managed entities
    // within the same Hibernate session as answerRepository.save(). The student
    // entity is fetched OUTSIDE this method (via resolveStudent in the controller)
    // and would be detached — we use getReferenceById to obtain a managed proxy.
    // Exceptions from the save block are caught so a persistence failure never
    // causes the points update below to roll back (it runs in the same transaction
    // but Hibernate's session is not corrupted by a caught application-level failure).
    @Transactional
    public SaveAnswerResponse saveAnswer(Student student, SaveAnswerRequest request) {
        log.info("[SAVE-ANSWER] incoming — studentId={} questionId={} lessonId={} answer='{}'",
                student.getId(), request.getQuestionId(), request.getLessonId(), request.getAnswer());

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
        // below counts only pre-existing correct answers, not the one we are about to save.
        boolean alreadyAnsweredCorrectly =
                answerRepository.existsByStudentIdAndQuestionIdAndIsCorrectTrue(
                        student.getId(), question.getId());

        log.info("[SAVE-ANSWER] before save — studentId={} questionId={} lessonId={} answer='{}' isCorrect={} alreadyCorrect={}",
                student.getId(), question.getId(), lesson.getId(), answer, isCorrect, alreadyAnsweredCorrectly);

        try {
            StudentQuestionAnswer record = new StudentQuestionAnswer();
            // student was fetched outside this transaction — use getReferenceById to
            // obtain a managed proxy so Hibernate does not throw DetachedObjectException.
            record.setStudent(studentRepository.getReferenceById(student.getId()));
            record.setQuestion(question);  // loaded above — managed in this transaction
            record.setLesson(lesson);      // loaded above — managed in this transaction
            record.setAnswerText(answer);
            record.setIsCorrect(isCorrect);
            record.setScore(score);
            record.setAnsweredAt(LocalDateTime.now());
            record.setFeedback(feedback);

            StudentQuestionAnswer saved = answerRepository.save(record);
            log.info("[SAVE-ANSWER] after save — id={} studentId={} questionId={}",
                    saved.getId(), student.getId(), question.getId());
        } catch (Exception e) {
            log.error("[SAVE-ANSWER] FAILED to persist answer — studentId={} questionId={} lessonId={} error={}",
                    student.getId(), question.getId(), lesson.getId(), e.getMessage(), e);
        }

        int pointsAwarded = 0;
        if (isCorrect && !alreadyAnsweredCorrectly) {
            try {
                Student fresh = studentRepository.findById(student.getId()).orElse(null);
                if (fresh != null) {
                    int earnedPoints = getPointsByDifficulty(question.getDifficultyLevel());
                    fresh.setTotalPoints(fresh.getTotalPoints() + earnedPoints);
                    studentRepository.save(fresh);
                    pointsAwarded = earnedPoints;
                    log.info("[SAVE-ANSWER] points awarded — studentId={} +{}pts total={}",
                            fresh.getId(), earnedPoints, fresh.getTotalPoints());
                }
            } catch (Exception e) {
                log.warn("[SAVE-ANSWER] failed to update points (non-fatal): {}", e.getMessage());
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
