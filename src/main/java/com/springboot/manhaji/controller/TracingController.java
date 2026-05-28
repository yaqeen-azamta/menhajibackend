package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.request.TracingSubmitRequest;
import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.TracingHistoryEntry;
import com.springboot.manhaji.dto.response.TracingProgressResponse;
import com.springboot.manhaji.dto.response.TracingQuestionResponse;
import com.springboot.manhaji.dto.response.TracingResultResponse;
import com.springboot.manhaji.service.TracingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tracing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tracing", description = "Tracing / handwriting learning APIs for the Flutter app")
public class TracingController {

    private final TracingService tracingService;

    // ═══════════════════════════════════════════════════════════════════════════
    //  QUESTION READ ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/tracing/questions/{questionId}
     *
     * Returns the full tracing question details including SVG path and expected
     * key-points so Flutter can render the guide overlay and run the client-side
     * accuracy heuristic.
     *
     * Example response:
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "questionId": 1,
     *     "character": "1",
     *     "displayName": "Number One",
     *     "language": "NUMBERS",
     *     "svgPath": "M 50 10 L 50 90 M 38 90 L 62 90",
     *     "expectedAccuracy": 70.0,
     *     "tolerancePercentage": 20.0,
     *     "effectiveRequiredAccuracy": 56.0,
     *     "strokeCount": 2
     *   }
     * }
     * </pre>
     */
    @GetMapping("/questions/{questionId}")
    @Operation(summary = "Get a single tracing question by its question ID")
    public ResponseEntity<ApiResponse<TracingQuestionResponse>> getTracingQuestion(
            @Parameter(description = "Question ID") @PathVariable Long questionId) {

        TracingQuestionResponse response = tracingService.getTracingQuestion(questionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/tracing/questions/lesson/{lessonId}
     *
     * Returns all tracing questions for a lesson, ordered by question ID.
     * Flutter calls this when entering a tracing lesson to load all characters.
     *
     * Example response:
     * <pre>
     * {
     *   "success": true,
     *   "data": [
     *     { "questionId": 1, "character": "1", "displayName": "Number One", ... },
     *     { "questionId": 2, "character": "2", "displayName": "Number Two", ... }
     *   ]
     * }
     * </pre>
     */
    @GetMapping("/questions/lesson/{lessonId}")
    @Operation(summary = "Get all tracing questions for a lesson")
    public ResponseEntity<ApiResponse<List<TracingQuestionResponse>>> getTracingQuestionsByLesson(
            @Parameter(description = "Lesson ID") @PathVariable Long lessonId) {

        List<TracingQuestionResponse> questions =
                tracingService.getTracingQuestionsByLesson(lessonId);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SUBMISSION ENDPOINT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * POST /api/tracing/submit
     *
     * Submits a student's tracing drawing. The backend re-validates the accuracy
     * against the stored expected path using a Hausdorff nearest-neighbor algorithm,
     * then awards score and stars.
     *
     * Example request body:
     * <pre>
     * {
     *   "questionId": 1,
     *   "isCorrect": true,
     *   "clientAccuracy": 82.5,
     *   "score": 5,
     *   "stars": 2,
     *   "drawingPoints": [
     *     { "x": 50.0, "y": 10.0, "t": 0   },
     *     { "x": 50.1, "y": 25.3, "t": 120 },
     *     { "x": 50.0, "y": 40.0, "t": 240 },
     *     { "x": 50.0, "y": 90.0, "t": 600 }
     *   ]
     * }
     * </pre>
     *
     * Example response:
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "success": true,
     *     "questionId": 1,
     *     "character": "1",
     *     "serverAccuracy": 94.2,
     *     "clientAccuracy": 82.5,
     *     "finalAccuracy": 90.4,
     *     "score": 86,
     *     "stars": 3,
     *     "isCorrect": true,
     *     "attemptNumber": 1,
     *     "feedback": "رائع جداً! كتبت 1 بشكل مثالي! ⭐⭐⭐",
     *     "requiredAccuracy": 56.0
     *   }
     * }
     * </pre>
     */
    @PostMapping("/submit")
    @Operation(summary = "Submit a tracing drawing for backend accuracy verification and scoring")
    public ResponseEntity<ApiResponse<TracingResultResponse>> submitTracingAnswer(
            @Valid @RequestBody TracingSubmitRequest request,
            Authentication authentication) {

        Long studentId = (Long) authentication.getPrincipal();
        log.debug("Tracing submit: studentId={} questionId={} clientAccuracy={}",
                studentId, request.getQuestionId(), request.getClientAccuracy());

        TracingResultResponse result = tracingService.submitTracingAnswer(studentId, request);
        return ResponseEntity.ok(ApiResponse.success("Tracing answer submitted", result));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PROGRESS & HISTORY ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * GET /api/tracing/progress
     *
     * Returns the authenticated student's aggregate tracing progress:
     * total attempts, correct answers, overall accuracy, stars, and a
     * per-character breakdown.
     *
     * Example response:
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "studentId": 7,
     *     "totalAttempts": 15,
     *     "totalCorrect": 12,
     *     "overallAccuracy": 78.4,
     *     "totalStars": 29,
     *     "maxPossibleStars": 45,
     *     "masteryRate": 66.7,
     *     "characterProgress": [
     *       { "questionId": 1, "character": "1", "attempts": 3,
     *         "bestStars": 3, "bestAccuracy": 94.2, "mastered": true },
     *       { "questionId": 2, "character": "2", "attempts": 2,
     *         "bestStars": 1, "bestAccuracy": 62.0, "mastered": false }
     *     ]
     *   }
     * }
     * </pre>
     */
    @GetMapping("/progress")
    @Operation(summary = "Get the authenticated student's overall tracing progress")
    public ResponseEntity<ApiResponse<TracingProgressResponse>> getMyProgress(
            Authentication authentication) {

        Long studentId = (Long) authentication.getPrincipal();
        TracingProgressResponse progress = tracingService.getStudentProgress(studentId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    /**
     * GET /api/tracing/progress/{studentId}
     *
     * Same as GET /api/tracing/progress but for a specific student ID.
     * Accessible by teachers, parents, and admins.
     */
    @GetMapping("/progress/{studentId}")
    @Operation(summary = "Get a specific student's overall tracing progress (teacher/admin view)")
    public ResponseEntity<ApiResponse<TracingProgressResponse>> getProgressByStudentId(
            @Parameter(description = "Student ID") @PathVariable Long studentId) {

        TracingProgressResponse progress = tracingService.getStudentProgress(studentId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    /**
     * GET /api/tracing/history?page=0&size=20
     *
     * Paginated answer history for the authenticated student, newest first.
     *
     * Example response:
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "content": [
     *       { "answerId": 42, "questionId": 2, "character": "2",
     *         "finalAccuracy": 71.3, "score": 79, "stars": 2,
     *         "isCorrect": true, "attemptNumber": 1,
     *         "submittedAt": "2026-05-20T10:32:00" }
     *     ],
     *     "totalElements": 15,
     *     "totalPages": 1
     *   }
     * }
     * </pre>
     */
    @GetMapping("/history")
    @Operation(summary = "Get the authenticated student's paginated tracing history")
    public ResponseEntity<ApiResponse<Page<TracingHistoryEntry>>> getMyHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Long studentId = (Long) authentication.getPrincipal();
        Page<TracingHistoryEntry> history = tracingService.getStudentHistory(
                studentId, PageRequest.of(page, size, Sort.by("submittedAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * GET /api/tracing/history/{studentId}?page=0&size=20
     *
     * Paginated history for a specific student (teacher/parent/admin view).
     */
    @GetMapping("/history/{studentId}")
    @Operation(summary = "Get paginated tracing history for a specific student (teacher/admin view)")
    public ResponseEntity<ApiResponse<Page<TracingHistoryEntry>>> getHistoryByStudentId(
            @Parameter(description = "Student ID") @PathVariable Long studentId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<TracingHistoryEntry> history = tracingService.getStudentHistory(
                studentId, PageRequest.of(page, size, Sort.by("submittedAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * GET /api/tracing/history/question/{questionId}
     *
     * All attempts by the authenticated student on one specific question.
     */
    @GetMapping("/history/question/{questionId}")
    @Operation(summary = "Get the authenticated student's attempt history for one tracing question")
    public ResponseEntity<ApiResponse<List<TracingHistoryEntry>>> getHistoryForQuestion(
            @Parameter(description = "Question ID") @PathVariable Long questionId,
            Authentication authentication) {

        Long studentId = (Long) authentication.getPrincipal();
        List<TracingHistoryEntry> history =
                tracingService.getStudentHistoryForQuestion(studentId, questionId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
