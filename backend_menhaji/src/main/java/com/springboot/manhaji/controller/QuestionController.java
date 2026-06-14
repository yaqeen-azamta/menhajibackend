package com.springboot.manhaji.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.dto.question.QuestionOption;
import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.QuestionResponse;
import com.springboot.manhaji.entity.Question;
import com.springboot.manhaji.entity.enums.QuestionType;
import com.springboot.manhaji.repository.QuestionRepository;
import com.springboot.manhaji.service.QuizService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Slf4j
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final QuizService quizService;

    @GetMapping("/{questionId}/hint")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHint(
            @PathVariable Long questionId,
            @RequestParam(defaultValue = "1") int level) {
        Map<String, Object> hint = quizService.getHint(questionId, level);
        return ResponseEntity.ok(ApiResponse.success(hint));
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getQuestionsByLesson(
            @PathVariable Long lessonId) {
        List<QuestionResponse> result = questionRepository
                .findByLessonIdOrderByIdAsc(lessonId)
                .stream()
                .map(this::buildQuestionResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private QuestionResponse buildQuestionResponse(Question question) {
        List<QuestionOption> options = List.of();

        if (question.getOptions() != null
                && !question.getOptions().isBlank()
                && question.getType() != QuestionType.READING) {
            try {
                // Try the object format [{text, imageUrl}] first.
                options = objectMapper.readValue(question.getOptions(), new TypeReference<>() {});
            } catch (Exception e) {
                try {
                    // Fall back to legacy plain-string array ["A","B","C"].
                    List<String> strings = objectMapper.readValue(
                            question.getOptions(), new TypeReference<>() {});
                    options = strings.stream()
                            .map(text -> new QuestionOption(text, ""))
                            .toList();
                } catch (Exception ex) {
                    log.warn("Failed to parse options for question {}: {}", question.getId(), ex.getMessage());
                }
            }
        }

        if (question.getType() == QuestionType.TRUE_FALSE && options.isEmpty()) {
            options = List.of(new QuestionOption("صح", ""), new QuestionOption("خطأ", ""));
        }

        // Send correctAnswer only for READING questions; it holds the reading
        // passage text that Flutter needs to display. Never exposed for other
        // types to prevent answer leakage before the student responds.
        String correctAnswer = question.getType() == QuestionType.READING
                ? question.getCorrectAnswer()
                : null;

        return QuestionResponse.builder()
                .id(question.getId())
                .type(question.getType().name())
                .questionText(question.getQuestionText())
                .options(options)
                .difficultyLevel(question.getDifficultyLevel())
                .subSkill(question.getSubSkill())
                .imageUrl(question.getImageUrl())
                .audioUrl(question.getAudioUrl())
                .correctAnswer(correctAnswer)
                .build();
    }
}
