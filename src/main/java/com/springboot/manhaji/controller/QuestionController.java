package com.springboot.manhaji.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.springboot.manhaji.dto.question.QuestionOption;
import com.springboot.manhaji.dto.response.QuestionResponse;

import com.springboot.manhaji.entity.Question;
import com.springboot.manhaji.entity.enums.QuestionType;

import com.springboot.manhaji.repository.QuestionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@CrossOrigin("*")
public class QuestionController {

    private final QuestionRepository questionRepository;

    private final ObjectMapper objectMapper;

    @GetMapping("/lesson/{lessonId}")
    public List<QuestionResponse> getQuestionsByLesson(
            @PathVariable Long lessonId
    ) {

        List<Question> questions =
                questionRepository
                        .findByLessonIdOrderByIdAsc(lessonId);

        return questions.stream()
                .map(this::buildQuestionResponse)
                .toList();
    }

   private QuestionResponse buildQuestionResponse(
        Question question
) {

    List<QuestionOption> options = List.of();

    try {

        // IMAGE_MCQ already objects
        if (question.getType() ==
                QuestionType.IMAGE_MCQ) {

            options = objectMapper.readValue(
                    question.getOptions(),
                    new TypeReference<
                            List<QuestionOption>>() {}
            );
        }

        // NORMAL QUESTIONS
        else {

            List<String> oldOptions =
                    objectMapper.readValue(
                            question.getOptions(),
                            new TypeReference<
                                    List<String>>() {}
                    );

            options = oldOptions.stream()
                    .map(text ->
                            new QuestionOption(
                                    text,
                                    ""
                            )
                    )
                    .toList();
        }

    } catch (Exception e) {

        e.printStackTrace();
    }

    return QuestionResponse.builder()
            .id(question.getId())
            .type(question.getType().name())
            .questionText(question.getQuestionText())
            .options(options)
            .difficultyLevel(question.getDifficultyLevel())
            .subSkill(question.getSubSkill())
            .imageUrl(question.getImageUrl())
            .audioUrl(question.getAudioUrl())
            .build();
}
}