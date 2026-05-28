package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.entity.Question;
import com.springboot.manhaji.entity.StudentQuestionAnswer;
import com.springboot.manhaji.repository.QuestionRepository;
import com.springboot.manhaji.repository.StudentQuestionAnswerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/student-answers")
@RequiredArgsConstructor
@Slf4j
public class StudentQuestionAnswerController {

    private final StudentQuestionAnswerRepository repository;
    private final QuestionRepository questionRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<StudentQuestionAnswer>> saveAnswer(
            @RequestBody StudentQuestionAnswer answer,
            Authentication authentication) {

        try {

            // ───────── GET STUDENT FROM JWT ─────────

            if (authentication != null &&
                    authentication.getPrincipal() instanceof Long) {

                answer.setStudentId(
                        (Long) authentication.getPrincipal());
            }

            // ───────── VALIDATION ─────────

            if (answer.getStudentId() == null) {

                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("studentId مطلوب"));
            }

            if (answer.getQuestionId() == null) {

                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("questionId مطلوب"));
            }

            // ───────── GET QUESTION ─────────

            Question question = questionRepository
                    .findById(answer.getQuestionId())
                    .orElse(null);

            if (question == null) {

                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(
                                "السؤال غير موجود: "
                                        + answer.getQuestionId()));
            }

            // ───────── GET STUDENT ANSWER ─────────

            String studentAnswer = "";

            // TEXT ANSWER
            if (answer.getAnswerText() != null &&
                    !answer.getAnswerText().isBlank()) {

                studentAnswer =
                        answer.getAnswerText().trim();
            }

            // MCQ / IMAGE MCQ
            else if (answer.getSelectedOption() != null &&
                    !answer.getSelectedOption().isBlank()) {

                studentAnswer =
                        answer.getSelectedOption().trim();
            }

            // ───────── CORRECT ANSWER ─────────

            String correctAnswer =
                    question.getCorrectAnswer() != null
                            ? question.getCorrectAnswer().trim()
                            : "";

            // ───────── QUESTION TYPE ─────────

            String questionType =
                    question.getType() != null
                            ? question.getType().name()
                            : "";

            log.debug(
                    "saveAnswer: questionId={} lessonId={} studentId={} type={} studentAnswer={}",
                    answer.getQuestionId(),
                    answer.getLessonId(),
                    answer.getStudentId(),
                    questionType,
                    studentAnswer
            );

            // ───────── CHECK ANSWER ─────────

            boolean correct;

            // TRACING + IMAGE MCQ
            if ("TRACING".equalsIgnoreCase(questionType)
                    || "IMAGE_MCQ".equalsIgnoreCase(questionType)) {

                correct = true;
            }

            // NORMAL QUESTIONS
            else {

                correct =
                        !studentAnswer.isEmpty()
                                && !correctAnswer.isEmpty()
                                && correctAnswer.equalsIgnoreCase(studentAnswer);
            }

            answer.setIsCorrect(correct ? 1 : 0);

            // ───────── SCORE SYSTEM ─────────

            double score = 0;

            if (correct) {

                Integer difficulty =
                        question.getDifficultyLevel();

                if (difficulty == null) {
                    difficulty = 1;
                }

                score = switch (difficulty) {

                    case 2 -> 10;

                    case 3 -> 15;

                    default -> 5;
                };
            }

            answer.setScore(score);

            // ───────── REQUIRED FIELDS ─────────

            if (answer.getAttemptNumber() == null) {

                answer.setAttemptNumber(1);
            }

            if (answer.getAnsweredAt() == null) {

                answer.setAnsweredAt(LocalDateTime.now());
            }

            // ───────── PREVENT DUPLICATE ANSWERS ─────────

            Optional<StudentQuestionAnswer> existingAnswer =
                    repository.findByStudentIdAndQuestionId(
                            answer.getStudentId(),
                            answer.getQuestionId()
                    );

            StudentQuestionAnswer saved;

            // UPDATE OLD ANSWER
            if (existingAnswer.isPresent()) {

                StudentQuestionAnswer oldAnswer =
                        existingAnswer.get();

                oldAnswer.setAnswerText(answer.getAnswerText());
                oldAnswer.setSelectedOption(answer.getSelectedOption());
                oldAnswer.setIsCorrect(answer.getIsCorrect());
                oldAnswer.setScore(answer.getScore());
                oldAnswer.setAttemptNumber(answer.getAttemptNumber());
                oldAnswer.setAnsweredAt(answer.getAnsweredAt());

                saved = repository.save(oldAnswer);
            }

            // CREATE NEW ANSWER
            else {

                saved = repository.save(answer);
            }

            log.debug(
                    "saveAnswer: saved id={} isCorrect={} score={}",
                    saved.getId(),
                    saved.getIsCorrect(),
                    saved.getScore()
            );

            return ResponseEntity.ok(
                    ApiResponse.success(
                            "تم حفظ الإجابة",
                            saved
                    )
            );

        } catch (Exception e) {

            log.error(
                    "saveAnswer failed: {}",
                    e.getMessage(),
                    e
            );

            return ResponseEntity.internalServerError()
                    .body(
                            ApiResponse.error(
                                    "فشل في حفظ الإجابة: "
                                            + e.getMessage()
                            )
                    );
        }
    }
}