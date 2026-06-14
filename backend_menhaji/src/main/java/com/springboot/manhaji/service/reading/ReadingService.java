package com.springboot.manhaji.service.reading;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.dto.response.ReadingAssessmentResponse;
import com.springboot.manhaji.dto.response.ReadingHistoryEntry;
import com.springboot.manhaji.entity.Lesson;
import com.springboot.manhaji.entity.Question;
import com.springboot.manhaji.entity.ReadingAssessmentResult;
import com.springboot.manhaji.entity.enums.QuestionType;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.exception.SpeechServiceUnavailableException;
import com.springboot.manhaji.repository.LessonRepository;
import com.springboot.manhaji.repository.QuestionRepository;
import com.springboot.manhaji.repository.ReadingAssessmentResultRepository;
import com.springboot.manhaji.service.ai.GoogleSpeechService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingService {

    private final LessonRepository                  lessonRepository;
    private final QuestionRepository                questionRepository;
    private final GoogleSpeechService               googleSpeechService;
    private final TextComparisonService             textComparisonService;
    private final ReadingAssessmentResultRepository resultRepository;
    private final ObjectMapper                      objectMapper;

    /**
     * Injected only when a Phase 2 bean implementing {@link PronunciationAssessmentService}
     * is present in the Spring context. {@code null} throughout Phase 1.
     */
    @Autowired(required = false)
    private PronunciationAssessmentService pronunciationService;

    private static final String ENGINE_GEMINI      = "gemini";
    private static final String ENGINE_UNAVAILABLE = "unavailable";

    // ─────────────────────────────────────────────────────────────────────────────
    //  Core assessment pipeline
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Full reading assessment pipeline:
     * <ol>
     *   <li>Load {@code lesson.content} from DB — source of truth.</li>
     *   <li>Transcribe the student's audio via Gemini.</li>
     *   <li>Compare word-by-word with Arabic/English normalization.</li>
     *   <li>Persist the attempt with word lists, counts, and metadata.</li>
     *   <li>Optionally invoke pronunciation scoring (Phase 2 hook).</li>
     *   <li>Return the structured assessment response.</li>
     * </ol>
     *
     * @throws ResourceNotFoundException if the lesson does not exist → 404 via GlobalExceptionHandler
     */
    @Transactional
    public ReadingAssessmentResponse assess(Long studentId, Long lessonId,
                                            Long questionId,
                                            MultipartFile audioFile, String language) {

        // ── Step 1: load lesson + resolve source text ───────────────────────────
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));

        // For READING questions the text to be read is in question.correctAnswer.
        // lessonContent is only the fallback for the legacy standalone reading screen.
        String originalText = resolveSourceText(questionId, lesson);

        log.info("assess: studentId={} lessonId={} questionId={} language={} audioSize={}B sourceLen={}",
                studentId, lessonId, questionId, language, audioFile.getSize(), originalText.length());

        // ── Step 2: transcribe audio ────────────────────────────────────────────
        String recognizedText   = "";
        String transcriptionEngine = ENGINE_UNAVAILABLE;

        boolean geminiAvailable = googleSpeechService.isAvailable();
        System.out.println("[READING] GoogleSpeechService.isAvailable()=" + geminiAvailable);
        log.info("[READING] assess: googleSpeechService.isAvailable()={} studentId={} lessonId={} questionId={}",
                geminiAvailable, studentId, lessonId, questionId);
if (geminiAvailable) {
    try {
        System.out.println("=================================");
        System.out.println("CALLING GEMINI...");
        System.out.println("ORIGINAL TEXT = " + originalText);
        System.out.println("LANGUAGE = " + language);
        System.out.println("AUDIO SIZE = " + audioFile.getSize());
        System.out.println("CONTENT TYPE = " + audioFile.getContentType());
        System.out.println("=================================");

        recognizedText = googleSpeechService.transcribeForReading(
                audioFile,
                originalText,
                language
        );

        System.out.println("=================================");
        System.out.println("GEMINI RETURNED:");
        System.out.println("RECOGNIZED = " + recognizedText);
        System.out.println("=================================");

        transcriptionEngine = ENGINE_GEMINI;

        log.info(
                "[READING] assess: transcription done — studentId={} lessonId={} questionId={} engine={} recognized=\"{}\"",
                studentId,
                lessonId,
                questionId,
                transcriptionEngine,
                recognizedText
        );

    } catch (SpeechServiceUnavailableException e) {
        // All retries exhausted — this is a system failure, NOT a wrong answer.
        // Do NOT save a 0% result. Roll back the transaction and surface the error.
        log.error("[READING] assess: speech service unavailable after all retries — studentId={} lessonId={} error={}",
                studentId, lessonId, e.getMessage(), e);
        System.out.println("[READING] SPEECH SERVICE UNAVAILABLE — not saving assessment result, re-throwing");
        throw e;

    } catch (Exception e) {

        System.out.println("=================================");
        System.out.println("GEMINI FAILED");
        System.out.println("ERROR TYPE = " + e.getClass().getSimpleName());
        System.out.println("ERROR = " + e.getMessage());
        System.out.println("=================================");

        log.error(
                "[READING] assess: transcription EXCEPTION — studentId={} lessonId={} questionId={} errorType={} error={}",
                studentId,
                lessonId,
                questionId,
                e.getClass().getSimpleName(),
                e.getMessage(),
                e
        );

        e.printStackTrace();

        recognizedText = "";
    }

    System.out.println("=================================");
    System.out.println("FINAL RESULT");
    System.out.println("EXPECTED = " + originalText);
    System.out.println("RECOGNIZED = " + recognizedText);
    System.out.println("=================================");

} else {

    log.error(
        "[READING] assess: GoogleSpeechService NOT AVAILABLE — GEMINI_API_KEY env-var is not set or equals 'not-set'. " +
        "Set the GEMINI_API_KEY environment variable and restart the server. recognizedText will be empty."
    );

    System.out.println("[READING] GEMINI_API_KEY is NOT SET — transcription skipped");
}
        // ── Step 3: word-level comparison ───────────────────────────────────────
        TextComparisonService.ComparisonResult comparison =
                textComparisonService.compare(originalText, recognizedText);

        log.info("assess: comparison done — studentId={} lessonId={} accuracy={} " +
                 "totalWords={} correct={} missing={} incorrect={}",
                studentId, lessonId,
                comparison.accuracy(),
                comparison.totalWords(),
                comparison.correctWords().size(),
                comparison.missingWords().size(),
                comparison.incorrectWords().size());

        // ── Step 4: persist attempt ─────────────────────────────────────────────
        ReadingAssessmentResult saved = persistAttempt(
                studentId, lesson, originalText, recognizedText,
                comparison, transcriptionEngine, language);

        log.debug("assess: attempt persisted — resultId={} studentId={} lessonId={} accuracy={}",
                saved.getId(), studentId, lessonId, comparison.accuracy());

        // ── Step 5: pronunciation scoring (Phase 2 hook) ────────────────────────
        if (pronunciationService != null && pronunciationService.isAvailable()) {
            try {
                PronunciationAssessmentService.PronunciationDetail detail =
                        pronunciationService.assess(
                                audioFile.getBytes(), originalText, recognizedText, language);

                log.debug("assess: pronunciation detail — studentId={} lessonId={} overallScore={}",
                        studentId, lessonId, detail.overallScore());

                // TODO: persist detail.overallScore() → saved.setPronunciationScore(...)
                // TODO: persist detail JSON  → saved.setPronunciationDetailJson(toJson(detail))
                // TODO: call resultRepository.save(saved) after setting both fields
            } catch (Exception e) {
                log.warn("assess: pronunciation scoring skipped — studentId={} error={}",
                        studentId, e.getMessage());
            }
        }

        // ── Step 6: build response ──────────────────────────────────────────────
        return ReadingAssessmentResponse.builder()
                .originalText(originalText)
                .recognizedText(recognizedText)
                .accuracy(comparison.accuracy())
                .correctWords(comparison.correctWords())
                .incorrectWords(comparison.incorrectWords())
                .missingWords(comparison.missingWords())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  History queries
    // ─────────────────────────────────────────────────────────────────────────────

    /** Paginated attempt history for the authenticated student, newest first. */
    @Transactional(readOnly = true)
    public Page<ReadingHistoryEntry> getHistory(Long studentId, Pageable pageable) {
        return resultRepository
                .findByStudentIdOrderByCreatedAtDesc(studentId, pageable)
                .map(this::toHistoryEntry);
    }

    /** All attempts by the authenticated student on one lesson, newest first. */
    @Transactional(readOnly = true)
    public List<ReadingHistoryEntry> getHistoryForLesson(Long studentId, Long lessonId) {
        return resultRepository
                .findByStudentIdAndLessonIdOrderByCreatedAtDesc(studentId, lessonId)
                .stream()
                .map(this::toHistoryEntry)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────────────

    private ReadingAssessmentResult persistAttempt(Long studentId, Lesson lesson,
                                                    String originalText, String recognizedText,
                                                    TextComparisonService.ComparisonResult comparison,
                                                    String transcriptionEngine, String language) {
        ReadingAssessmentResult result = new ReadingAssessmentResult();
        result.setStudentId(studentId);
        result.setLesson(lesson);

        // Text content
        result.setOriginalText(originalText);
        result.setRecognizedText(recognizedText.isBlank() ? null : recognizedText);

        // Scalar scores
        result.setAccuracy(comparison.accuracy());
        result.setTotalWords(comparison.totalWords());
        result.setCorrectWordCount(comparison.correctWords().size());
        result.setIncorrectWordCount(comparison.incorrectWords().size());
        result.setMissingWordCount(comparison.missingWords().size());

        // Word lists as JSON
        result.setCorrectWordsJson(toJson(comparison.correctWords()));
        result.setIncorrectWordsJson(toJson(comparison.incorrectWords()));
        result.setMissingWordsJson(toJson(comparison.missingWords()));

        // Attempt metadata
        result.setTranscriptionEngine(transcriptionEngine);
        result.setLanguage(language);

        // Phase 2 pronunciation fields — NULL until a PronunciationAssessmentService bean is wired
        // TODO: result.setPronunciationScore(...)        set from PronunciationAssessmentService.assess()
        // TODO: result.setPronunciationDetailJson(...)   set from PronunciationAssessmentService.assess()

        return resultRepository.save(result);
    }

    private ReadingHistoryEntry toHistoryEntry(ReadingAssessmentResult result) {
        return ReadingHistoryEntry.builder()
                .id(result.getId())
                .lessonId(result.getLesson().getId())
                .lessonTitle(result.getLesson().getTitle())
                .originalText(result.getOriginalText())
                .recognizedText(result.getRecognizedText())
                .accuracy(result.getAccuracy())
                .totalWords(result.getTotalWords())
                .correctWordCount(result.getCorrectWordCount())
                .incorrectWordCount(result.getIncorrectWordCount())
                .missingWordCount(result.getMissingWordCount())
                .transcriptionEngine(result.getTranscriptionEngine())
                .language(result.getLanguage())
                .createdAt(result.getCreatedAt())
                .build();
    }

    /**
     * Resolves the source text the student is expected to read.
     *
     * <p>Priority:
     * <ol>
     *   <li>[A] {@code questionId} non-null → load Question → use {@code correctAnswer}
     *       (falls back to {@code questionText} if correctAnswer is blank).</li>
     *   <li>[B] {@code questionId} null/0 → auto-lookup first READING question for
     *       the lesson — handles stale Flutter clients that don't send questionId yet.</li>
     *   <li>[C] No READING question for lesson → use {@code lesson.content}.</li>
     * </ol>
     */
    private String resolveSourceText(Long questionId, Lesson lesson) {
        // ── Path A: questionId supplied by Flutter ───────────────────────────
        if (questionId != null && questionId != 0) {
            Question question = questionRepository.findById(questionId).orElse(null);
            if (question != null) {
                String text = question.getCorrectAnswer();
                if (text != null && !text.isBlank()) {
                    log.info("resolveSourceText: [A] questionId={} → correctAnswer=\"{}\"", questionId, text);
                    return text;
                }
                text = question.getQuestionText();
                if (text != null && !text.isBlank()) {
                    log.warn("resolveSourceText: [A] questionId={} correctAnswer blank → questionText=\"{}\"", questionId, text);
                    return text;
                }
                log.warn("resolveSourceText: [A] questionId={} both fields blank", questionId);
            } else {
                log.warn("resolveSourceText: [A] questionId={} not found in DB", questionId);
            }
        } else {
            log.warn("resolveSourceText: [B] questionId is null/0 — Flutter app may not have been rebuilt. " +
                     "Falling back to auto-lookup of READING questions for lessonId={}", lesson.getId());
        }

        // ── Path B: no questionId — find the first READING question for the lesson ──
        // This handles the case where the Flutter client was not rebuilt after the
        // questionId parameter was added to ReadingApiService.assessReading().
        List<Question> readingQuestions = questionRepository
                .findByLessonIdAndType(lesson.getId(), QuestionType.READING);
        if (!readingQuestions.isEmpty()) {
            Question rq = readingQuestions.get(0);
            String text = rq.getCorrectAnswer();
            if (text != null && !text.isBlank()) {
                log.info("resolveSourceText: [B] auto-found READING questionId={} → correctAnswer=\"{}\"", rq.getId(), text);
                return text;
            }
            text = rq.getQuestionText();
            if (text != null && !text.isBlank()) {
                log.warn("resolveSourceText: [B] auto-found READING questionId={} correctAnswer blank → questionText=\"{}\"", rq.getId(), text);
                return text;
            }
        }

        // ── Path C: no READING question for this lesson — final fallback ────
        String content = lesson.getContent();
        if (content == null || content.isBlank()) {
            log.warn("resolveSourceText: [C] lessonId={} has no content — originalText will be empty", lesson.getId());
            return "";
        }
        log.warn("resolveSourceText: [C] lessonId={} no READING question found — using lesson.content", lesson.getId());
        return content;
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("toJson: failed to serialize word list — returning empty array: {}", e.getMessage());
            return "[]";
        }
    }
}
