package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.entity.Lesson;
import com.springboot.manhaji.entity.Question;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.LessonRepository;
import com.springboot.manhaji.repository.QuestionRepository;
import com.springboot.manhaji.service.ai.TtsService;
import com.springboot.manhaji.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
@Slf4j
public class AudioController {

    private final TtsService ttsService;
    private final FileStorageService fileStorageService;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;

    @PostMapping("/lesson/{lessonId}/narrate")
    public ResponseEntity<ApiResponse<Map<String, String>>> narrateLesson(@PathVariable Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        // Return cached audio if already generated
        if (lesson.getAudioUrl() != null && !lesson.getAudioUrl().isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("audioUrl", lesson.getAudioUrl())));
        }

        if (!ttsService.isAvailable()) {
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("message", "خدمة النطق غير متوفرة حالياً")));
        }

        try {
            // Determine language from subject
            String language = detectLanguage(lesson);

            // Synthesize the lesson content
            String textToSpeak = lesson.getTitle() + ". " + lesson.getContent();
            // Limit to 5000 chars for TTS
            if (textToSpeak.length() > 5000) {
                textToSpeak = textToSpeak.substring(0, 5000);
            }

            byte[] audio = ttsService.synthesize(textToSpeak, language);
            if (audio == null) {
                return ResponseEntity.ok(ApiResponse.success(
                        Map.of("message", "فشل في إنشاء الصوت")));
            }

            // Save audio file
            String filename = "lesson_" + lessonId + ".mp3";
            String audioUrl = fileStorageService.saveAudio(audio, filename);

            // Update lesson with audio URL
            lesson.setAudioUrl(audioUrl);
            lessonRepository.save(lesson);

            log.info("Generated audio for lesson {}: {}", lessonId, audioUrl);
            return ResponseEntity.ok(ApiResponse.success(Map.of("audioUrl", audioUrl)));

        } catch (Exception e) {
            log.error("Failed to generate audio for lesson {}: {}", lessonId, e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error("حدث خطأ في إنشاء الصوت"));
        }
    }

    @PostMapping("/question/{questionId}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readQuestion(@PathVariable Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

        if (!ttsService.isAvailable()) {
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("message", "خدمة النطق غير متوفرة حالياً")));
        }

        try {
            String language = "ar"; // Default to Arabic
            byte[] audio = ttsService.synthesize(question.getQuestionText(), language);
            if (audio == null) {
                return ResponseEntity.ok(ApiResponse.success(
                        Map.of("message", "فشل في إنشاء الصوت")));
            }

            String filename = "question_" + questionId + ".mp3";
            String audioUrl = fileStorageService.saveAudio(audio, filename);

            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("audioUrl", audioUrl)));

        } catch (Exception e) {
            log.error("Failed to generate audio for question {}: {}", questionId, e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error("حدث خطأ في إنشاء الصوت"));
        }
    }

    @GetMapping("/tts/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getTtsStatus() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("available", ttsService.isAvailable())));
    }

    private String detectLanguage(Lesson lesson) {
        String subjectName = lesson.getSubject().getName();
        if (subjectName.contains("English") || subjectName.contains("الإنجليزية")) {
            return "en";
        }
        return "ar";
    }
}
