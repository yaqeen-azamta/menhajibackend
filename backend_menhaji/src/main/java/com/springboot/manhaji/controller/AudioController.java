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

        String textToSpeak = lesson.getTitle() + ". " + lesson.getContent();
        if (textToSpeak.length() > 5000) {
            textToSpeak = textToSpeak.substring(0, 5000);
        }

        // Content-fingerprinted cache: serve cached narration only while it
        // still matches the current lesson text; regenerate when content changes.
        String fingerprint = ttsService.speechFingerprint(textToSpeak);
        if (isCacheFresh(lesson.getAudioUrl(), lesson.getAudioTextHash(), fingerprint)) {
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("audioUrl", lesson.getAudioUrl())));
        }

        if (!ttsService.isAvailable()) {
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("message", "خدمة النطق غير متوفرة حالياً")));
        }

        try {
            String language = detectLanguage(lesson);
            byte[] audio = ttsService.synthesize(textToSpeak, language);
            if (audio == null) {
                return ResponseEntity.ok(ApiResponse.success(
                        Map.of("message", "فشل في إنشاء الصوت")));
            }

            String filename = "lesson_" + lessonId + ".mp3";
            String audioUrl = fileStorageService.saveAudio(audio, filename);

            lesson.setAudioUrl(audioUrl);
            lesson.setAudioTextHash(fingerprint);
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

        // Content-fingerprinted cache: serve cached clip only while it still
        // matches the current question text. When the text is edited the stored
        // hash diverges and we regenerate instead of serving stale audio.
        String fingerprint = ttsService.speechFingerprint(question.getQuestionText());
        if (isCacheFresh(question.getAudioUrl(), question.getAudioTextHash(), fingerprint)) {
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("audioUrl", question.getAudioUrl())));
        }

        if (!ttsService.isAvailable()) {
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("message", "خدمة النطق غير متوفرة حالياً")));
        }

        try {
            // Detect language from the question text itself — any Arabic Unicode
            // character => Arabic voice. Previously hardcoded to "ar", so English
            // questions came out with Arabic phonemes.
            String language = containsArabic(question.getQuestionText()) ? "ar" : "en";
            byte[] audio = ttsService.synthesize(question.getQuestionText(), language);
            if (audio == null) {
                return ResponseEntity.ok(ApiResponse.success(
                        Map.of("message", "فشل في إنشاء الصوت")));
            }

            String filename = "question_" + questionId + ".mp3";
            String audioUrl = fileStorageService.saveAudio(audio, filename);

            question.setAudioUrl(audioUrl);
            question.setAudioTextHash(fingerprint);
            questionRepository.save(question);

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

    /**
     * Whether a cached audio clip can be served as-is, or must be regenerated.
     *
     * <ul>
     *   <li><b>No clip</b> (audioUrl null/blank) → not fresh; generate.</li>
     *   <li><b>Authored asset</b> (URL not under uploads/audio/) → always
     *       fresh. Bundled reciter/native-speaker files must never be
     *       overwritten by synthesis.</li>
     *   <li><b>TTS-generated clip</b> (URL under uploads/audio/) → fresh only
     *       while the stored fingerprint matches the current text fingerprint.</li>
     * </ul>
     */
    private static boolean isCacheFresh(String audioUrl, String storedHash, String currentFingerprint) {
        if (audioUrl == null || audioUrl.isBlank()) {
            return false;
        }
        // Support both "/uploads/audio/" (our FileStorageService) and
        // "uploads/audio/" (no leading slash) for forward-compatibility.
        if (!audioUrl.contains("uploads/audio/")) {
            return true; // Authored asset — serve untouched.
        }
        return storedHash != null && storedHash.equals(currentFingerprint);
    }

    private String detectLanguage(Lesson lesson) {
        String subjectName = lesson.getSubject().getName();
        if (subjectName.contains("English") || subjectName.contains("الإنجليزية")) {
            return "en";
        }
        return "ar";
    }

    /** Any character in the Arabic unicode block ⇒ Arabic. */
    private static boolean containsArabic(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0600 && c <= 0x06FF) return true;
        }
        return false;
    }
}
