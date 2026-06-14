package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.ReadingAssessmentResponse;
import com.springboot.manhaji.dto.response.ReadingHistoryEntry;
import com.springboot.manhaji.service.StudentService;
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
    private final StudentService studentService;

    /**
     * A student (or parent on behalf of a child) submits a recording of themselves reading a
     * lesson paragraph. The backend loads {@code lesson.content} from the database (source of
     * truth), transcribes the audio via Gemini, compares word-by-word with Arabic normalization,
     * persists the attempt, and returns a structured accuracy report.
     *
     * Form fields:
     * <ul>
     *   <li>{@code audio}      – multipart audio file (webm / m4a / wav / mp3 / ogg)</li>
     *   <li>{@code lessonId}   – ID of the lesson (always required for history persistence)</li>
     *   <li>{@code questionId} – optional; when present {@code question.correctAnswer} is used
     *                            as the source text instead of {@code lesson.content}.</li>
     *   <li>{@code language}   – "ar" (default) or "en"</li>
     *   <li>{@code studentId}  – PARENT/ADMIN accounts: the child's {@code students.id}</li>
     * </ul>
     */
    @PostMapping(value = "/assess", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReadingAssessmentResponse>> assess(
            @RequestParam MultipartFile audio,
            @RequestParam Long lessonId,
            @RequestParam(required = false) Long questionId,
            @RequestParam(defaultValue = "ar") String language,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        if (audio == null || audio.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("الملف الصوتي مطلوب"));
        }

        Long userId = (Long) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream()
                .findFirst().map(a -> a.getAuthority()).orElse("UNKNOWN");

        log.info("[READING] assess incoming — role={} userId/parentId={} requestedStudentId={} lessonId={} questionId={} language={} size={}B",
                role, userId, studentId, lessonId, questionId, language, audio.getSize());

        Long targetUserId = studentService.resolveStudent(authentication, studentId).getUser().getId();

        log.info("[READING] assess resolved — role={} userId={} requestedStudentId={} resolvedStudentUserId={}",
                role, userId, studentId, targetUserId);

        ReadingAssessmentResponse response =
                readingService.assess(targetUserId, lessonId, questionId, audio, language);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Returns reading attempt history, newest first.
     *
     * STUDENT accounts: no extra param needed.
     * PARENT/ADMIN accounts: pass ?studentId=<students.id> of the target student.
     *
     * Query params: {@code page} (default 0), {@code size} (default 10).
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<ReadingHistoryEntry>>> getHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long targetUserId = studentService.resolveStudent(authentication, studentId).getUser().getId();

        Page<ReadingHistoryEntry> history = readingService.getHistory(
                targetUserId, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * All reading attempts for one specific lesson, newest first.
     *
     * STUDENT accounts: no extra param needed.
     * PARENT/ADMIN accounts: pass ?studentId=<students.id> of the target student.
     */
    @GetMapping("/history/lesson/{lessonId}")
    public ResponseEntity<ApiResponse<List<ReadingHistoryEntry>>> getHistoryForLesson(
            @PathVariable Long lessonId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long targetUserId = studentService.resolveStudent(authentication, studentId).getUser().getId();

        return ResponseEntity.ok(ApiResponse.success(
                readingService.getHistoryForLesson(targetUserId, lessonId)));
    }
}
