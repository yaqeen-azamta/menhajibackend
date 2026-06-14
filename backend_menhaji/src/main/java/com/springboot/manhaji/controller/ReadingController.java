package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.ReadingAssessmentResponse;
import com.springboot.manhaji.dto.response.ReadingHistoryEntry;
import com.springboot.manhaji.service.reading.ReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reading")
@RequiredArgsConstructor
@Slf4j
public class ReadingController {

    private final ReadingService readingService;

    // ─────────────────────────────────────────────────────────────────────────────
    //  POST /api/reading/assess
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * A student submits a recording of themselves reading a lesson paragraph.
     *
     * <p>The backend loads {@code lesson.content} from the database (source of truth),
     * transcribes the audio via Gemini, compares word-by-word with Arabic normalization,
     * persists the attempt, and returns a structured accuracy report.
     *
     * <p>Form fields:
     * <ul>
     *   <li>{@code audio}    – multipart audio file (webm / m4a / wav / mp3 / ogg)</li>
     *   <li>{@code lessonId} – ID of the lesson whose content is the ground truth</li>
     *   <li>{@code language} – "ar" (default) or "en"</li>
     * </ul>
     *
     * <p>Error responses:
     * <ul>
     *   <li>400 – missing or empty audio file</li>
     *   <li>404 – lesson not found (propagated from {@link com.springboot.manhaji.exception.ResourceNotFoundException})</li>
     *   <li>500 – unexpected transcription or persistence failure</li>
     * </ul>
     */
    /**
     * Form fields:
     * <ul>
     *   <li>{@code audio}      – multipart audio file (webm / m4a / wav / mp3 / ogg)</li>
     *   <li>{@code lessonId}   – ID of the lesson (always required for history persistence)</li>
     *   <li>{@code questionId} – optional; when present, {@code question.correctAnswer} is used
     *                            as the source text instead of {@code lesson.content}.
     *                            Must be provided for READING question types in the quiz flow.</li>
     *   <li>{@code language}   – "ar" (default) or "en"</li>
     * </ul>
     */
    @PostMapping(value = "/assess", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReadingAssessmentResponse>> assess(
            @RequestParam MultipartFile audio,
            @RequestParam Long lessonId,
            @RequestParam(required = false) Long questionId,
            @RequestParam(defaultValue = "ar") String language,
            Authentication authentication) {

        System.out.println("[READING] READING ASSESS CALLED — lessonId=" + lessonId
                + " questionId=" + questionId + " language=" + language);

        Long studentId = (Long) authentication.getPrincipal();

        if (audio == null || audio.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("الملف الصوتي مطلوب"));
        }

        log.info("[READING] assess ENTRY: studentId={} lessonId={} questionId={} language={} contentType={} size={}B",
                studentId, lessonId, questionId, language,
                audio.getContentType(), audio.getSize());

        ReadingAssessmentResponse response =
                readingService.assess(studentId, lessonId, questionId, audio, language);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  GET /api/reading/history
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the authenticated student's reading attempt history, newest first.
     *
     * <p>Query params: {@code page} (default 0), {@code size} (default 10).
     * Response does not include word lists — only summary fields per attempt.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<ReadingHistoryEntry>>> getHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long studentId = (Long) authentication.getPrincipal();
        Page<ReadingHistoryEntry> history = readingService.getHistory(
                studentId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  GET /api/reading/history/lesson/{lessonId}
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * All attempts by the authenticated student on one specific lesson, newest first.
     * Useful for showing reading progress over multiple tries on the same lesson.
     */
    @GetMapping("/history/lesson/{lessonId}")
    public ResponseEntity<ApiResponse<List<ReadingHistoryEntry>>> getHistoryForLesson(
            @PathVariable Long lessonId,
            Authentication authentication) {

        Long studentId = (Long) authentication.getPrincipal();
        List<ReadingHistoryEntry> history =
                readingService.getHistoryForLesson(studentId, lessonId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
