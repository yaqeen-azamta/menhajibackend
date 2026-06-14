package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.request.ReadingSubmitRequest;
import com.springboot.manhaji.dto.request.SubmitAnswerRequest;
import com.springboot.manhaji.dto.request.TracingSubmitRequest;
import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.AttemptResponse;
import com.springboot.manhaji.dto.response.PronunciationScoreResponse;
import com.springboot.manhaji.dto.response.QuizResponse;
import com.springboot.manhaji.dto.response.SubmitAnswerResponse;
import com.springboot.manhaji.service.QuizService;
import com.springboot.manhaji.service.StudentService;
import com.springboot.manhaji.service.ai.WhisperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
@Slf4j
public class QuizController {

    private final QuizService    quizService;
    private final StudentService studentService;
    private final WhisperService whisperService;

    // Get quiz for a lesson (no auth needed — questions have no correct answers exposed)
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuizByLesson(
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(ApiResponse.success(quizService.getQuizByLesson(lessonId)));
    }

    /**
     * Start a new quiz attempt.
     * STUDENT: no extra param needed.
     * PARENT/ADMIN: pass ?studentId=<students.id> of the target student.
     */
    @PostMapping("/attempt/start/{quizId}")
    public ResponseEntity<ApiResponse<AttemptResponse>> startAttempt(
            @PathVariable Long quizId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        Long targetUserId = studentService.resolveStudent(authentication, studentId).getUser().getId();
        log.info("POST /quiz/attempt/start/{} — userId={}, requestedStudentId={}, targetUserId={}",
                quizId, userId, studentId, targetUserId);

        return ResponseEntity.ok(ApiResponse.success(quizService.startAttempt(quizId, targetUserId)));
    }

    /**
     * Submit a text answer for one question in an attempt.
     * PARENT/ADMIN: pass ?studentId=<students.id> of the target student.
     */
    @PostMapping("/attempt/{attemptId}/answer")
    public ResponseEntity<ApiResponse<SubmitAnswerResponse>> submitAnswer(
            @PathVariable Long attemptId,
            @Valid @RequestBody SubmitAnswerRequest request,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long targetUserId = studentService.resolveStudent(authentication, studentId).getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(quizService.submitAnswer(attemptId, request, targetUserId)));
    }

    /**
     * Submit a voice answer — transcribes audio then evaluates.
     * PARENT/ADMIN: pass ?studentId=<students.id> of the target student.
     */
    @PostMapping("/attempt/{attemptId}/voice-answer")
    public ResponseEntity<ApiResponse<SubmitAnswerResponse>> submitVoiceAnswer(
            @PathVariable Long attemptId,
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam("questionId") Long questionId,
            @RequestParam(value = "language", defaultValue = "ar") String language,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long targetUserId = studentService.resolveStudent(authentication, studentId).getUser().getId();

        try {
            String transcription = whisperService.transcribe(audioFile.getBytes(), language);
            log.info("Voice transcription for question {}: {}", questionId, transcription);

            SubmitAnswerRequest request = new SubmitAnswerRequest();
            request.setQuestionId(questionId);
            request.setAnswer(transcription);
            request.setSpokenText(transcription);

            return ResponseEntity.ok(ApiResponse.success(quizService.submitAnswer(attemptId, request, targetUserId)));
        } catch (Exception e) {
            log.error("Voice answer failed for attempt {}: {}", attemptId, e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error("حدث خطأ في معالجة الصوت"));
        }
    }

    /**
     * Submit a pronunciation attempt — transcribes audio then scores phonetic similarity.
     * PARENT/ADMIN: pass ?studentId=<students.id> of the target student.
     */
    @PostMapping("/attempt/{attemptId}/pronunciation")
    public ResponseEntity<ApiResponse<PronunciationScoreResponse>> submitPronunciation(
            @PathVariable Long attemptId,
            @RequestParam("audio") MultipartFile audioFile,
            @RequestParam("questionId") Long questionId,
            @RequestParam(value = "language", defaultValue = "ar") String language,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long targetUserId = studentService.resolveStudent(authentication, studentId).getUser().getId();

        try {
            PronunciationScoreResponse response = quizService.submitPronunciation(
                    attemptId, questionId, audioFile.getBytes(), language, targetUserId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Pronunciation scoring failed for attempt {}: {}", attemptId, e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error("حدث خطأ في تقييم النطق"));
        }
    }

    /**
     * Submit a client-scored tracing result.
     * PARENT/ADMIN: pass ?studentId=<students.id> of the target student.
     */
    @PostMapping("/attempt/{attemptId}/tracing")
    public ResponseEntity<ApiResponse<SubmitAnswerResponse>> submitTracing(
            @PathVariable Long attemptId,
            @Valid @RequestBody TracingSubmitRequest request,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long targetUserId = studentService.resolveStudent(authentication, studentId).getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(quizService.submitTracingResult(attemptId, request, targetUserId)));
    }

    // Hint does not require student identity
    @GetMapping("/question/{questionId}/hint")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHint(
            @PathVariable Long questionId,
            @RequestParam(defaultValue = "1") int level) {
        return ResponseEntity.ok(ApiResponse.success(quizService.getHint(questionId, level)));
    }

    /**
     * Submit a reading result forwarded from /api/reading/assess.
     * PARENT/ADMIN: pass ?studentId=<students.id> of the target student.
     */
    @PostMapping("/attempt/{attemptId}/reading")
    public ResponseEntity<ApiResponse<SubmitAnswerResponse>> submitReading(
            @PathVariable Long attemptId,
            @Valid @RequestBody ReadingSubmitRequest request,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long targetUserId = studentService.resolveStudent(authentication, studentId).getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(quizService.submitReadingResult(attemptId, request, targetUserId)));
    }

    /**
     * Complete the attempt and retrieve final results.
     * PARENT/ADMIN: pass ?studentId=<students.id> of the target student.
     */
    @PostMapping("/attempt/{attemptId}/complete")
    public ResponseEntity<ApiResponse<AttemptResponse>> completeAttempt(
            @PathVariable Long attemptId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long targetUserId = studentService.resolveStudent(authentication, studentId).getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(quizService.completeAttempt(attemptId, targetUserId)));
    }
}
