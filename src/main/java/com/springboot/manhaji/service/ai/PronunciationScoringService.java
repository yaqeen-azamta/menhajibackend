package com.springboot.manhaji.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Language-aware pronunciation scoring.
 *
 * <p>Arabic: strip diacritics, unify hamza/taa-marbuta/alif-maqsura variants, then
 * Levenshtein similarity.
 *
 * <p>English: lowercase, strip punctuation, collapse to a simple phonetic code
 * (Metaphone-lite: drop silent letters, collapse double consonants, map
 * interchangeable sounds like c→k and ph→f), then Levenshtein on the codes.
 *
 * <p>The rating/feedback copy stays Arabic because the app UI is Arabic-first;
 * English phonetic scoring just means a 6-year-old who says "apple" gets
 * credit even if the transcription comes back as "appel" or "aple".
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PronunciationScoringService {

    public int score(String expected, String transcribed) {
        return score(expected, transcribed, null);
    }

    /**
     * @param language "ar", "en", or null to auto-detect from {@code expected}.
     *                 Auto-detect wins when any Arabic character is present;
     *                 otherwise English phonetic normalization is used.
     */
    public int score(String expected, String transcribed, String language) {
        if (expected == null || expected.isBlank()) return 0;
        if (transcribed == null || transcribed.isBlank()) return 0;

        String lang = (language == null || language.isBlank())
                ? detectLanguage(expected)
                : language;

        String a = normalize(expected, lang);
        String b = normalize(transcribed, lang);
        if (a.isEmpty() || b.isEmpty()) return 0;
        if (a.equals(b)) return 100;

        int distance = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 0;

        int similarity = (int) Math.round((1.0 - (double) distance / maxLen) * 100);
        return Math.max(0, Math.min(100, similarity));
    }

    /** Arabic unicode block U+0600..U+06FF → Arabic; anything else → English. */
    private String detectLanguage(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0600 && c <= 0x06FF) return "ar";
        }
        return "en";
    }

    public String rating(int score) {
        if (score >= 90) return "ممتاز";
        if (score >= 75) return "جيد جداً";
        if (score >= 60) return "جيد";
        if (score >= 40) return "حاول مرة أخرى";
        return "لم أسمعك جيداً";
    }

    public String feedback(int score, String expected) {
        if (score >= 90) return "نطق رائع! أحسنت.";
        if (score >= 75) return "نطق جيد جداً، استمر.";
        if (score >= 60) return "جيد، حاول النطق بوضوح أكثر.";
        if (score >= 40) return "كرر بعدي: " + expected;
        return "تأكد من النطق بصوت واضح.";
    }

    public boolean isCorrect(int score) {
        return score >= 60;
    }

    public int starsForScore(int score) {
        if (score >= 90) return 3;
        if (score >= 75) return 2;
        if (score >= 60) return 1;
        return 0;
    }

    private String normalize(String text, String language) {
        if ("en".equalsIgnoreCase(language)) {
            return normalizeEnglish(text);
        }
        return normalizeArabic(text);
    }

    private String normalizeArabic(String text) {
        String t = text.trim().toLowerCase();
        t = t.replaceAll("[\\u064B-\\u065F\\u0670]", "");
        t = t.replace('\u0623', '\u0627');
        t = t.replace('\u0625', '\u0627');
        t = t.replace('\u0622', '\u0627');
        t = t.replace('\u0629', '\u0647');
        t = t.replace('\u0649', '\u064A');
        t = t.replaceAll("[^\\p{L}\\p{N}]+", "");
        return t;
    }

    /**
     * Cheap English phonetic normalizer. Not full Metaphone — just the bits that
     * matter for Grade 1 vocabulary ("apple", "book", "cat", "dog", "red").
     *
     * <p>Rules applied in order:
     * <ul>
     *   <li>lowercase, strip punctuation and spaces</li>
     *   <li>"ph" → "f"; "ck" → "k"; "qu" → "kw"; "x" → "ks"</li>
     *   <li>"c" before e/i/y → "s"; other "c" → "k"</li>
     *   <li>drop silent leading "k" in "kn" ("knee"→"ne") and "w" in "wr"</li>
     *   <li>drop silent trailing "e" ("apple"→"appl")</li>
     *   <li>collapse any run of identical letters ("appl"→"apl")</li>
     * </ul>
     */
    private String normalizeEnglish(String text) {
        String t = text.trim().toLowerCase();
        t = t.replaceAll("[^a-z]+", "");
        if (t.isEmpty()) return t;

        t = t.replace("ph", "f");
        t = t.replace("ck", "k");
        t = t.replace("qu", "kw");
        t = t.replace("x", "ks");

        // c→s before e/i/y, else c→k
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch == 'c') {
                char next = (i + 1 < t.length()) ? t.charAt(i + 1) : ' ';
                sb.append((next == 'e' || next == 'i' || next == 'y') ? 's' : 'k');
            } else {
                sb.append(ch);
            }
        }
        t = sb.toString();

        // Silent leading kn-, wr-
        if (t.startsWith("kn")) t = t.substring(1);
        if (t.startsWith("wr")) t = t.substring(1);

        // Silent trailing e (but keep if word is only 2 chars, e.g. "be")
        if (t.length() > 2 && t.endsWith("e")) {
            t = t.substring(0, t.length() - 1);
        }

        // Collapse consecutive duplicates
        StringBuilder collapsed = new StringBuilder(t.length());
        char prev = '\0';
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch != prev) collapsed.append(ch);
            prev = ch;
        }
        return collapsed.toString();
    }

    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}
