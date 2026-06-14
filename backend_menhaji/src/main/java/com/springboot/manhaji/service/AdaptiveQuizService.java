package com.springboot.manhaji.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.config.QuizConfigProperties;
import com.springboot.manhaji.dto.adaptive.*;
import com.springboot.manhaji.dto.question.QuestionOption;
import com.springboot.manhaji.entity.*;
import com.springboot.manhaji.entity.enums.AttemptStatus;
import com.springboot.manhaji.entity.enums.QuestionType;
import com.springboot.manhaji.exception.BadRequestException;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.exception.TooManyRequestsException;
import com.springboot.manhaji.exception.UnauthorizedException;
import com.springboot.manhaji.repository.AdaptiveQuizAttemptRepository;
import com.springboot.manhaji.repository.AdaptiveResponseRepository;
import com.springboot.manhaji.repository.LessonRepository;
import com.springboot.manhaji.repository.QuestionRepository;
import com.springboot.manhaji.repository.StudentRepository;
import com.springboot.manhaji.repository.StudentSkillProfileRepository;
import com.springboot.manhaji.service.ai.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdaptiveQuizService {

    // Types that require special client-side rendering and are excluded from DB fallback
    private static final List<QuestionType> FALLBACK_EXCLUDED_TYPES = List.of(
            QuestionType.TRACING, QuestionType.PRONUNCIATION, QuestionType.READING,
            QuestionType.IMAGE_MCQ, QuestionType.ORDERING);

    private final AdaptiveQuizAttemptRepository attemptRepository;
    private final AdaptiveResponseRepository    responseRepository;
    private final StudentSkillProfileRepository skillProfileRepository;
    private final StudentRepository             studentRepository;
    private final LessonRepository              lessonRepository;
    private final QuestionRepository            questionRepository;
    private final StudentService                studentService;
    private final ObjectMapper                  objectMapper;
    private final GeminiService                 geminiService;
    private final QuizConfigProperties          quizConfig;

    // -----------------------------------------------------------------------
    // Generate quiz
    // -----------------------------------------------------------------------

    public AdaptiveQuizPayload generateAdaptiveQuiz(Long lessonId, Authentication authentication, Long childStudentId) {
        Student student = studentService.resolveStudent(authentication, childStudentId);
        Lesson  lesson  = resolveLesson(lessonId);

        // Resume an in-progress attempt if one already exists (read-only DB touch, no transaction)
        Optional<AdaptiveQuizAttempt> existing =
                attemptRepository.findByStudentIdAndLessonIdAndStatus(
                        student.getId(), lessonId, AttemptStatus.IN_PROGRESS);
        if (existing.isPresent()) {
            return buildPayload(existing.get());
        }

        // Build skill context from persisted profiles
        List<StudentSkillProfile> profiles =
                skillProfileRepository.findByStudentIdAndLessonId(student.getId(), lessonId);

        double threshold = quizConfig.getAdaptiveWeakSkillThreshold();
        List<SkillSummary> weakSkills   = toSummaries(profiles, p -> p.getAccuracy() < threshold);
        List<SkillSummary> strongSkills = toSummaries(profiles, p -> p.getAccuracy() >= threshold);

        int targetDifficulty = profiles.isEmpty() ? 1
                : (int) Math.round(profiles.stream()
                        .mapToInt(StudentSkillProfile::getCurrentDifficulty)
                        .average().orElse(1.0));

        int questionCount = quizConfig.getAdaptiveQuestionCount();

        // ── Load existing questions for duplicate prevention ───────────────────
        List<String> lessonQuestions = questionRepository.findByLessonId(lesson.getId()).stream()
                .map(Question::getQuestionText)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<String> previousAttemptQuestions = attemptRepository
                .findByStudentIdAndLessonId(student.getId(), lessonId).stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED)
                .flatMap(a -> parseGeneratedQuestions(a.getGeneratedQuestionsJson()).stream())
                .map(GeneratedQuestion::getQuestionText)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("[ADAPTIVE] Generating quiz: student={} lesson={} | lessonDBQuestions={} | prevAttemptQuestions={} | targetDifficulty={} | target={}",
                student.getId(), lessonId,
                lessonQuestions.size(), previousAttemptQuestions.size(),
                targetDifficulty, questionCount);

        AdaptiveQuizContext context = AdaptiveQuizContext.builder()
                .lessonTitle(lesson.getTitle())
                .subjectName(lesson.getSubject() != null ? lesson.getSubject().getName() : "")
                .lessonContent(lesson.getContent())
                .lessonObjectives(lesson.getObjectives())
                .targetDifficulty(targetDifficulty)
                .questionCount(questionCount)
                .weakSkills(weakSkills)
                .strongSkills(strongSkills)
                .existingQuestions(lessonQuestions)
                .previousAttemptQuestions(previousAttemptQuestions)
                .build();

        // Gemini call (with internal retry + duplicate filtering) outside any transaction.
        List<GeneratedQuestion> questions = geminiService.generateAdaptiveQuestions(context);

        log.info("[ADAPTIVE] Gemini returned {} unique questions for lesson {} (requested {})",
                questions.size(), lessonId, questionCount);

        String source;
        if (!questions.isEmpty()) {
            source = "GEMINI";
        } else {
            log.warn("Gemini retry also failed for lesson {}. Attempting DB fallback.", lessonId);
            if (!quizConfig.isAdaptiveFallbackEnabled()) {
                throw new BadRequestException("لم يتمكن النظام من توليد أسئلة. تأكد من إعداد Gemini API.");
            }
            questions = buildFallbackFromDatabase(lesson, targetDifficulty, weakSkills, questionCount);
            if (questions.isEmpty()) {
                log.error("No quizzable database questions found for lesson {} — fallback exhausted.", lessonId);
                throw new BadRequestException("لا تتوفر أسئلة لهذا الدرس حالياً. يرجى التواصل مع المعلم.");
            }
            source = "FALLBACK_DB";
            log.info("Using {} database questions (fallback) for lesson {}.", questions.size(), lessonId);
        }

        // Persist attempt in a short, focused transaction AFTER questions are resolved.
        return saveNewAttempt(student, lesson, targetDifficulty, weakSkills, questions, source);
    }

    /**
     * Short-lived transaction that only runs the INSERT.
     * Called after Gemini has already returned so no DB connection is held during the HTTP call.
     * SERIALIZABLE isolation makes the "check for existing IN_PROGRESS, then insert" atomic,
     * preventing race conditions from double-tap or network retries.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    protected AdaptiveQuizPayload saveNewAttempt(Student student, Lesson lesson,
                                                  int targetDifficulty,
                                                  List<SkillSummary> weakSkills,
                                                  List<GeneratedQuestion> questions,
                                                  String source) {
        // Guard against race conditions (double-tap / network retry).
        Optional<AdaptiveQuizAttempt> race =
                attemptRepository.findByStudentIdAndLessonIdAndStatus(
                        student.getId(), lesson.getId(), AttemptStatus.IN_PROGRESS);
        if (race.isPresent()) {
            return buildPayload(race.get());
        }

        AdaptiveQuizAttempt attempt = new AdaptiveQuizAttempt();
        attempt.setStudent(student);
        attempt.setLesson(lesson);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setDifficultyLevel(targetDifficulty);
        attempt.setQuizSource(source);
        attempt.setQuestionCount(questions.size());
        attempt.setFocusSkillsJson(toJsonSilently(
                weakSkills.stream().map(SkillSummary::getSubSkill).collect(Collectors.toList())));
        attempt.setGeneratedQuestionsJson(toJsonSilently(questions));

        attemptRepository.save(attempt);
        return buildPayload(attempt, questions);
    }

    // -----------------------------------------------------------------------
    // Submit answers
    // -----------------------------------------------------------------------

    @Transactional
    public AdaptiveQuizResult submitAdaptiveQuiz(Long attemptId,
                                                  AdaptiveSubmitRequest request,
                                                  Authentication authentication) {
        AdaptiveQuizAttempt attempt = resolveAttempt(attemptId);
        Student student = verifyAndGetStudent(authentication, attempt);

        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            throw new BadRequestException("تم تسليم هذا الاختبار مسبقاً");
        }

        List<GeneratedQuestion> generatedQuestions = parseGeneratedQuestions(attempt.getGeneratedQuestionsJson());
        if (generatedQuestions.isEmpty()) {
            throw new BadRequestException("بيانات الاختبار تالفة. يرجى بدء اختبار جديد.");
        }

        // Require the student to have answered at least half the questions.
        // Prevents the "submit all-blank → get answer key → restart" exploit.
        long answeredCount = request.getAnswers().stream()
                .filter(a -> a.getAnswer() != null && !a.getAnswer().isBlank()
                          || a.getSpokenText() != null && !a.getSpokenText().isBlank())
                .count();
        int minimumRequired = Math.max(1, generatedQuestions.size() / 2);
        if (answeredCount < minimumRequired) {
            throw new BadRequestException(
                    String.format("يجب الإجابة على ما لا يقل عن %d سؤال قبل التسليم", minimumRequired));
        }

        // Build an index map for fast lookup
        Map<Integer, AdaptiveAnswerItem> answerByIndex = request.getAnswers().stream()
                .collect(Collectors.toMap(AdaptiveAnswerItem::getQuestionIndex, a -> a, (a, b) -> a));

        int correctCount = 0;
        int pointsEarned = 0;
        List<AdaptiveAnswerFeedback> feedbackList = new ArrayList<>();

        for (int i = 0; i < generatedQuestions.size(); i++) {
            GeneratedQuestion gq = generatedQuestions.get(i);
            AdaptiveAnswerItem answerItem = answerByIndex.get(i);

            String studentAnswer = resolveStudentAnswer(answerItem);
            boolean isCorrect = evaluateAnswer(gq, studentAnswer);

            if (isCorrect) {
                correctCount++;
                pointsEarned += getPointsByDifficulty(gq.getDifficultyLevel());
            }

            // Update skill profile
            updateSkillProfile(student, attempt.getLesson(), gq.getSubSkill(), isCorrect);

            // Build feedback string
            String feedback = isCorrect
                    ? "أحسنت! إجابة صحيحة 🌟"
                    : "إجابة خاطئة. الإجابة الصحيحة هي: " + gq.getCorrectAnswer();

            // Persist response
            AdaptiveResponse resp = new AdaptiveResponse();
            resp.setAttempt(attempt);
            resp.setQuestionIndex(i);
            resp.setQuestionText(gq.getQuestionText());
            resp.setQuestionType(gq.getType());
            resp.setSubSkill(gq.getSubSkill());
            resp.setStudentAnswer(studentAnswer);
            resp.setCorrectAnswer(gq.getCorrectAnswer());
            resp.setIsCorrect(isCorrect);
            resp.setFeedback(feedback);
            responseRepository.save(resp);

            feedbackList.add(AdaptiveAnswerFeedback.builder()
                    .questionIndex(i)
                    .questionText(gq.getQuestionText())
                    .studentAnswer(studentAnswer)
                    .correctAnswer(gq.getCorrectAnswer())
                    .correct(isCorrect)
                    .feedback(feedback)
                    .subSkill(gq.getSubSkill())
                    .build());
        }

        // Compute score
        int totalQ = generatedQuestions.size();
        double score = totalQ > 0 ? (correctCount * 100.0) / totalQ : 0.0;

        // Award points (already summed by difficulty in the grading loop above)
        student.setTotalPoints((student.getTotalPoints() == null ? 0 : student.getTotalPoints()) + pointsEarned);
        studentRepository.save(student);

        // Close attempt
        attempt.setCorrectCount(correctCount);
        attempt.setScore(score);
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setCompletedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        // Build updated skill summaries
        List<SkillSummary> updatedSkills = toSummaries(
                skillProfileRepository.findByStudentIdAndLessonId(
                        student.getId(), attempt.getLesson().getId()),
                p -> true);

        return AdaptiveQuizResult.builder()
                .attemptId(attemptId)
                .score((int) Math.round(score))
                .correctCount(correctCount)
                .totalQuestions(totalQ)
                .pointsEarned(pointsEarned)
                .updatedSkills(updatedSkills)
                .feedback(feedbackList)
                .build();
    }

    // -----------------------------------------------------------------------
    // Hint for a generated question — with DB-persisted rate limiting
    // -----------------------------------------------------------------------

    /**
     * Returns a hint for one question in an active adaptive quiz.
     *
     * Limits enforced and persisted to the database:
     *   • max {@code quizConfig.maxHintsPerQuestion} hints per question (default 3)
     *   • max {@code quizConfig.maxHintsPerAttempt}  hints per attempt  (default 10)
     *
     * Both checks are performed inside a PESSIMISTIC_WRITE transaction so that
     * concurrent requests from the same student (double-tap, parallel tabs) cannot
     * both pass the limit gate simultaneously.
     */
    @Transactional
    public Map<String, Object> getHintForAdaptiveQuestion(Long attemptId,
                                                           int questionIndex,
                                                           int level,
                                                           Authentication authentication) {
        // Acquire row-level write lock — makes check-then-increment atomic.
        AdaptiveQuizAttempt attempt = attemptRepository.findByIdForUpdate(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("AdaptiveQuizAttempt", attemptId));

        // ── ownership & lifecycle guards ──────────────────────────────────────
        verifyAndGetStudent(authentication, attempt);

        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            throw new BadRequestException("لا يمكن طلب تلميح بعد تسليم الاختبار");
        }

        // ── validate question index ───────────────────────────────────────────
        List<GeneratedQuestion> questions = parseGeneratedQuestions(attempt.getGeneratedQuestionsJson());
        if (questionIndex < 0 || questionIndex >= questions.size()) {
            throw new BadRequestException("رقم السؤال غير صحيح");
        }

        // ── rate-limit: per-attempt total ─────────────────────────────────────
        int maxPerAttempt = quizConfig.getMaxHintsPerAttempt();
        if (attempt.getTotalHintsUsed() >= maxPerAttempt) {
            throw new TooManyRequestsException(
                    String.format("وصلت إلى الحد الأقصى من التلميحات (%d) لهذا الاختبار", maxPerAttempt));
        }

        // ── rate-limit: per-question ──────────────────────────────────────────
        int maxPerQuestion = quizConfig.getMaxHintsPerQuestion();
        Map<String, Integer> usageMap = parseHintUsageMap(attempt.getHintUsageJson());
        String key = String.valueOf(questionIndex);
        int usedForQuestion = usageMap.getOrDefault(key, 0);

        if (usedForQuestion >= maxPerQuestion) {
            throw new TooManyRequestsException(
                    String.format("وصلت إلى الحد الأقصى من التلميحات (%d) لهذا السؤال", maxPerQuestion));
        }

        // ── all checks passed — call Gemini ───────────────────────────────────
        int clampedLevel = Math.max(1, Math.min(level, quizConfig.getMaxHintLevel()));
        GeneratedQuestion gq = questions.get(questionIndex);

        log.info("[HINT] AdaptiveQuizService.getHint — attemptId={} questionIndex={} questionText=\"{}\" requestedLevel={} clampedLevel={} hintsUsedForQuestion={} totalHintsUsed={}",
                attemptId, questionIndex, gq.getQuestionText(), level, clampedLevel, usedForQuestion, attempt.getTotalHintsUsed());

        String hint = geminiService.generateHint(
                gq.getQuestionText(), gq.getCorrectAnswer(), clampedLevel, "ar");

        log.info("[HINT] AdaptiveQuizService.getHint — attemptId={} questionIndex={} level={} hintText=\"{}\"",
                attemptId, questionIndex, clampedLevel, hint);

        // ── persist incremented counters ──────────────────────────────────────
        usageMap.put(key, usedForQuestion + 1);
        attempt.setHintUsageJson(toJsonSilently(usageMap));
        attempt.setTotalHintsUsed(attempt.getTotalHintsUsed() + 1);
        attemptRepository.save(attempt);

        // ── build response with accurate remaining-hints values ───────────────
        int remainingForQuestion = maxPerQuestion - (usedForQuestion + 1);
        int remainingForAttempt  = maxPerAttempt  - attempt.getTotalHintsUsed();

        return Map.of(
                "hint",                    hint,
                "hintLevel",               clampedLevel,
                "hintsUsedForQuestion",    usedForQuestion + 1,
                "remainingForQuestion",    remainingForQuestion,
                "totalHintsUsed",          attempt.getTotalHintsUsed(),
                "remainingForAttempt",     remainingForAttempt
        );
    }

    /** Deserialise the hint-usage JSON map. Returns empty map on any parse failure. */
    private Map<String, Integer> parseHintUsageMap(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse hintUsageJson, resetting to empty: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    // -----------------------------------------------------------------------
    // Explanation for a submitted answer (Part 5)
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Map<String, Object> getAnswerExplanation(Long attemptId, int questionIndex,
                                                     Authentication authentication) {
        AdaptiveQuizAttempt attempt = resolveAttempt(attemptId);
        verifyAndGetStudent(authentication, attempt);

        if (attempt.getStatus() != AttemptStatus.SUBMITTED) {
            throw new BadRequestException("الشرح متاح فقط بعد تسليم الاختبار");
        }

        List<GeneratedQuestion> questions = parseGeneratedQuestions(attempt.getGeneratedQuestionsJson());
        if (questionIndex < 0 || questionIndex >= questions.size()) {
            throw new BadRequestException("رقم السؤال غير صحيح");
        }

        GeneratedQuestion gq = questions.get(questionIndex);
        AdaptiveResponse response = responseRepository
                .findByAttemptIdAndQuestionIndex(attemptId, questionIndex)
                .orElseThrow(() -> new BadRequestException("لم يتم الإجابة على هذا السؤال بعد"));

        String explanation = geminiService.generateAnswerExplanation(
                gq.getQuestionText(),
                response.getStudentAnswer(),
                gq.getCorrectAnswer(),
                Boolean.TRUE.equals(response.getIsCorrect()));

        return Map.of(
                "questionIndex",  questionIndex,
                "questionText",   gq.getQuestionText(),
                "studentAnswer",  response.getStudentAnswer() != null ? response.getStudentAnswer() : "",
                "correctAnswer",  gq.getCorrectAnswer(),
                "isCorrect",      Boolean.TRUE.equals(response.getIsCorrect()),
                "explanation",    explanation
        );
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Verifies the authenticated caller has access to the attempt's student, then
     * returns that student. Delegates to {@link StudentService#resolveStudent} so
     * STUDENT, PARENT, and ADMIN roles are all handled in one place.
     */
    private Student verifyAndGetStudent(Authentication authentication, AdaptiveQuizAttempt attempt) {
        Student resolved = studentService.resolveStudent(authentication, attempt.getStudent().getId());
        if (!resolved.getId().equals(attempt.getStudent().getId())) {
            throw new UnauthorizedException("هذا الاختبار لا يخصك");
        }
        return attempt.getStudent();
    }

    private Lesson resolveLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));
    }

    private AdaptiveQuizAttempt resolveAttempt(Long attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("AdaptiveQuizAttempt", attemptId));
    }

    private List<SkillSummary> toSummaries(List<StudentSkillProfile> profiles,
                                            java.util.function.Predicate<StudentSkillProfile> filter) {
        return profiles.stream()
                .filter(filter)
                .map(p -> SkillSummary.builder()
                        .subSkill(p.getSubSkill())
                        .accuracy(p.getAccuracy())
                        .totalAttempts(p.getTotalAttempts())
                        .currentDifficulty(p.getCurrentDifficulty())
                        .build())
                .collect(Collectors.toList());
    }

    /** Build payload from a persisted attempt (resume path). */
    private AdaptiveQuizPayload buildPayload(AdaptiveQuizAttempt attempt) {
        List<GeneratedQuestion> questions = parseGeneratedQuestions(attempt.getGeneratedQuestionsJson());
        return buildPayload(attempt, questions);
    }

    private AdaptiveQuizPayload buildPayload(AdaptiveQuizAttempt attempt,
                                              List<GeneratedQuestion> questions) {
        List<AdaptiveQuizItem> items = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            GeneratedQuestion gq = questions.get(i);
            items.add(AdaptiveQuizItem.builder()
                    .index(i)
                    .type(gq.getType())
                    .questionText(gq.getQuestionText())
                    .options(gq.getOptions())
                    .subSkill(gq.getSubSkill())
                    .difficultyLevel(gq.getDifficultyLevel())
                    .build());
        }

        List<String> focusSkills = parseFocusSkills(attempt.getFocusSkillsJson());

        return AdaptiveQuizPayload.builder()
                .attemptId(attempt.getId())
                .lessonId(attempt.getLesson().getId())
                .lessonTitle(attempt.getLesson().getTitle())
                .difficulty(attempt.getDifficultyLevel())
                .focusSkills(focusSkills)
                .questionCount(items.size())
                .questions(items)
                .source(attempt.getQuizSource())
                .build();
    }

    // -----------------------------------------------------------------------
    // DB fallback — used when Gemini fails after one retry
    // -----------------------------------------------------------------------

    /**
     * Selects questions from the lesson's question bank, applying the same
     * adaptive priorities as the Gemini path:
     *  1. Weak-skill questions at the target difficulty (highest priority)
     *  2. Weak-skill questions at any difficulty
     *  3. Questions at target difficulty
     *  4. Any remaining quizzable question
     *
     * Special-rendering types (TRACING, PRONUNCIATION, READING, IMAGE_MCQ, ORDERING)
     * are excluded because the client cannot display them without their assets.
     */
    private List<GeneratedQuestion> buildFallbackFromDatabase(
            Lesson lesson, int targetDifficulty, List<SkillSummary> weakSkills, int count) {

        List<Question> candidates = questionRepository.findByLessonIdAndTypeNotIn(
                lesson.getId(), FALLBACK_EXCLUDED_TYPES);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> weakSkillNames = weakSkills.stream()
                .map(SkillSummary::getSubSkill)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Question> weakAtTarget = new ArrayList<>();
        List<Question> weakAny      = new ArrayList<>();
        List<Question> atTarget     = new ArrayList<>();
        List<Question> rest         = new ArrayList<>();

        for (Question q : candidates) {
            boolean isWeak   = q.getSubSkill() != null && weakSkillNames.contains(q.getSubSkill());
            int     qLevel   = q.getDifficultyLevel() != null ? q.getDifficultyLevel() : 1;
            boolean isTarget = qLevel == targetDifficulty;

            if      (isWeak && isTarget) weakAtTarget.add(q);
            else if (isWeak)             weakAny.add(q);
            else if (isTarget)           atTarget.add(q);
            else                         rest.add(q);
        }

        // Shuffle each bucket so the same lesson doesn't always produce the same order
        Collections.shuffle(weakAtTarget);
        Collections.shuffle(weakAny);
        Collections.shuffle(atTarget);
        Collections.shuffle(rest);

        List<Question> merged = new ArrayList<>(candidates.size());
        merged.addAll(weakAtTarget);
        merged.addAll(weakAny);
        merged.addAll(atTarget);
        merged.addAll(rest);

        return merged.subList(0, Math.min(count, merged.size())).stream()
                .map(q -> convertToGeneratedQuestion(q, targetDifficulty))
                .collect(Collectors.toList());
    }

    private GeneratedQuestion convertToGeneratedQuestion(Question q, int fallbackDifficulty) {
        return GeneratedQuestion.builder()
                .type(q.getType() != null ? q.getType().name() : "MCQ")
                .questionText(q.getQuestionText())
                .correctAnswer(q.getCorrectAnswer())
                .options(parseOptionsFromJson(q.getOptions()))
                .subSkill(q.getSubSkill() != null ? q.getSubSkill() : "")
                .difficultyLevel(q.getDifficultyLevel() != null ? q.getDifficultyLevel() : fallbackDifficulty)
                .build();
    }

    /**
     * Handles the two storage formats Question.options can be in:
     *  • List<QuestionOption> — JSON objects with a "text" field (most common)
     *  • List<String>         — plain JSON string array (older records)
     */
    private List<String> parseOptionsFromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<QuestionOption> opts = objectMapper.readValue(json, new TypeReference<List<QuestionOption>>() {});
            return opts.stream()
                    .map(QuestionOption::getText)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            try {
                return objectMapper.readValue(json, new TypeReference<List<String>>() {});
            } catch (Exception ex) {
                log.warn("Could not parse options JSON: {}", ex.getMessage());
                return Collections.emptyList();
            }
        }
    }

    private boolean evaluateAnswer(GeneratedQuestion gq, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.isBlank()) return false;
        String type     = gq.getType() == null ? "" : gq.getType().toUpperCase();
        String correct  = normalizeArabic(gq.getCorrectAnswer());
        String student  = normalizeArabic(studentAnswer);
        // MCQ and TRUE_FALSE require exact match — substring logic causes false positives
        // (e.g., correct="ب" would match student answer "باب").
        return switch (type) {
            case "MCQ", "TRUE_FALSE", "IMAGE_MCQ" -> correct.equals(student);
            // SHORT_ANSWER / FILL_BLANK / ORDERING allow fuzzy match for partial credit
            default -> correct.equals(student) || student.contains(correct) || correct.contains(student);
        };
    }

    private void updateSkillProfile(Student student, Lesson lesson, String subSkill, boolean correct) {
        if (subSkill == null || subSkill.isBlank()) return;
        // Guard against Gemini producing unexpectedly long skill names (column is VARCHAR 64)
        final String skill = subSkill.length() > 64 ? subSkill.substring(0, 64) : subSkill;
        StudentSkillProfile profile = skillProfileRepository
                .findByStudentIdAndLessonIdAndSubSkill(student.getId(), lesson.getId(), skill)
                .orElseGet(() -> {
                    StudentSkillProfile p = new StudentSkillProfile();
                    p.setStudent(student);
                    p.setLesson(lesson);
                    p.setSubSkill(skill);
                    return p;
                });
        profile.recordAnswer(correct);
        skillProfileRepository.save(profile);
    }

    private String resolveStudentAnswer(AdaptiveAnswerItem item) {
        if (item == null) return "";
        if (item.getSpokenText() != null && !item.getSpokenText().isBlank()) return item.getSpokenText();
        return item.getAnswer() != null ? item.getAnswer() : "";
    }

    private List<GeneratedQuestion> parseGeneratedQuestions(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialise generatedQuestionsJson: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> parseFocusSkills(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String toJsonSilently(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON serialisation failed: {}", e.getMessage());
            return "[]";
        }
    }

    private int getPointsByDifficulty(int difficulty) {
        return switch (difficulty) {
            case 1 -> 5;
            case 2 -> 10;
            case 3 -> 15;
            case 4 -> 20;
            case 5 -> 25;
            default -> 5;
        };
    }

    private String normalizeArabic(String text) {
        if (text == null) return "";
        return text
                .replaceAll("[\\u064B-\\u065F\\u0670]", "")
                .replaceAll("[آأإ]", "ا")
                .replace('ة', 'ه')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }
}
