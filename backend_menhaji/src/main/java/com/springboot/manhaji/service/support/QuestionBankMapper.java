package com.springboot.manhaji.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.dto.response.LessonSummary;
import com.springboot.manhaji.dto.response.QuestionBankItem;
import com.springboot.manhaji.dto.response.SubjectSummary;
import com.springboot.manhaji.entity.Lesson;
import com.springboot.manhaji.entity.Question;
import com.springboot.manhaji.entity.Subject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared helpers for the teacher/admin question-bank viewer. Keeps DTO
 * construction — especially the JSON {@code options} parse — in one place.
 */
@Component
public class QuestionBankMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST =
            new TypeReference<>() {};

    public SubjectSummary toSubjectSummary(Subject subject) {
        int lessonCount = subject.getLessons() == null ? 0 : subject.getLessons().size();
        int questionCount = subject.getLessons() == null ? 0 :
                subject.getLessons().stream()
                        .mapToInt(l -> l.getQuestions() == null ? 0 : l.getQuestions().size())
                        .sum();
        return SubjectSummary.builder()
                .id(subject.getId())
                .name(subject.getName())
                .gradeLevel(subject.getGradeLevel())
                .lessonCount(lessonCount)
                .questionCount(questionCount)
                .build();
    }

    public QuestionBankItem toQuestionItem(Question question) {
        return QuestionBankItem.builder()
                .id(question.getId())
                .type(question.getType())
                .questionText(question.getQuestionText())
                .correctAnswer(question.getCorrectAnswer())
                .options(parseOptions(question.getOptions()))
                .difficultyLevel(question.getDifficultyLevel())
                .subSkill(question.getSubSkill())
                .imageUrl(question.getImageUrl())
                .audioUrl(question.getAudioUrl())
                .lessonId(question.getLesson() != null ? question.getLesson().getId() : null)
                .lessonTitle(question.getLesson() != null ? question.getLesson().getTitle() : null)
                .build();
    }

    /**
     * Unique lessons from a question list, sorted by {@code orderIndex}. Used to
     * populate the lesson filter dropdown.
     */
    public List<LessonSummary> collectLessonFilters(List<Question> questions) {
        Map<Long, Lesson> lessons = new LinkedHashMap<>();
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (Question q : questions) {
            Lesson l = q.getLesson();
            if (l == null) {
                continue;
            }
            lessons.putIfAbsent(l.getId(), l);
            counts.merge(l.getId(), 1, Integer::sum);
        }
        List<LessonSummary> result = new ArrayList<>();
        for (Lesson l : lessons.values()) {
            result.add(LessonSummary.builder()
                    .id(l.getId())
                    .title(l.getTitle())
                    .orderIndex(l.getOrderIndex())
                    .questionCount(counts.getOrDefault(l.getId(), 0))
                    .build());
        }
        result.sort(Comparator.comparing(
                LessonSummary::getOrderIndex,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private List<String> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException e) {
            // Seed data uses well-formed JSON arrays; if a row is malformed,
            // return the raw string as a single-element list so the UI can still show it.
            return List.of(json);
        }
    }
}
