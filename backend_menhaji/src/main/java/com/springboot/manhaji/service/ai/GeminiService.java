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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final AiConfigProperties aiConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;  // audit TD3 (2026-04-29): Spring's auto-configured singleton

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    public boolean isAvailable() {
        return aiConfig.getGemini().isConfigured();
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
     * Generate a hint for a question at the specified level (1-3).
     */
    public String generateHint(String questionText, String correctAnswer, int hintLevel, String language) {
        if (!isAvailable()) {
            return getDefaultHint(hintLevel, language);
        }

        String prompt = String.format("""
                أنت معلم فلسطيني لطيف للأطفال في الصف الأول.
                السؤال: %s
                الإجابة الصحيحة: %s

                أعطِ تلميحاً من المستوى %d (من 3 مستويات):
                - المستوى 1: تلميح عام وغامض
                - المستوى 2: تلميح أكثر تحديداً
                - المستوى 3: تلميح قريب جداً من الإجابة

                أجب بالتلميح فقط، بجملة واحدة بسيطة مناسبة لطفل.
                """, questionText, correctAnswer, hintLevel);

        try {
            return callGemini(prompt);
        } catch (Exception e) {
            log.warn("Gemini hint generation failed: {}", e.getMessage());
            return getDefaultHint(hintLevel, language);
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
     * Returns an ordered list of questions that can be serialised and stored on the attempt.
     */
    public List<GeneratedQuestion> generateAdaptiveQuestions(AdaptiveQuizContext context) {
        if (!isAvailable()) {
            log.warn("Gemini is not configured – cannot generate adaptive questions");
            return Collections.emptyList();
        }
        String prompt = buildAdaptiveQuizPrompt(context);
        try {
            String raw = callGeminiWithHighTokens(prompt);
            return parseGeneratedQuestions(raw);
        } catch (Exception e) {
            log.error("Adaptive question generation failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
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

    private String getDefaultHint(int level, String language) {
        return switch (level) {
            case 1 -> "فكّر جيداً في السؤال 🤔";
            case 2 -> "أنت قريب من الإجابة! حاول مرة أخرى 💪";
            case 3 -> "التلميح الأخير: راجع الدرس وستجد الإجابة ⭐";
            default -> "حاول مرة أخرى!";
        };
    }
}
