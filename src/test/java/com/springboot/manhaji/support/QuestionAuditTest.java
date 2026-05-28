package com.springboot.manhaji.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.fail;

/**
 * Lints every curriculum JSON file under {@code src/main/resources/curriculum/}
 * against the rules in {@code Manhaji/docs/question-authoring-spec.md}.
 *
 * <p>The audit is split into two checks:
 *
 * <ul>
 *   <li><b>Schema integrity</b> ({@link #auditCurriculumSchemaIntegrity()}) — hard
 *       rules that break user experience if violated (MCQ correct answer not in
 *       options, ORDERING items mismatch, etc.). Fails the build.
 *   <li><b>Quality warnings</b> ({@link #auditCurriculumQualityWarnings()}) — soft
 *       rules that drive long-term content quality (≥8 questions per lesson,
 *       ≥1 difficulty-3 per lesson, ≥3 sub-skills covered). Prints a report to
 *       stderr but does NOT fail the build, so the project stays green during
 *       the multi-session Grade 1 backfill. Once that backfill ships these will
 *       be promoted to the strict check.
 * </ul>
 *
 * <p>The test deliberately avoids spinning up a Spring context — it parses JSON
 * directly via Jackson. This keeps it fast (~50ms) and makes it independent of
 * the entity / DTO layer, so refactors there do not silently break the lint.
 */
class QuestionAuditTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    /** Set of values we accept as TRUE_FALSE answers (Arabic + English). */
    private static final Set<String> TRUE_FALSE_ANSWERS = Set.of("صح", "خطأ", "True", "False");

    /** ORDERING delimiter — accepts Arabic comma (with or without space) or ASCII comma. */
    private static final Pattern ORDERING_DELIM = Pattern.compile("[،,]\\s*");

    /** Sub-skill enum values, per spec §6 — used to validate any explicit subSkill field. */
    private static final Set<String> VALID_SUB_SKILLS = Set.of(
            "recognition", "production", "pronunciation", "handwriting",
            "comprehension", "computation", "application",
            "memorization", "recitation");

    @Test
    @DisplayName("Schema integrity — hard rules per spec §10 (R1, R3–R11, R14, RU)")
    void auditCurriculumSchemaIntegrity() throws Exception {
        Audit audit = runAudit();
        if (!audit.strict.isEmpty()) {
            fail(buildReport("Curriculum schema audit", audit.strict,
                    "These are content-quality bugs (e.g. MCQ correct answer not in options, "
                            + "duplicate questionText, lesson missing difficulty-3, lesson under 8 questions)."));
        }
    }

    @Test
    @DisplayName("Quality warnings — soft rules per spec §10 (R15–R18) — non-blocking")
    void auditCurriculumQualityWarnings() throws Exception {
        Audit audit = runAudit();
        if (!audit.warnings.isEmpty()) {
            String report = buildReport("Curriculum quality warnings", audit.warnings,
                    "These will become hard failures after the Grade 1 backfill is complete. "
                            + "See Manhaji/docs/question-authoring-spec.md §10.");
            // Print to stderr so it shows in `./gradlew test` output, but do not fail.
            System.err.println(report);
        }
    }

    // ----------------------------------------------------------------
    // Audit driver
    // ----------------------------------------------------------------

    private Audit runAudit() throws Exception {
        List<File> jsonFiles = findCurriculumJsonFiles();
        if (jsonFiles.isEmpty()) {
            fail("No curriculum JSON files found on the test classpath under /curriculum");
        }

        Audit audit = new Audit();
        // Cross-file state: question text by subject (for duplicate detection).
        Map<String, Map<String, String>> textBySubject = new HashMap<>();

        for (File jsonFile : jsonFiles) {
            Map<String, Object> root = MAPPER.readValue(jsonFile, MAP_TYPE);
            String subject = String.valueOf(root.get("subject"));
            String fileTag = jsonFile.getName();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lessons = (List<Map<String, Object>>) root.get("lessons");
            if (lessons == null) {
                audit.strict("Missing lessons array", fileTag + ": file has no 'lessons' array");
                continue;
            }

            Map<String, String> seenInSubject =
                    textBySubject.computeIfAbsent(subject, k -> new HashMap<>());

            for (Map<String, Object> lesson : lessons) {
                String lessonTitle = String.valueOf(lesson.get("title"));
                String tag = fileTag + " :: " + lessonTitle;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> questions =
                        (List<Map<String, Object>>) lesson.get("questions");
                if (questions == null) questions = List.of();

                // -- LESSON-LEVEL CHECKS (soft) --

                // R8 (strict): ≥8 questions per lesson (12 ideal; 8 floor).
                if (questions.size() < 8) {
                    audit.strict("R8 — lesson has fewer than 8 questions",
                            tag + " (has " + questions.size() + ")");
                }

                // R9 (strict): ≥1 difficulty-3 question per lesson.
                boolean hasDifficulty3 = questions.stream().anyMatch(q -> {
                    Object d = q.get("difficultyLevel");
                    return d instanceof Integer && (Integer) d == 3;
                });
                if (!questions.isEmpty() && !hasDifficulty3) {
                    audit.strict("R9 — lesson has no difficulty-3 question", tag);
                }

                // R14 (strict): ≥3 distinct sub-skills covered (derived from question types).
                Set<String> subSkills = new HashSet<>();
                for (Map<String, Object> q : questions) {
                    subSkills.add(deriveSubSkill(q, subject));
                }
                if (!questions.isEmpty() && subSkills.size() < 3) {
                    audit.strict("R14 — lesson covers fewer than 3 sub-skills",
                            tag + " (covers: " + subSkills + ")");
                }

                // -- QUESTION-LEVEL CHECKS --
                int qIdx = 0;
                for (Map<String, Object> q : questions) {
                    qIdx++;
                    String qTag = tag + " #" + qIdx;
                    auditQuestion(q, qTag, subject, seenInSubject, audit);
                }
            }
        }
        return audit;
    }

    // ----------------------------------------------------------------
    // Question-level checks
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void auditQuestion(Map<String, Object> q,
                               String qTag,
                               String subject,
                               Map<String, String> seenInSubject,
                               Audit audit) {
        String type = stringOrNull(q.get("type"));
        String text = stringOrNull(q.get("questionText"));
        String correct = stringOrNull(q.get("correctAnswer"));
        Object optionsRaw = q.get("options");
        List<String> options = optionsRaw instanceof List
                ? toStringList((List<Object>) optionsRaw) : null;
        Object difficultyRaw = q.get("difficultyLevel");
        Integer difficulty = difficultyRaw instanceof Integer ? (Integer) difficultyRaw : null;

        // Universal (strict): questionText non-empty, ≤500 chars
        if (text == null || text.isBlank()) {
            audit.strict("RU — questionText is missing or blank", qTag);
        } else if (text.length() > 500) {
            audit.strict("RU — questionText exceeds 500 chars", qTag);
        }

        // Universal (strict): correctAnswer non-empty
        if (correct == null || correct.isBlank()) {
            audit.strict("RU — correctAnswer is missing or blank", qTag);
        }

        // R7 (strict): difficultyLevel ∈ {1,2,3}
        if (difficulty == null || difficulty < 1 || difficulty > 3) {
            audit.strict("R7 — difficultyLevel out of range",
                    qTag + " (= " + difficulty + ")");
        }

        // Optional explicit subSkill must be from the allowed set (strict).
        Object explicitSkill = q.get("subSkill");
        if (explicitSkill instanceof String s && !s.isBlank() && !VALID_SUB_SKILLS.contains(s)) {
            audit.strict("RU — subSkill not in allowed set",
                    qTag + " (=" + s + ")");
        }

        // Type-specific (strict)
        if (type == null) {
            audit.strict("RU — type missing", qTag);
            return;
        }
        switch (type) {
            case "MCQ" -> auditMcq(qTag, options, correct, audit);
            case "TRUE_FALSE" -> auditTrueFalse(qTag, options, correct, audit);
            case "ORDERING" -> auditOrdering(qTag, options, correct, audit);
            case "FILL_BLANK" -> auditFillBlank(qTag, text, audit);
            case "PRONUNCIATION", "TRACING", "SHORT_ANSWER" -> {
                if (options != null) {
                    audit.strict("R5/R6 — options must be null for " + type, qTag);
                }
            }
            default -> audit.strict("RU — unknown type " + type, qTag);
        }

        // R10 (strict): duplicate questionText within the same subject. Was demoted
        // to warning during the multi-session Grade 1 backfill (Apr 2026); promoted
        // back to strict after the backfill cleared all 75 known duplicates.
        if (text != null && !text.isBlank()) {
            String key = text.trim().toLowerCase(Locale.ROOT);
            String prior = seenInSubject.get(key);
            if (prior != null) {
                audit.strict("R10 — duplicate questionText within subject",
                        qTag + "  (also at " + prior + ")");
            } else {
                seenInSubject.put(key, qTag);
            }
        }

        // Media URL existence (warning, not strict — assets may be added in a later pass).
        String imageUrl = stringOrNull(q.get("imageUrl"));
        if (imageUrl != null && !imageUrl.isBlank()) {
            File f = resolveStaticAsset(imageUrl);
            if (f == null || !f.exists()) {
                audit.warning("RU — imageUrl points to missing file",
                        qTag + " (" + imageUrl + ")");
            }
        }
        String audioUrl = stringOrNull(q.get("audioUrl"));
        if (audioUrl != null && !audioUrl.isBlank()) {
            File f = resolveStaticAsset(audioUrl);
            if (f == null || !f.exists()) {
                audit.warning("RU — audioUrl points to missing file",
                        qTag + " (" + audioUrl + ")");
            }
        }
    }

    private void auditMcq(String tag, List<String> options, String correct, Audit audit) {
        if (options == null) {
            audit.strict("R3 — MCQ missing options array", tag);
            return;
        }
        if (options.size() < 3 || options.size() > 5) {
            audit.strict("R4 — MCQ options must be 3–5 items",
                    tag + " (has " + options.size() + ")");
        }
        if (correct != null && !options.contains(correct)) {
            audit.strict("R1 — MCQ correctAnswer not in options",
                    tag + " (correct=" + correct + ", options=" + options + ")");
        }
        // R11: MCQ with options including ONLY صح/خطأ should be TRUE_FALSE.
        if (options.size() <= 3 && options.contains("صح") && options.contains("خطأ")) {
            audit.strict("R11 — MCQ with صح/خطأ options should be TRUE_FALSE", tag);
        }
    }

    private void auditTrueFalse(String tag, List<String> options, String correct, Audit audit) {
        if (options != null) {
            audit.strict("R3 — TRUE_FALSE must have null options", tag);
        }
        if (correct == null || !TRUE_FALSE_ANSWERS.contains(correct.trim())) {
            audit.strict("R3 — TRUE_FALSE correctAnswer must be one of {صح, خطأ, True, False}",
                    tag + " (= " + correct + ")");
        }
    }

    private void auditOrdering(String tag, List<String> options, String correct, Audit audit) {
        if (options == null || options.size() < 3) {
            audit.strict("R6 — ORDERING needs ≥3 options", tag);
            return;
        }
        if (correct == null) {
            audit.strict("R5 — ORDERING correctAnswer missing", tag);
            return;
        }
        String[] tokens = ORDERING_DELIM.split(correct.trim());
        if (tokens.length < 3) {
            audit.strict("R5 — ORDERING correctAnswer must list ≥3 comma-separated items",
                    tag + " (= " + correct + ")");
            return;
        }
        // Token multiset must equal options multiset (after trim).
        List<String> normalizedCorrect = new ArrayList<>();
        for (String t : tokens) normalizedCorrect.add(t.trim());
        List<String> normalizedOptions = new ArrayList<>();
        for (String o : options) normalizedOptions.add(o.trim());
        java.util.Collections.sort(normalizedCorrect);
        java.util.Collections.sort(normalizedOptions);
        if (!normalizedCorrect.equals(normalizedOptions)) {
            audit.strict("R5 — ORDERING options/correctAnswer item set mismatch",
                    tag + " (correct=" + correct + ", options=" + options + ")");
        }
    }

    private void auditFillBlank(String tag, String text, Audit audit) {
        if (text == null) return;
        int count = countOccurrences(text, "___");
        if (count != 1) {
            audit.strict("R3 — FILL_BLANK questionText must contain exactly one '___' marker",
                    tag + " (found " + count + ")");
        }
    }

    // ----------------------------------------------------------------
    // Reporting
    // ----------------------------------------------------------------

    private static String buildReport(String title,
                                       Map<String, List<String>> bag,
                                       String footer) {
        StringBuilder report = new StringBuilder();
        int total = bag.values().stream().mapToInt(List::size).sum();
        report.append(title).append(": ")
                .append(total).append(" issues across ")
                .append(bag.size()).append(" rules:\n\n");
        for (Map.Entry<String, List<String>> e : bag.entrySet()) {
            report.append("=== ").append(e.getKey())
                    .append(" (").append(e.getValue().size()).append(") ===\n");
            int shown = 0;
            for (String v : e.getValue()) {
                if (shown++ >= 25) {
                    report.append("  … and ").append(e.getValue().size() - 25).append(" more\n");
                    break;
                }
                report.append("  • ").append(v).append("\n");
            }
            report.append("\n");
        }
        report.append(footer).append("\n");
        return report.toString();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private List<File> findCurriculumJsonFiles() throws Exception {
        URL url = getClass().getClassLoader().getResource("curriculum");
        if (url == null) return List.of();
        File dir = new File(url.toURI());
        if (!dir.isDirectory()) return List.of();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
        return files == null ? List.of() : List.of(files);
    }

    /**
     * Resolve a JSON-declared media URL like {@code /assets/questions/ar/letters/ra/x.png}
     * to the on-disk file under {@code src/main/resources/static/}.
     */
    private File resolveStaticAsset(String url) {
        try {
            String rel = url.startsWith("/") ? url.substring(1) : url;
            URL u = getClass().getClassLoader().getResource("static/" + rel);
            if (u == null) return null;
            return new File(u.toURI());
        } catch (Exception e) {
            return null;
        }
    }

    /** Map a question to its sub-skill — explicit if set, else derived from type. */
    private String deriveSubSkill(Map<String, Object> q, String subject) {
        Object explicit = q.get("subSkill");
        if (explicit instanceof String s && VALID_SUB_SKILLS.contains(s)) return s;

        String type = stringOrNull(q.get("type"));
        if (type == null) return "unknown";
        boolean isReligion = subject != null && subject.contains("الإسلامية");
        return switch (type) {
            case "MCQ", "TRUE_FALSE" -> isReligion ? "comprehension" : "recognition";
            case "SHORT_ANSWER", "FILL_BLANK" -> "production";
            case "ORDERING" -> "application";
            case "PRONUNCIATION" -> isReligion ? "recitation" : "pronunciation";
            case "TRACING" -> "handwriting";
            default -> "unknown";
        };
    }

    private static String stringOrNull(Object o) {
        return o == null ? null : o.toString();
    }

    private static List<String> toStringList(List<Object> list) {
        List<String> out = new ArrayList<>(list.size());
        for (Object o : list) out.add(o == null ? null : o.toString());
        return out;
    }

    private static int countOccurrences(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isEmpty()) return 0;
        int count = 0, idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /** Tiny holder for split strict / warning violation lists. */
    private static final class Audit {
        final Map<String, List<String>> strict = new LinkedHashMap<>();
        final Map<String, List<String>> warnings = new LinkedHashMap<>();
        void strict(String rule, String detail) {
            strict.computeIfAbsent(rule, k -> new ArrayList<>()).add(detail);
        }
        void warning(String rule, String detail) {
            warnings.computeIfAbsent(rule, k -> new ArrayList<>()).add(detail);
        }
    }
}
