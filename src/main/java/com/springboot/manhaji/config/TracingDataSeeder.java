package com.springboot.manhaji.config;

import com.springboot.manhaji.entity.Lesson;
import com.springboot.manhaji.entity.Question;
import com.springboot.manhaji.entity.Subject;
import com.springboot.manhaji.entity.TracingQuestion;
import com.springboot.manhaji.entity.enums.QuestionType;
import com.springboot.manhaji.entity.enums.TracingCharacterType;
import com.springboot.manhaji.entity.enums.TracingLanguage;
import com.springboot.manhaji.repository.LessonRepository;
import com.springboot.manhaji.repository.QuestionRepository;
import com.springboot.manhaji.repository.SubjectRepository;
import com.springboot.manhaji.repository.TracingQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * Seeds tracing-question sample data on first startup.
 *
 * <p>Creates three subjects ("Tracing - Numbers", "Tracing - English Letters",
 * "Tracing - Arabic Letters") each with one lesson, and populates tracing questions
 * for the following characters:</p>
 * <ul>
 *   <li>Numbers  : 1, 2, 3</li>
 *   <li>English  : A, B, C</li>
 *   <li>Arabic   : أ, ب, ج</li>
 * </ul>
 *
 * <p>All expected paths are stored in a <strong>0-100 normalized coordinate space</strong>
 * so Flutter can scale them to any screen size. SVG viewBox is also 0-100.</p>
 *
 * <p>This seeder is idempotent: it skips seeding if tracing questions already exist.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TracingDataSeeder {

    private final SubjectRepository        subjectRepository;
    private final LessonRepository         lessonRepository;
    private final QuestionRepository       questionRepository;
    private final TracingQuestionRepository tracingQuestionRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (tracingQuestionRepository.count() > 0) {
            log.info("TracingDataSeeder: tracing questions already exist – skipping.");
            return;
        }

        log.info("TracingDataSeeder: seeding tracing questions…");

        seedNumbers();
        seedEnglishLetters();
        seedArabicLetters();

        log.info("TracingDataSeeder: done – {} tracing questions seeded.",
                tracingQuestionRepository.count());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NUMBERS
    // ─────────────────────────────────────────────────────────────────────────

    private void seedNumbers() {
        Subject subject = saveSubject("Tracing - Numbers", 1);
        Lesson lesson   = saveLesson(subject, "Numbers 1–3", "Trace numbers 1, 2, and 3", 1);

        // ── Number 1 ─────────────────────────────────────────────────────────
        // Two strokes: serif tick + main vertical bar + base serif
        saveTracingQuestion(lesson,
                "1", "Number One",
                TracingLanguage.NUMBERS, TracingCharacterType.NUMBER, 1,
                "M 45 15 L 50 10 L 55 15 L 50 15 L 50 90 M 38 90 L 62 90",
                "[" +
                "{\"x\":45,\"y\":15},{\"x\":50,\"y\":10},{\"x\":55,\"y\":15}," +
                "{\"x\":50,\"y\":15},{\"x\":50,\"y\":25},{\"x\":50,\"y\":35}," +
                "{\"x\":50,\"y\":45},{\"x\":50,\"y\":55},{\"x\":50,\"y\":65}," +
                "{\"x\":50,\"y\":75},{\"x\":50,\"y\":90}," +
                "{\"x\":38,\"y\":90},{\"x\":62,\"y\":90}" +
                "]",
                "[{\"order\":1,\"path\":\"M 45 15 L 50 10 L 55 15 L 50 15 L 50 90\"}" +
                ",{\"order\":2,\"path\":\"M 38 90 L 62 90\"}]",
                2, 70.0, 20.0
        );

        // ── Number 2 ─────────────────────────────────────────────────────────
        // Curved arc at top then diagonal down then horizontal base
        saveTracingQuestion(lesson,
                "2", "Number Two",
                TracingLanguage.NUMBERS, TracingCharacterType.NUMBER, 2,
                "M 30 22 C 38 8 68 8 70 28 C 72 44 52 54 28 88 H 68",
                "[" +
                "{\"x\":30,\"y\":22},{\"x\":38,\"y\":14},{\"x\":48,\"y\":10}," +
                "{\"x\":58,\"y\":10},{\"x\":66,\"y\":15},{\"x\":70,\"y\":26}," +
                "{\"x\":70,\"y\":36},{\"x\":64,\"y\":46},{\"x\":54,\"y\":54}," +
                "{\"x\":42,\"y\":64},{\"x\":34,\"y\":74},{\"x\":28,\"y\":84}," +
                "{\"x\":28,\"y\":90},{\"x\":40,\"y\":92},{\"x\":54,\"y\":92}," +
                "{\"x\":68,\"y\":92}" +
                "]",
                "[{\"order\":1,\"path\":\"M 30 22 C 38 8 68 8 70 28 C 72 44 52 54 28 88 H 68\"}]",
                1, 65.0, 25.0
        );

        // ── Number 3 ─────────────────────────────────────────────────────────
        // Upper arc + midpoint + lower arc
        saveTracingQuestion(lesson,
                "3", "Number Three",
                TracingLanguage.NUMBERS, TracingCharacterType.NUMBER, 2,
                "M 32 18 Q 62 6 68 28 Q 72 42 50 50 Q 74 56 70 74 Q 64 92 32 84",
                "[" +
                "{\"x\":32,\"y\":18},{\"x\":44,\"y\":11},{\"x\":56,\"y\":10}," +
                "{\"x\":65,\"y\":15},{\"x\":69,\"y\":26},{\"x\":66,\"y\":37}," +
                "{\"x\":58,\"y\":44},{\"x\":50,\"y\":48}," +
                "{\"x\":58,\"y\":52},{\"x\":67,\"y\":60},{\"x\":70,\"y\":72}," +
                "{\"x\":66,\"y\":82},{\"x\":56,\"y\":88},{\"x\":44,\"y\":88}," +
                "{\"x\":33,\"y\":84}" +
                "]",
                "[{\"order\":1,\"path\":\"M 32 18 Q 62 6 68 28 Q 72 42 50 50 Q 74 56 70 74 Q 64 92 32 84\"}]",
                1, 65.0, 25.0
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ENGLISH LETTERS
    // ─────────────────────────────────────────────────────────────────────────

    private void seedEnglishLetters() {
        Subject subject = saveSubject("Tracing - English Letters", 1);
        Lesson lesson   = saveLesson(subject, "English Letters A–C", "Trace uppercase letters A, B, and C", 1);

        // ── Letter A ─────────────────────────────────────────────────────────
        // Left stroke, right stroke, crossbar
        saveTracingQuestion(lesson,
                "A", "Letter A",
                TracingLanguage.ENGLISH, TracingCharacterType.LETTER_UPPERCASE_EN, 1,
                "M 50 10 L 18 90 M 50 10 L 82 90 M 32 62 L 68 62",
                "[" +
                // Left leg
                "{\"x\":50,\"y\":10},{\"x\":42,\"y\":28},{\"x\":34,\"y\":46}," +
                "{\"x\":26,\"y\":68},{\"x\":18,\"y\":90}," +
                // Return to apex for right leg
                "{\"x\":50,\"y\":10}," +
                "{\"x\":58,\"y\":28},{\"x\":66,\"y\":46},{\"x\":74,\"y\":68},{\"x\":82,\"y\":90}," +
                // Crossbar
                "{\"x\":32,\"y\":62},{\"x\":50,\"y\":62},{\"x\":68,\"y\":62}" +
                "]",
                "[{\"order\":1,\"path\":\"M 50 10 L 18 90\"}" +
                ",{\"order\":2,\"path\":\"M 50 10 L 82 90\"}" +
                ",{\"order\":3,\"path\":\"M 32 62 L 68 62\"}]",
                3, 68.0, 22.0
        );

        // ── Letter B ─────────────────────────────────────────────────────────
        // Vertical spine + top bump + bottom bump
        saveTracingQuestion(lesson,
                "B", "Letter B",
                TracingLanguage.ENGLISH, TracingCharacterType.LETTER_UPPERCASE_EN, 2,
                "M 25 10 L 25 90 M 25 10 Q 65 10 65 32 Q 65 50 25 50 Q 70 50 70 72 Q 70 90 25 90",
                "[" +
                // Spine
                "{\"x\":25,\"y\":10},{\"x\":25,\"y\":30},{\"x\":25,\"y\":50},{\"x\":25,\"y\":70},{\"x\":25,\"y\":90}," +
                // Top bump
                "{\"x\":25,\"y\":10},{\"x\":45,\"y\":10},{\"x\":58,\"y\":14},{\"x\":64,\"y\":22}," +
                "{\"x\":64,\"y\":34},{\"x\":56,\"y\":44},{\"x\":44,\"y\":50},{\"x\":25,\"y\":50}," +
                // Bottom bump
                "{\"x\":46,\"y\":50},{\"x\":60,\"y\":52},{\"x\":68,\"y\":60}," +
                "{\"x\":68,\"y\":74},{\"x\":60,\"y\":84},{\"x\":46,\"y\":90},{\"x\":25,\"y\":90}" +
                "]",
                "[{\"order\":1,\"path\":\"M 25 10 L 25 90\"}" +
                ",{\"order\":2,\"path\":\"M 25 10 Q 65 10 65 32 Q 65 50 25 50\"}" +
                ",{\"order\":3,\"path\":\"M 25 50 Q 70 50 70 72 Q 70 90 25 90\"}]",
                3, 65.0, 25.0
        );

        // ── Letter C ─────────────────────────────────────────────────────────
        // Single open arc
        saveTracingQuestion(lesson,
                "C", "Letter C",
                TracingLanguage.ENGLISH, TracingCharacterType.LETTER_UPPERCASE_EN, 1,
                "M 72 24 Q 50 5 26 24 Q 14 50 26 76 Q 50 95 72 76",
                "[" +
                "{\"x\":72,\"y\":24},{\"x\":62,\"y\":14},{\"x\":50,\"y\":10}," +
                "{\"x\":38,\"y\":11},{\"x\":28,\"y\":18},{\"x\":20,\"y\":30}," +
                "{\"x\":16,\"y\":44},{\"x\":18,\"y\":58},{\"x\":24,\"y\":70}," +
                "{\"x\":34,\"y\":80},{\"x\":46,\"y\":86},{\"x\":58,\"y\":84}," +
                "{\"x\":68,\"y\":78},{\"x\":72,\"y\":76}" +
                "]",
                "[{\"order\":1,\"path\":\"M 72 24 Q 50 5 26 24 Q 14 50 26 76 Q 50 95 72 76\"}]",
                1, 70.0, 20.0
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ARABIC LETTERS
    // ─────────────────────────────────────────────────────────────────────────

    private void seedArabicLetters() {
        Subject subject = saveSubject("Tracing - Arabic Letters", 1);
        Lesson lesson   = saveLesson(subject, "Arabic Letters أ–ج", "Trace Arabic letters أ, ب, and ج", 1);

        // ── أ  Alef with Hamza Above ─────────────────────────────────────────
        // Single vertical downstroke (the hamza above is a diacritic, not a stroke)
        saveTracingQuestion(lesson,
                "أ", "حرف الألف",
                TracingLanguage.ARABIC, TracingCharacterType.LETTER_AR, 1,
                "M 50 14 L 50 88",
                "[" +
                "{\"x\":50,\"y\":14},{\"x\":50,\"y\":24},{\"x\":50,\"y\":36}," +
                "{\"x\":50,\"y\":48},{\"x\":50,\"y\":60},{\"x\":50,\"y\":72}," +
                "{\"x\":50,\"y\":88}" +
                "]",
                "[{\"order\":1,\"path\":\"M 50 14 L 50 88\"}]",
                1, 72.0, 18.0
        );

        // ── ب  Ba ────────────────────────────────────────────────────────────
        // Horizontal baseline with a upward hook on the right side
        saveTracingQuestion(lesson,
                "ب", "حرف الباء",
                TracingLanguage.ARABIC, TracingCharacterType.LETTER_AR, 2,
                "M 80 52 Q 50 66 20 52 Q 14 42 22 32 Q 30 24 44 24",
                "[" +
                "{\"x\":80,\"y\":52},{\"x\":68,\"y\":57},{\"x\":54,\"y\":60}," +
                "{\"x\":40,\"y\":58},{\"x\":26,\"y\":52},{\"x\":18,\"y\":44}," +
                "{\"x\":18,\"y\":36},{\"x\":22,\"y\":30},{\"x\":30,\"y\":26}," +
                "{\"x\":40,\"y\":24},{\"x\":50,\"y\":24}" +
                "]",
                "[{\"order\":1,\"path\":\"M 80 52 Q 50 66 20 52 Q 14 42 22 32 Q 30 24 44 24\"}]",
                1, 68.0, 22.0
        );

        // ── ج  Jeem ──────────────────────────────────────────────────────────
        // Rounded body then a descending tail curling left
        saveTracingQuestion(lesson,
                "ج", "حرف الجيم",
                TracingLanguage.ARABIC, TracingCharacterType.LETTER_AR, 2,
                "M 60 22 Q 80 30 78 50 Q 74 68 52 72 Q 32 74 24 60 Q 18 46 26 34 " +
                "Q 34 24 48 24 Q 54 30 52 44 L 52 72",
                "[" +
                "{\"x\":60,\"y\":22},{\"x\":70,\"y\":28},{\"x\":76,\"y\":38}," +
                "{\"x\":76,\"y\":50},{\"x\":70,\"y\":60},{\"x\":60,\"y\":68}," +
                "{\"x\":48,\"y\":72},{\"x\":36,\"y\":70},{\"x\":26,\"y\":62}," +
                "{\"x\":20,\"y\":50},{\"x\":22,\"y\":38},{\"x\":30,\"y\":28}," +
                "{\"x\":42,\"y\":23},{\"x\":52,\"y\":26},{\"x\":56,\"y\":34}," +
                "{\"x\":54,\"y\":46},{\"x\":52,\"y\":60},{\"x\":52,\"y\":72}" +
                "]",
                "[{\"order\":1,\"path\":\"M 60 22 Q 80 30 78 50 Q 74 68 52 72 Q 32 74 24 60 Q 18 46 26 34 Q 34 24 48 24\"}" +
                ",{\"order\":2,\"path\":\"M 52 44 L 52 72\"}]",
                2, 62.0, 28.0
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SHARED HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Subject saveSubject(String name, int gradeLevel) {
        return subjectRepository.findAll().stream()
                .filter(s -> s.getName().equals(name) && s.getGradeLevel() == gradeLevel)
                .findFirst()
                .orElseGet(() -> {
                    Subject s = new Subject();
                    s.setName(name);
                    s.setGradeLevel(gradeLevel);
                    return subjectRepository.save(s);
                });
    }

    private Lesson saveLesson(Subject subject, String title, String content, int orderIndex) {
        return lessonRepository.findAll().stream()
                .filter(l -> l.getTitle().equals(title)
                        && l.getSubject().getId().equals(subject.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Lesson l = new Lesson();
                    l.setSubject(subject);
                    l.setTitle(title);
                    l.setContent(content);
                    l.setGradeLevel(subject.getGradeLevel());
                    l.setOrderIndex(orderIndex);
                    l.setSemesterNumber(1);
                    return lessonRepository.save(l);
                });
    }

    private void saveTracingQuestion(
            Lesson lesson,
            String character,
            String displayName,
            TracingLanguage language,
            TracingCharacterType characterType,
            int difficultyLevel,
            String svgPath,
            String expectedPointsJson,
            String strokeOrderJson,
            int strokeCount,
            double expectedAccuracy,
            double tolerancePercentage) {

        // Create + save the base Question
        Question question = new Question();
        question.setLesson(lesson);
        question.setType(QuestionType.TRACING);
        question.setQuestionText("اكتب الرمز: " + character);
        question.setCorrectAnswer(character);
        question.setDifficultyLevel(difficultyLevel);
        question.setSubSkill("tracing");
        Question savedQ = questionRepository.save(question);

        // Create + save the TracingQuestion config
        TracingQuestion tq = new TracingQuestion();
        tq.setQuestion(savedQ);
        tq.setCharacter(character);
        tq.setDisplayName(displayName);
        tq.setLanguage(language);
        tq.setCharacterType(characterType);
        tq.setSvgPath(svgPath);
        tq.setExpectedPointsJson(expectedPointsJson);
        tq.setStrokeOrderJson(strokeOrderJson);
        tq.setStrokeCount(strokeCount);
        tq.setExpectedAccuracy(expectedAccuracy);
        tq.setTolerancePercentage(tolerancePercentage);
        tracingQuestionRepository.save(tq);

        log.debug("  Seeded tracing question: '{}' (questionId={})", character, savedQ.getId());
    }

}
