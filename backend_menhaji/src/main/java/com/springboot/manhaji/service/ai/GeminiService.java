package com.springboot.manhaji.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.config.AiConfigProperties;
import com.springboot.manhaji.dto.adaptive.AdaptiveQuizContext;
import com.springboot.manhaji.dto.adaptive.GeneratedQuestion;
import com.springboot.manhaji.dto.adaptive.SkillSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final AiConfigProperties aiConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;  // audit TD3 (2026-04-29): Spring's auto-configured singleton

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    // Phrases that make a hint useless — any hint containing these is rejected
    private static final Set<String> GENERIC_HINT_PHRASES = Set.of(
        "think carefully", "read the question", "read again", "take your time",
        "look closely", "consider your knowledge", "what you have learned",
        "what you know", "try again", "focus on", "remember what", "go back",
        "re-read", "review", "recall what", "concentrate",
        "فكّر جيداً", "فكّر في السؤال", "تذكّر ما تعلمته", "تذكّر ما تعرفه",
        "راجع", "اقرأ السؤال", "خذ وقتك", "انتبه", "تأمّل", "حاول مرة أخرى",
        "تذكّر دروسك", "ما تعلمته في الدرس"
    );

    // Detects standard math expressions: "2 + 3", "٣ - ١", "10 × 2", etc.
    private static final Pattern MATH_EXPRESSION_PATTERN =
        Pattern.compile("([٠-٩0-9]+)\\s*([+\\-×÷*/])\\s*([٠-٩0-9]+)");

    // Jaccard token-overlap threshold above which two questions are considered duplicates
    private static final double DUPLICATE_SIMILARITY_THRESHOLD = 0.8;

    public boolean isAvailable() {
        boolean configured = aiConfig.getGemini().isConfigured();
        if (!configured) {
            String raw = aiConfig.getGemini().getApiKey();
            if ("not-set".equals(raw)) {
                log.warn("[GEMINI] DISABLED: GEMINI_API_KEY env var is not set (key resolved to 'not-set')");
            } else if (raw != null && raw.startsWith("REPLACE_")) {
                log.warn("[GEMINI] DISABLED: application-local.yaml still has placeholder '{}' — replace it with a real key", raw);
            } else {
                log.warn("[GEMINI] DISABLED: api-key='{}' is not a valid key", raw);
            }
        }
        return configured;
    }

    /**
     * Evaluate a short answer using Gemini AI.
     * Returns a Map with: isCorrect (boolean), feedback (String), hint (String)
     */
    public Map<String, Object> evaluateShortAnswer(String questionText, String correctAnswer, String studentAnswer, String language) {
        if (!isAvailable()) {
            return null; // Caller should fall back to string matching
        }

        String prompt = buildEvaluationPrompt(questionText, correctAnswer, studentAnswer, language);

        try {
            String response = callGemini(prompt);
            return parseEvaluationResponse(response);
        } catch (Exception e) {
            log.warn("Gemini evaluation failed, falling back to string matching: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate a hint for a question at the specified level (1–3).
     *
     * Level semantics:
     *   1 = gentle directional clue (topic / approach only)
     *   2 = stronger clue (a concrete step, analogy, or key property)
     *   3 = near-answer (full reasoning path, answer not stated)
     *
     * Validates the result and retries once with a stricter prompt if the hint is generic.
     * Falls back to a content-derived hint (never generic encouragement) if AI is unavailable.
     */
    public String generateHint(String questionText, String correctAnswer, int hintLevel, String language) {
        log.info("[HINT] request  — questionText=\"{}\" | hintLevel={}", questionText, hintLevel);

        if (!isAvailable()) {
            String fallback = getContentBasedFallbackHint(questionText, correctAnswer, hintLevel);
            log.warn("[HINT] Gemini unavailable — returning content fallback: level={} text=\"{}\"", hintLevel, fallback);
            return fallback;
        }

        try {
            String hint = callGemini(buildHintPrompt(questionText, correctAnswer, hintLevel));
            log.info("[HINT] Gemini response — level={} text=\"{}\"", hintLevel, hint);

            if (isGenericHint(hint, questionText)) {
                log.warn("[HINT] Generic hint detected at level={}, retrying with stricter prompt. rejected=\"{}\"", hintLevel, hint);
                hint = callGemini(buildStrictHintPrompt(questionText, correctAnswer, hintLevel));
                log.info("[HINT] Retry response — level={} text=\"{}\"", hintLevel, hint);
            }

            if (isGenericHint(hint, questionText)) {
                String fallback = getContentBasedFallbackHint(questionText, correctAnswer, hintLevel);
                log.warn("[HINT] Still generic after retry — using content fallback: level={} rejected=\"{}\" fallback=\"{}\"", hintLevel, hint, fallback);
                return fallback;
            }

            log.info("[HINT] Final hint — level={} text=\"{}\"", hintLevel, hint);
            return hint;
        } catch (Exception e) {
            String fallback = getContentBasedFallbackHint(questionText, correctAnswer, hintLevel);
            log.warn("[HINT] Gemini call failed: level={} error=\"{}\" — returning content fallback=\"{}\"", hintLevel, e.getMessage(), fallback);
            return fallback;
        }
    }

    /**
     * Level-aware prompt. Each level tightens the guidance:
     *   1 = topic/approach direction only
     *   2 = concrete step or analogy
     *   3 = full reasoning path (answer must NOT be stated)
     */
    private String buildHintPrompt(String questionText, String correctAnswer, int hintLevel) {
        String levelInstruction = switch (hintLevel) {
            case 1 -> """
                    HINT LEVEL 1 — Gentle direction:
                    Guide the child toward the general topic or approach.
                    Do NOT give specific steps, numbers, or properties.
                    Example: for "2 + 3", say "You can use counting to find the answer."
                    Example: for "What color is the sky?", say "Look outside on a sunny day and observe the sky."
                    """;
            case 2 -> """
                    HINT LEVEL 2 — Stronger clue:
                    Give a specific step, analogy, or key property that significantly narrows down the answer.
                    For math: mention the starting number and the operation direction.
                    For knowledge: give a concrete real-world association for the answer.
                    Example: for "2 + 3", say "Start with 2 apples, then add 3 more apples."
                    Example: for "What color is the sky?", say "It is the same color as the ocean on a clear day."
                    """;
            default -> """
                    HINT LEVEL 3 — Near-answer:
                    Give the complete reasoning path so the child can arrive at the answer on their own.
                    Walk through every step clearly. Do NOT state the final answer.
                    For math: count aloud step by step all the way to one before the answer.
                    For knowledge: describe the answer in full detail without naming it.
                    Example: for "2 + 3", say "Start at 2, count: 3, 4, 5 — the last number you reach is the answer."
                    Example: for "What color is the sky?", say "The sky looks the same color as a calm deep sea."
                    """;
        };

        return String.format("""
                You are an expert educational tutor for children aged 5-8.

                %s

                Rules that apply to ALL levels:
                - Use simple, child-friendly language.
                - NEVER say: "think carefully", "read again", "take your time", "remember what you learned".
                - NEVER mention answer length, word count, letter count, grammar, or question structure.
                - NEVER reveal the final answer directly.
                - Keep the hint short (maximum 20 words).
                - Return only the hint text with no prefix or label.
                - If the question is in Arabic, respond in Arabic. If in English, respond in English.

                Question: %s
                Answer: %s
                Hint:""", levelInstruction, questionText, correctAnswer);
    }

    /**
     * Stricter retry prompt used when the first attempt produced a generic hint.
     * Still level-aware so the retry produces the right depth of guidance.
     */
    private String buildStrictHintPrompt(String questionText, String correctAnswer, int hintLevel) {
        String levelLabel = switch (hintLevel) {
            case 1 -> "a gentle directional hint (topic or approach, no specific steps)";
            case 2 -> "a concrete clue (a specific step, analogy, or key property of the answer)";
            default -> "a near-answer hint (walk through all reasoning steps, stop just before stating the answer)";
        };

        return String.format("""
                You are an expert educational tutor for children aged 5-8.

                IMPORTANT: The previous hint was rejected because it was too generic.
                You MUST now generate %s.

                FORBIDDEN — any phrase that applies to ANY question:
                - "Think carefully", "Read the question", "Remember what you learned", "Take your time"

                REQUIRED — your hint MUST:
                - Reference specific content from THIS question
                - For math: use numbers, counting direction, or real objects (apples, fingers, blocks)
                - For general knowledge: give a real-world example or comparison specific to the answer
                - Be directly solvable — the child should be able to find the answer using only your hint

                Return ONLY the hint text. Maximum 20 words.
                If the question is in Arabic, respond in Arabic. If in English, respond in English.

                Question: %s
                Answer: %s
                Hint:""", levelLabel, questionText, correctAnswer);
    }

    /**
     * Generate an AI explanation of why an answer was correct or incorrect.
     * Used by the post-submission explanation endpoint.
     */
    public String generateAnswerExplanation(String questionText, String studentAnswer,
                                             String correctAnswer, boolean isCorrect) {
        if (!isAvailable()) {
            return buildStaticExplanation(correctAnswer, isCorrect);
        }

        String prompt = String.format("""
                أنت معلم فلسطيني لطيف ومشجع للأطفال في الصف الأول الابتدائي.

                السؤال: %s
                الإجابة الصحيحة: %s
                إجابة الطالب: %s
                النتيجة: %s

                %s

                قواعد الشرح:
                - جملة واحدة أو جملتان فقط
                - مناسب لطفل في السادسة من عمره — لا كلمات معقدة
                - اكتب بالعربية الفصحى البسيطة فقط
                """,
                questionText, correctAnswer, studentAnswer,
                isCorrect ? "صحيحة" : "خاطئة",
                isCorrect
                    ? "اشرح لماذا هذه الإجابة صحيحة بطريقة بسيطة وممتعة."
                    : "اشرح بلطف لماذا الإجابة خاطئة، ثم وضّح لماذا الإجابة الصحيحة هي الأفضل."
        );

        try {
            return callGemini(prompt);
        } catch (Exception e) {
            log.warn("Gemini explanation generation failed: {}", e.getMessage());
            return buildStaticExplanation(correctAnswer, isCorrect);
        }
    }

    /**
     * Generate practice questions for a lesson.
     * Returns raw JSON string from Gemini.
     */
    public String generateQuestions(String lessonContent, String subject, int count, int difficultyLevel) {
        if (!isAvailable()) {
            return null;
        }

        String prompt = String.format("""
                أنت معلم فلسطيني خبير. بناءً على محتوى الدرس التالي، أنشئ %d أسئلة بمستوى صعوبة %d (من 1-3).
                المادة: %s

                محتوى الدرس:
                %s

                أجب بصيغة JSON فقط كمصفوفة من الكائنات:
                [{"type": "MCQ"|"TRUE_FALSE"|"SHORT_ANSWER", "questionText": "...", "correctAnswer": "...", "options": ["..."] أو null, "difficultyLevel": %d}]
                """, count, difficultyLevel, subject, lessonContent, difficultyLevel);

        try {
            return callGemini(prompt);
        } catch (Exception e) {
            log.warn("Gemini question generation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Analyze PDF-extracted text and clean it up.
     */
    public String analyzePdfContent(String rawText, String subject) {
        if (!isAvailable()) {
            return null;
        }

        String prompt = String.format("""
                أنت خبير في المناهج الفلسطينية. النص التالي مستخرج من كتاب %s للصف الأول.
                قد يكون النص العربي مقلوباً أو مشوهاً بسبب استخراجه من PDF.

                المطلوب:
                1. أصلح أي نص عربي مقلوب أو مشوه
                2. نظّم المحتوى بشكل واضح
                3. حدد عنوان الدرس
                4. استخرج الأهداف التعليمية

                النص المستخرج:
                %s

                أجب بصيغة JSON:
                {"title": "عنوان الدرس", "content": "المحتوى المنظم", "objectives": "الأهداف التعليمية"}
                """, subject, rawText);

        try {
            return callGemini(prompt);
        } catch (Exception e) {
            log.warn("Gemini PDF analysis failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate a progress report summary for a student based on their performance data.
     */
    public String generateProgressReport(String studentName, int gradeLevel,
                                          String performanceData) {
        if (!isAvailable()) {
            return null;
        }

        String prompt = String.format("""
                أنت مستشار تربوي فلسطيني متخصص في التعليم الابتدائي.
                اكتب تقريراً مختصراً عن تقدم الطالب التالي:

                اسم الطالب: %s
                الصف: %d

                بيانات الأداء:
                %s

                المطلوب:
                1. ملخص عام عن أداء الطالب (2-3 جمل)
                2. نقاط القوة (2-3 نقاط)
                3. نقاط تحتاج تحسين (2-3 نقاط)
                4. توصيات لولي الأمر (2-3 نقاط)
                5. مستوى الخطر: LOW أو MEDIUM أو HIGH

                أجب بصيغة JSON فقط:
                {"summary": "...", "strengths": ["..."], "improvements": ["..."], "recommendations": ["..."], "riskLevel": "LOW|MEDIUM|HIGH"}
                """, studentName, gradeLevel, performanceData);

        try {
            return callGemini(prompt);
        } catch (Exception e) {
            log.warn("Gemini progress report generation failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate personalized learning path recommendations for a student.
     */
    public String generateLearningPath(String studentName, int gradeLevel,
                                        String weakAreas, String completedLessons) {
        if (!isAvailable()) {
            return null;
        }

        String prompt = String.format("""
                أنت معلم ذكي يصمم خطط تعلّم مخصصة لطلاب الصف الأول في فلسطين.

                الطالب: %s (الصف %d)

                المواضيع الضعيفة:
                %s

                الدروس المكتملة:
                %s

                صمّم خطة تعلّم مخصصة تتضمن:
                1. ترتيب الدروس المقترحة للمراجعة (الأضعف أولاً)
                2. أنشطة إضافية مقترحة
                3. نصائح للطالب

                أجب بصيغة JSON فقط:
                {"reviewLessons": [{"subject": "...", "topic": "...", "reason": "..."}], "activities": ["..."], "tips": ["..."]}
                """, studentName, gradeLevel, weakAreas, completedLessons);

        try {
            return callGemini(prompt);
        } catch (Exception e) {
            log.warn("Gemini learning path generation failed: {}", e.getMessage());
            return null;
        }
    }

    // --- Adaptive quiz generation ---

    /**
     * Generate an adaptive quiz tailored to the student's current skill profile.
     *
     * Deduplication pipeline (up to 3 Gemini attempts):
     *   1. All lesson DB questions and previous-attempt questions are sent to Gemini as forbidden.
     *   2. Each returned question is validated against: lesson questions, previous-attempt questions,
     *      already-accepted questions in this batch (exact match, same math operands, ≥80% Jaccard).
     *   3. Rejected questions are logged with their rejection reason.
     *   4. If after 3 attempts the quota is still not met, programmatic math variations fill the gap.
     *
     * Returns however many unique questions were found (may be fewer than requested).
     */
    public List<GeneratedQuestion> generateAdaptiveQuestions(AdaptiveQuizContext context) {
        if (!isAvailable()) {
            log.warn("[ADAPTIVE-GEN] Gemini not configured — cannot generate adaptive questions");
            return Collections.emptyList();
        }

        int needed = context.getQuestionCount();

        // Combine all initially forbidden texts (lesson bank + previous attempts)
        List<String> forbiddenTexts = new ArrayList<>();
        if (context.getExistingQuestions() != null)       forbiddenTexts.addAll(context.getExistingQuestions());
        if (context.getPreviousAttemptQuestions() != null) forbiddenTexts.addAll(context.getPreviousAttemptQuestions());

        log.info("[ADAPTIVE-GEN] Start: lessonQuestions={} prevAttemptQuestions={} target={}",
                context.getExistingQuestions()        != null ? context.getExistingQuestions().size()        : 0,
                context.getPreviousAttemptQuestions() != null ? context.getPreviousAttemptQuestions().size() : 0,
                needed);

        List<GeneratedQuestion> accepted    = new ArrayList<>();
        List<String>            rejectedTexts = new ArrayList<>();
        int totalDuplicates   = 0;
        int totalRegenerations = 0;

        AdaptiveQuizContext currentContext = context;

        for (int attempt = 1; attempt <= 3 && accepted.size() < needed; attempt++) {

            if (attempt > 1) {
                totalRegenerations++;
                // Build an extended forbidden list: original + accepted + previously rejected
                List<String> extended = new ArrayList<>(forbiddenTexts);
                accepted.stream().map(GeneratedQuestion::getQuestionText)
                        .filter(Objects::nonNull).forEach(extended::add);
                extended.addAll(rejectedTexts);

                currentContext = AdaptiveQuizContext.builder()
                        .lessonTitle(context.getLessonTitle())
                        .subjectName(context.getSubjectName())
                        .lessonContent(context.getLessonContent())
                        .lessonObjectives(context.getLessonObjectives())
                        .targetDifficulty(context.getTargetDifficulty())
                        .questionCount(needed - accepted.size())   // request only what's still missing
                        .weakSkills(context.getWeakSkills())
                        .strongSkills(context.getStrongSkills())
                        .existingQuestions(extended)
                        .previousAttemptQuestions(Collections.emptyList()) // already in extended
                        .build();

                log.info("[ADAPTIVE-GEN] Retry {}: requesting {} more questions, forbiddenCount={}",
                        attempt, needed - accepted.size(), extended.size());
            }

            List<GeneratedQuestion> generated;
            try {
                String raw = callGeminiWithHighTokens(buildAdaptiveQuizPrompt(currentContext));
                generated = parseGeneratedQuestions(raw);
            } catch (Exception e) {
                log.error("[ADAPTIVE-GEN] Gemini call failed on attempt {}: {}", attempt, e.getMessage(), e);
                continue;
            }

            log.info("[ADAPTIVE-GEN] Attempt {}: Gemini returned {} questions, need {} more",
                    attempt, generated.size(), needed - accepted.size());

            // Snapshot of accepted texts at start of this round for intra-batch comparison
            List<String> acceptedTexts = accepted.stream()
                    .map(GeneratedQuestion::getQuestionText)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(ArrayList::new));

            int roundDuplicates = 0;
            for (GeneratedQuestion gq : generated) {
                if (accepted.size() >= needed) break;

                if (gq.getQuestionText() == null || gq.getQuestionText().isBlank()) {
                    log.warn("[ADAPTIVE-GEN] Skipping question with null/blank text");
                    continue;
                }

                String reason = findDuplicateReason(gq.getQuestionText(), forbiddenTexts, acceptedTexts);
                if (reason != null) {
                    roundDuplicates++;
                    totalDuplicates++;
                    rejectedTexts.add(gq.getQuestionText());
                    log.warn("[ADAPTIVE-GEN] REJECTED (attempt {}): reason=\"{}\" | text=\"{}\"",
                            attempt, reason, gq.getQuestionText());
                } else {
                    accepted.add(gq);
                    acceptedTexts.add(gq.getQuestionText());
                    log.info("[ADAPTIVE-GEN] ACCEPTED (attempt {}): \"{}\"", attempt, gq.getQuestionText());
                }
            }

            log.info("[ADAPTIVE-GEN] After attempt {}: accepted={} roundDuplicates={} totalDuplicates={}",
                    attempt, accepted.size(), roundDuplicates, totalDuplicates);
        }

        // If we still need more and the lesson is math-heavy, fill with programmatic variations
        if (accepted.size() < needed) {
            List<String> allUsed = new ArrayList<>(forbiddenTexts);
            accepted.stream().map(GeneratedQuestion::getQuestionText).filter(Objects::nonNull).forEach(allUsed::add);
            allUsed.addAll(rejectedTexts);

            int mathFilled = 0;
            while (accepted.size() < needed) {
                GeneratedQuestion variation = generateMathVariation(context.getTargetDifficulty(), allUsed);
                if (variation == null) break;   // no more unique variations possible
                accepted.add(variation);
                allUsed.add(variation.getQuestionText());
                mathFilled++;
                log.info("[ADAPTIVE-GEN] Programmatic math variation filled slot: \"{}\"", variation.getQuestionText());
            }
            if (mathFilled > 0) {
                log.info("[ADAPTIVE-GEN] Filled {} slots with programmatic math variations", mathFilled);
            }
        }

        log.info("[ADAPTIVE-GEN] Complete: accepted={} | duplicatesRejected={} | regenerations={} | requested={}",
                accepted.size(), totalDuplicates, totalRegenerations, needed);

        return accepted;
    }

    /**
     * Returns a human-readable rejection reason if the candidate duplicates or closely resembles
     * any question in the forbidden list or the already-accepted batch; null if unique.
     *
     * Checks (in order):
     *   1. Exact match (after normalisation) against forbidden / batch
     *   2. Same math operands (catches "2 + 3 = ?" vs "٢ + ٣")
     *   3. Jaccard token-overlap ≥ DUPLICATE_SIMILARITY_THRESHOLD
     */
    private String findDuplicateReason(String candidate,
                                        List<String> forbiddenTexts,
                                        List<String> acceptedTexts) {
        String normCandidate = normalizeForComparison(candidate);

        for (String forbidden : forbiddenTexts) {
            String normForbidden = normalizeForComparison(forbidden);
            if (normForbidden.equals(normCandidate)) {
                return "exact match with forbidden question: \"" + shorten(forbidden) + "\"";
            }
            if (hasSameMathOperands(candidate, forbidden)) {
                return "same math operands as forbidden question: \"" + shorten(forbidden) + "\"";
            }
            double sim = jaccardSimilarity(normCandidate, normForbidden);
            if (sim >= DUPLICATE_SIMILARITY_THRESHOLD) {
                return String.format("%.0f%% similar to forbidden question: \"%s\"", sim * 100, shorten(forbidden));
            }
        }

        for (String acc : acceptedTexts) {
            String normAcc = normalizeForComparison(acc);
            if (normAcc.equals(normCandidate)) {
                return "exact match within this batch: \"" + shorten(acc) + "\"";
            }
            if (hasSameMathOperands(candidate, acc)) {
                return "same math operands as batch question: \"" + shorten(acc) + "\"";
            }
            double sim = jaccardSimilarity(normCandidate, normAcc);
            if (sim >= DUPLICATE_SIMILARITY_THRESHOLD) {
                return String.format("%.0f%% similar to batch question: \"%s\"", sim * 100, shorten(acc));
            }
        }

        return null; // unique — accepted
    }

    /** Jaccard token-overlap similarity (0.0–1.0). */
    private double jaccardSimilarity(String a, String b) {
        Set<String> setA = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> setB = new HashSet<>(Arrays.asList(b.split("\\s+")));
        setA.remove("");
        setB.remove("");
        if (setA.isEmpty() && setB.isEmpty()) return 1.0;
        if (setA.isEmpty() || setB.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return (double) intersection.size() / union.size();
    }

    /**
     * Returns true if both strings contain a math expression sharing the same operator
     * and the same operands (in either order for commutative ops: + and ×).
     */
    private boolean hasSameMathOperands(String q1, String q2) {
        Matcher m1 = MATH_EXPRESSION_PATTERN.matcher(q1);
        Matcher m2 = MATH_EXPRESSION_PATTERN.matcher(q2);
        if (!m1.find() || !m2.find()) return false;
        String l1 = toWesternDigits(m1.group(1)), op1 = m1.group(2).trim(), r1 = toWesternDigits(m1.group(3));
        String l2 = toWesternDigits(m2.group(1)), op2 = m2.group(2).trim(), r2 = toWesternDigits(m2.group(3));
        if (!op1.equals(op2)) return false;
        return (l1.equals(l2) && r1.equals(r2)) || (l1.equals(r2) && r1.equals(l2));
    }

    /** Normalise Arabic text for comparison (remove diacritics, unify letters, lowercase). */
    private String normalizeForComparison(String text) {
        if (text == null) return "";
        return text
                .toLowerCase()
                .replaceAll("[\\u064B-\\u065F\\u0670]", "")
                .replaceAll("[آأإ]", "ا")
                .replace('ة', 'ه')
                .replaceAll("[^\\w\\u0600-\\u06FF0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Generates a unique math variation question programmatically.
     * Used as a last-resort filler when Gemini cannot produce enough unique questions.
     * Returns null if no unique variation could be found within 30 random tries.
     */
    private GeneratedQuestion generateMathVariation(int targetDifficulty, List<String> usedTexts) {
        Random rng = new Random();
        int maxVal = targetDifficulty <= 2 ? 10 : (targetDifficulty <= 3 ? 20 : 50);
        String[] ops = {"+", "-"};

        for (int i = 0; i < 30; i++) {
            int a = rng.nextInt(maxVal) + 1;
            int b = rng.nextInt(maxVal) + 1;
            String op = ops[rng.nextInt(ops.length)];
            if (op.equals("-") && a < b) { int t = a; a = b; b = t; }

            int result = op.equals("+") ? a + b : a - b;
            String questionText  = "ما ناتج " + a + " " + op + " " + b + "؟";
            String correctAnswer = String.valueOf(result);

            if (findDuplicateReason(questionText, usedTexts, Collections.emptyList()) == null) {
                log.info("[ADAPTIVE-GEN] Math variation generated: \"{}\" = {}", questionText, correctAnswer);
                return GeneratedQuestion.builder()
                        .type("SHORT_ANSWER")
                        .questionText(questionText)
                        .correctAnswer(correctAnswer)
                        .options(null)
                        .subSkill("arithmetic")
                        .difficultyLevel(targetDifficulty)
                        .build();
            }
        }
        log.warn("[ADAPTIVE-GEN] Could not generate a unique math variation after 30 tries");
        return null;
    }

    private String shorten(String s) {
        if (s == null) return "";
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }

    private String buildAdaptiveQuizPrompt(AdaptiveQuizContext ctx) {
        // ---------- skill lists ----------
        String weakSkillsList = (ctx.getWeakSkills() == null || ctx.getWeakSkills().isEmpty())
                ? "لا توجد بيانات أداء سابقة — أنشئ أسئلة تغطي مهارات متنوعة من الدرس"
                : ctx.getWeakSkills().stream()
                        .map(s -> String.format("  • %s (نسبة الإتقان: %.0f%%، مستوى الصعوبة الحالي: %d من 5)",
                                s.getSubSkill(), s.getAccuracy(), s.getCurrentDifficulty()))
                        .collect(Collectors.joining("\n"));

        String strongSkillsList = (ctx.getStrongSkills() == null || ctx.getStrongSkills().isEmpty())
                ? "  • لا توجد"
                : ctx.getStrongSkills().stream()
                        .map(s -> String.format("  • %s (نسبة الإتقان: %.0f%%)",
                                s.getSubSkill(), s.getAccuracy()))
                        .collect(Collectors.joining("\n"));

        // ---------- question count split (60 % weak / 40 % strong) ----------
        int total       = ctx.getQuestionCount();
        int weakCount   = (int) Math.round(total * 0.6);
        int strongCount = total - weakCount;

        // ---------- type distribution (≥ 30 % each for MCQ & TRUE_FALSE, remainder SHORT_ANSWER) ----------
        int mcqCount   = Math.max(1, (int) Math.round(total * 0.4));
        int tfCount    = Math.max(1, (int) Math.round(total * 0.3));
        int saCount    = Math.max(1, total - mcqCount - tfCount);

        // ---------- lesson content — trim at sentence boundary ----------
        String rawContent = ctx.getLessonContent() == null ? "" : ctx.getLessonContent();
        String lessonContent = trimAtSentenceBoundary(rawContent, 900);

        String objectives = Objects.toString(ctx.getLessonObjectives(), "");

        // difficulty label for child-friendly explanation to the model
        String diffLabel = switch (ctx.getTargetDifficulty()) {
            case 1 -> "سهل جداً — مجرد تعرف على الكلمة أو الصورة";
            case 2 -> "سهل — فهم بسيط للمعنى";
            case 3 -> "متوسط — تطبيق ما تعلمه في جملة قصيرة";
            case 4 -> "صعب نسبياً — مقارنة بين مفهومين أو استنتاج";
            case 5 -> "تحدي — تطبيق إبداعي للمعلومة في سياق جديد";
            default -> "متوسط";
        };

        // Build forbidden questions block (lesson bank + previous attempts)
        List<String> allForbidden = new ArrayList<>();
        if (ctx.getExistingQuestions() != null)       allForbidden.addAll(ctx.getExistingQuestions());
        if (ctx.getPreviousAttemptQuestions() != null) allForbidden.addAll(ctx.getPreviousAttemptQuestions());

        String forbiddenBlock = allForbidden.isEmpty() ? "" :
                "══════════════════════════════════════\n"
                + "  أسئلة محظورة — يجب تجنبها تماماً\n"
                + "══════════════════════════════════════\n"
                + "لا تُنشئ أي سؤال يطابق أو يشبه الأسئلة التالية أو يعيد استخدام نفس أرقامها أو مفاهيمها الأساسية:\n"
                + allForbidden.stream()
                        .filter(Objects::nonNull)
                        .map(q -> "  ✗ " + q)
                        .collect(Collectors.joining("\n"))
                + "\n\nقواعد خاصة بالأسئلة الحسابية:\n"
                + "  - لا تعيد استخدام نفس الأرقام أو نفس زوج المعاملات (مثلاً إذا استُخدم ٢ + ٣ فلا تستخدمه أو ٣ + ٢)\n"
                + "  - لا تُنشئ سؤالاً ينتج عنه نفس الناتج مع نفس الأعداد\n"
                + "  - استخدم أرقاماً مختلفة تماماً في كل سؤال رياضي\n\n";

        return String.format("""
                أنت متخصص في تصميم الاختبارات التعليمية لأطفال الصف الأول الابتدائي في فلسطين (عمر 6-7 سنوات).

                ══════════════════════════════════════
                  معلومات الدرس
                ══════════════════════════════════════
                الدرس : %s
                المادة: %s
                محتوى الدرس:
                %s

                الأهداف التعليمية للدرس:
                %s

                %s
                ══════════════════════════════════════
                  ملف أداء الطالب
                ══════════════════════════════════════
                المهارات التي تحتاج تعزيزاً (%d أسئلة — 60%% من الاختبار):
                %s

                المهارات القوية للمراجعة السريعة (%d أسئلة — 40%% من الاختبار):
                %s

                مستوى الصعوبة المطلوب: %d من 5 (%s)

                ══════════════════════════════════════
                  قواعد صياغة الأسئلة — يجب الالتزام بها تماماً
                ══════════════════════════════════════
                1. اللغة والصياغة:
                   - استخدم لغة عربية فصحى مبسّطة يفهمها طفل في السادسة من عمره
                   - لا يتجاوز نص السؤال 8 كلمات
                   - لا تستخدم أفعالاً مضارعة مركّبة أو أساليب شرط معقدة
                   - تجنّب الكلمات غير المألوفة أو المصطلحات العلمية الصعبة
                   - لا تستخدم الضمائر المبهمة — اذكر الاسم صراحةً في كل سؤال

                2. نطاق المحتوى:
                   - اعتمد فقط على المعلومات الواردة في محتوى الدرس أعلاه
                   - لا تضف معلومات خارجية من معرفتك العامة حتى لو كانت صحيحة
                   - كل سؤال يجب أن يرتبط بهدف تعليمي من الأهداف المذكورة

                3. عدد الأسئلة والتنويع المطلوب:
                   - المجموع الكلي: %d سؤال بالضبط
                   - MCQ (اختيار من متعدد): %d أسئلة
                   - TRUE_FALSE (صح أو خطأ): %d أسئلة
                   - SHORT_ANSWER (إجابة قصيرة بكلمة واحدة أو كلمتين): %d أسئلة
                   - لا تتجاوز هذه الأعداد ولا تقل عنها

                4. جودة خيارات MCQ:
                   - أربعة خيارات فقط، خيار واحد صحيح
                   - الخيارات الخاطئة (المشتتات) يجب أن تكون خاطئة بوضوح — لا تستخدم خيارات تشبه الصحيح بدرجة تُربك الطفل
                   - جميع الخيارات من نفس النوع اللغوي (كلها أسماء أو كلها أفعال)
                   - لا يتجاوز كل خيار كلمتين

                5. قواعد TRUE_FALSE:
                   - options يجب أن يكون دائماً: ["صح", "خطأ"]
                   - correctAnswer يجب أن يكون إما "صح" أو "خطأ" بالضبط (لا "نعم" لا "صحيح" لا "صواب")
                   - لا تجعل جميع الإجابات "صح" — وزّع بين صح وخطأ

                6. قواعد SHORT_ANSWER:
                   - options يجب أن يكون null بالضبط
                   - correctAnswer يجب أن تكون كلمة واحدة أو كلمتين فقط
                   - الإجابة يجب أن تكون واضحة لا تقبل تأويلاً

                7. منع التكرار:
                   - لا تسأل عن نفس المعلومة مرتين حتى لو بصياغة مختلفة
                   - تأكد أن كل سؤال يختبر جانباً مختلفاً من الدرس
                   - إذا كانت المهارات الضعيفة متشابهة، طوّع الأسئلة لتختبر أبعاداً مختلفة من نفس المهارة

                ══════════════════════════════════════
                  تنسيق الإخراج — يجب الالتزام الكامل
                ══════════════════════════════════════
                أجب بـ JSON فقط. لا تكتب أي نص قبل المصفوفة أو بعدها. لا تستخدم ```json أو أي markdown.

                مثال على السؤال الأول من كل نوع:
                [
                  {
                    "type": "MCQ",
                    "questionText": "أين يعيش السمك؟",
                    "correctAnswer": "في الماء",
                    "options": ["في الماء", "في الهواء", "في الرمل", "في الشجر"],
                    "subSkill": "تعرف على بيئات الحيوانات",
                    "difficultyLevel": 1
                  },
                  {
                    "type": "TRUE_FALSE",
                    "questionText": "الشمس تشرق من الغرب.",
                    "correctAnswer": "خطأ",
                    "options": ["صح", "خطأ"],
                    "subSkill": "معرفة الاتجاهات",
                    "difficultyLevel": 1
                  },
                  {
                    "type": "SHORT_ANSWER",
                    "questionText": "ما لون السماء في النهار؟",
                    "correctAnswer": "أزرق",
                    "options": null,
                    "subSkill": "الألوان",
                    "difficultyLevel": 1
                  }
                ]

                الآن أنشئ %d سؤالاً كاملاً وفق القواعد السابقة. ابدأ المصفوفة مباشرة:
                """,
                ctx.getLessonTitle(), ctx.getSubjectName(),
                lessonContent, objectives,
                forbiddenBlock,
                weakCount, weakSkillsList,
                strongCount, strongSkillsList,
                ctx.getTargetDifficulty(), diffLabel,
                total, mcqCount, tfCount, saCount,
                total
        );
    }

    /** Trim content at the nearest sentence boundary (Arabic '.' or '،') before maxLen chars. */
    private String trimAtSentenceBoundary(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text == null ? "" : text;
        int cutoff = maxLen;
        // Walk back to find a sentence-ending punctuation
        for (int i = maxLen; i > maxLen - 200 && i > 0; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '،' || c == '؟' || c == '!' || c == '\n') {
                cutoff = i + 1;
                break;
            }
        }
        return text.substring(0, cutoff).trim();
    }

    @SuppressWarnings("unchecked")
    private List<GeneratedQuestion> parseGeneratedQuestions(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();

        // Strip code fences
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence    = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).trim();
            }
        }

        // Find the first '[' to skip any preamble text Gemini may have prepended
        int arrayStart = text.indexOf('[');
        int arrayEnd   = text.lastIndexOf(']');
        if (arrayStart < 0 || arrayEnd < arrayStart) {
            log.warn("No JSON array found in Gemini adaptive response");
            return Collections.emptyList();
        }
        text = text.substring(arrayStart, arrayEnd + 1);

        try {
            List<Map<String, Object>> raw2 = objectMapper.readValue(text, new TypeReference<>() {});
            return raw2.stream()
                       .map(this::mapToGeneratedQuestion)
                       .filter(Objects::nonNull)
                       .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse generated questions JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private GeneratedQuestion mapToGeneratedQuestion(Map<String, Object> m) {
        try {
            String type         = Objects.toString(m.get("type"),         "MCQ");
            String questionText = Objects.toString(m.get("questionText"), "");
            String correctAnswer = Objects.toString(m.get("correctAnswer"), "");
            String subSkill     = Objects.toString(m.get("subSkill"),     "عام");
            int difficulty = m.get("difficultyLevel") instanceof Number n ? n.intValue() : 1;

            List<String> options = null;
            if (m.get("options") instanceof List<?> rawOpts) {
                options = rawOpts.stream().map(Objects::toString).collect(Collectors.toList());
            }

            return GeneratedQuestion.builder()
                    .type(type)
                    .questionText(questionText)
                    .correctAnswer(correctAnswer)
                    .options(options)
                    .subSkill(subSkill)
                    .difficultyLevel(difficulty)
                    .build();
        } catch (Exception e) {
            log.warn("Skipping malformed question entry: {}", e.getMessage());
            return null;
        }
    }

    /** Like callGemini but requests more output tokens for question arrays. */
    private String callGeminiWithHighTokens(String prompt) {
        String model  = aiConfig.getGemini().getModel();
        String apiKey = aiConfig.getGemini().getApiKey();
        String url = String.format("%s/models/%s:generateContent?key=%s", GEMINI_BASE_URL, model, apiKey);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.15,   // low temp for reliable JSON structure; slight variance for creative content
                        "maxOutputTokens", 4096
                )
        );

        String responseJson = webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(java.time.Duration.ofSeconds(30));

        return extractTextFromGeminiResponse(responseJson);
    }

    // --- Internal methods ---

    private String callGemini(String prompt) {
        String model = aiConfig.getGemini().getModel();
        String apiKey = aiConfig.getGemini().getApiKey();
        String url = String.format("%s/models/%s:generateContent?key=%s", GEMINI_BASE_URL, model, apiKey);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 1024
                )
        );

        String responseJson = webClientBuilder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(java.time.Duration.ofSeconds(12));

        return extractTextFromGeminiResponse(responseJson);
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromGeminiResponse(String json) {
        if (json == null || json.isBlank()) {
            log.error("Gemini returned an empty body");
            throw new RuntimeException("Gemini returned empty response");
        }
        try {
            var root = objectMapper.readTree(json);
            // Surface Gemini-level errors (e.g. quota exceeded) before attempting text extraction.
            if (root.has("error")) {
                String errMsg = root.path("error").path("message").asText("unknown");
                log.error("Gemini API error: {}", errMsg);
                throw new RuntimeException("Gemini API error: " + errMsg);
            }
            String text = root.path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText(null);
            if (text == null || text.isBlank()) {
                log.error("Gemini response contained no text field. Full body: {}", json);
                throw new RuntimeException("Gemini response contained no text");
            }
            return text;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }

    private Map<String, Object> parseEvaluationResponse(String response) {
        String trimmed = response == null ? "" : response.trim();

        // Strip Markdown code-fences (```json ... ``` or ``` ... ```) — Gemini sometimes wraps JSON.
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }

        // Try strict JSON first (the prompt requests JSON-only).
        try {
            var node = objectMapper.readTree(trimmed);
            if (node.has("isCorrect")) {
                return Map.of(
                        "isCorrect", node.get("isCorrect").asBoolean(false),
                        "feedback", node.has("feedback") ? node.get("feedback").asText("") : trimmed
                );
            }
        } catch (Exception ignored) {
            // Fall through to heuristic
        }

        // Heuristic fallback for narrative responses (audit fix 2026-04-29):
        // The previous fallback looked for the literal substring "iscorrect: true",
        // which a narrative response will essentially never contain. Instead match
        // on Arabic/English correctness vocabulary the prompt instructs Gemini to use.
        String lower = trimmed.toLowerCase();
        boolean isCorrect = trimmed.startsWith("صحيحة")
                         || trimmed.startsWith("صحيح")
                         || trimmed.startsWith("صحّ")
                         || lower.startsWith("correct")
                         || lower.contains("\"iscorrect\":true")
                         || lower.contains("\"iscorrect\": true");

        return Map.of(
                "isCorrect", isCorrect,
                "feedback", trimmed
        );
    }

    private String buildEvaluationPrompt(String questionText, String correctAnswer, String studentAnswer, String language) {
        return String.format("""
                أنت معلم فلسطيني لطيف ومشجع للأطفال في الصف الأول.

                السؤال: %s
                الإجابة الصحيحة: %s
                إجابة الطالب: %s

                قيّم إجابة الطالب. إذا كانت الإجابة صحيحة أو قريبة من الصحيحة، اعتبرها صحيحة.
                كن لطيفاً ومشجعاً في ردك. اجعل التغذية الراجعة جملة أو جملتين فقط، مناسبة لطفل في الصف الأول.

                IMPORTANT: Respond with JSON only. Do NOT add any prose before or after.
                Format:
                {"isCorrect": true|false, "feedback": "<one or two short sentences in Arabic>"}
                """, questionText, correctAnswer, studentAnswer);
    }

    /**
     * Returns true if the hint is generic and provides no educational guidance.
     * Checks for forbidden phrases and, for math questions, verifies that
     * the hint contains at least one math-reasoning concept.
     */
    private boolean isGenericHint(String hint, String questionText) {
        if (hint == null || hint.isBlank()) return true;
        String lower = hint.toLowerCase();
        for (String phrase : GENERIC_HINT_PHRASES) {
            if (lower.contains(phrase.toLowerCase())) return true;
        }
        if (isMathQuestion(questionText)) {
            boolean hasMathConcept =
                lower.matches(".*\\d.*") ||
                lower.contains("count") || lower.contains("add") || lower.contains("subtract") ||
                lower.contains("plus") || lower.contains("more") || lower.contains("apples") ||
                lower.contains("fingers") || lower.contains("blocks") || lower.contains("step") ||
                lower.contains("عدّ") || lower.contains("اجمع") || lower.contains("أضف") ||
                lower.contains("أكثر") || lower.contains("تفاحات") || lower.contains("أصابع") ||
                lower.contains("خطوة") || lower.contains("للأمام") || lower.contains("للخلف");
            if (!hasMathConcept) return true;
        }
        return false;
    }

    private boolean isMathQuestion(String questionText) {
        if (questionText == null) return false;
        return MATH_EXPRESSION_PATTERN.matcher(questionText).find()
            || questionText.matches(".*[+\\-×÷*/].*")
            || questionText.toLowerCase().matches(".*(add|subtract|multiply|divide|sum|difference|product|plus|minus).*")
            || questionText.matches(".*(ناتج|اجمع|اطرح|جمع|طرح|ضرب|قسمة).*");
    }

    /**
     * Content-aware fallback used when AI is unavailable or produces a generic hint.
     * Differentiates by level so the child receives progressively stronger help even offline.
     *
     *   Level 1 — direction only (counting approach for math, subject anchor for text)
     *   Level 2 — concrete counting step or first word of the answer topic
     *   Level 3 — full step-by-step path, stopping just before the final answer
     */
    private String getContentBasedFallbackHint(String questionText, String correctAnswer, int hintLevel) {
        boolean isArabic = questionText != null && questionText.matches(".*[\\u0600-\\u06FF].*");

        if (isMathQuestion(questionText)) {
            return buildMathFallbackHint(questionText, isArabic, hintLevel);
        }

        // Non-math fallback: escalate amount of information revealed with the level
        String anchor = questionText != null ? questionText.replaceAll("[?؟]", "").trim() : "";
        if (anchor.length() > 50) anchor = anchor.substring(0, 50) + "…";

        if (hintLevel >= 3 && correctAnswer != null && !correctAnswer.isBlank()) {
            // Level 3: describe the answer category without naming it
            return isArabic
                    ? "الإجابة هي: " + correctAnswer.substring(0, Math.min(correctAnswer.length(), 3)) + "…"
                    : "The answer starts with: " + correctAnswer.substring(0, Math.min(correctAnswer.length(), 3)) + "…";
        }
        return isArabic
                ? "فكّر في السؤال: " + anchor
                : "Think about this question: " + anchor;
    }

    /**
     * Level-aware math fallback hint derived directly from the expression.
     *
     *   Level 1: describes the operation approach (e.g. "You can use counting to add.")
     *   Level 2: gives the starting point and direction (e.g. "Start with 2 and count 3 more forward.")
     *   Level 3: enumerates every intermediate step up to the answer (e.g. "Start at 2: count 3, 4, 5.")
     */
    private String buildMathFallbackHint(String questionText, boolean isArabic, int hintLevel) {
        Matcher m = MATH_EXPRESSION_PATTERN.matcher(questionText);
        if (!m.find()) {
            return isArabic
                    ? "استخدم أصابعك لتعدّ الأرقام خطوة بخطوة."
                    : "Use your fingers to count step by step.";
        }

        String left  = toWesternDigits(m.group(1));
        String op    = m.group(2).trim();
        String right = toWesternDigits(m.group(3));

        if (hintLevel == 1) {
            return switch (op) {
                case "+"        -> isArabic ? "يمكنك استخدام العدّ للإضافة." : "You can use counting to add.";
                case "-"        -> isArabic ? "يمكنك العدّ للخلف للطرح." : "You can count backwards to subtract.";
                case "×", "*"  -> isArabic ? "الضرب هو جمع متكرر للعدد نفسه." : "Multiplication means adding the same number repeatedly.";
                case "÷", "/"  -> isArabic ? "القسمة هي توزيع متساوٍ للأشياء." : "Division means sharing things into equal groups.";
                default         -> isArabic ? "استخدم أصابعك لتعدّ الأرقام." : "Use your fingers to count the numbers.";
            };
        }

        if (hintLevel == 2) {
            return switch (op) {
                case "+"       -> isArabic
                        ? "ابدأ بالعدد " + left + " ثم عدّ " + right + " خطوات للأمام."
                        : "Start with " + left + " and count " + right + " more steps forward.";
                case "-"       -> isArabic
                        ? "ابدأ بالعدد " + left + " ثم عدّ " + right + " خطوات للخلف."
                        : "Start with " + left + " and count back " + right + " steps.";
                case "×", "*" -> isArabic
                        ? "اجمع العدد " + left + " مع نفسه " + right + " مرات."
                        : "Add " + left + " to itself " + right + " times.";
                case "÷", "/" -> isArabic
                        ? "وزّع " + left + " على مجموعات كل مجموعة فيها " + right + "."
                        : "Split " + left + " into equal groups of " + right + ".";
                default        -> isArabic ? "استخدم أصابعك." : "Use your fingers.";
            };
        }

        // Level 3: enumerate steps
        return switch (op) {
            case "+" -> {
                int l = parseIntSafe(left), r = parseIntSafe(right);
                String steps = buildCountingSteps(l + 1, l + r);
                yield isArabic
                        ? "ابدأ من " + left + "، عدّ: " + steps + " — آخر رقم هو الإجابة."
                        : "Start at " + left + ", count: " + steps + " — the last number is the answer.";
            }
            case "-" -> {
                int l = parseIntSafe(left), r = parseIntSafe(right);
                String steps = buildCountingStepsDown(l - 1, l - r);
                yield isArabic
                        ? "ابدأ من " + left + "، عدّ للخلف: " + steps + " — آخر رقم هو الإجابة."
                        : "Start at " + left + ", count back: " + steps + " — the last number is the answer.";
            }
            default -> isArabic
                    ? "اجمع العدد " + left + " مع نفسه " + right + " مرات وستجد الإجابة."
                    : "Add " + left + " to itself " + right + " times and you will find the answer.";
        };
    }

    private String buildCountingSteps(int from, int to) {
        if (from > to) return String.valueOf(to);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to; i++) {
            if (i > from) sb.append(", ");
            sb.append(i);
        }
        return sb.toString();
    }

    private String buildCountingStepsDown(int from, int to) {
        if (from < to) return String.valueOf(to);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i >= to; i--) {
            if (i < from) sb.append(", ");
            sb.append(i);
        }
        return sb.toString();
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private String toWesternDigits(String s) {
        return s.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3')
                .replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7')
                .replace('٨','8').replace('٩','9');
    }

    private String buildStaticExplanation(String correctAnswer, boolean isCorrect) {
        return isCorrect
            ? "إجابتك صحيحة! «" + correctAnswer + "» هي الإجابة الصحيحة."
            : "الإجابة الصحيحة هي: «" + correctAnswer + "». راجع هذه المعلومة في درسك.";
    }
}
