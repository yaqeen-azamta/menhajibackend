package com.springboot.manhaji.service.reading;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Word-level comparison between an original lesson text and a child's transcribed reading.
 *
 * <h3>Algorithm</h3>
 * <ol>
 *   <li>Collapse extra whitespace and strip leading/trailing space from both texts.</li>
 *   <li>Tokenize on whitespace and Arabic/Latin punctuation.</li>
 *   <li>Normalize each token with {@link #normalize} — see below.</li>
 *   <li>Greedy left-to-right matching: for each original word, consume the first
 *       unmatched recognized word that is identical after normalization.</li>
 *   <li>Derive correct / missing / incorrect word lists and the accuracy percentage.</li>
 * </ol>
 *
 * <h3>Arabic normalization rules applied</h3>
 * <ol>
 *   <li>Tatweel / kashida (U+0640) removed — children often hear it in slow speech.</li>
 *   <li>All tashkeel diacritics (U+064B–U+065F) removed.</li>
 *   <li>Superscript alef (U+0670) removed.</li>
 *   <li>All alef variants unified to plain alef (U+0627):
 *       أ (U+0623), إ (U+0625), آ (U+0622), ٱ (U+0671 wasla), ٲ (U+0672), ٳ (U+0673).</li>
 *   <li>Waw with hamza (ؤ U+0624) → waw (و U+0648).</li>
 *   <li>Yaa with hamza (ئ U+0626) → yaa (ي U+064A).</li>
 *   <li>Taa marbuta (ة U+0629) → haa (ه U+0647) — common child reading variant.</li>
 *   <li>Alif maqsura (ى U+0649) → yaa (ي U+064A).</li>
 *   <li>All remaining non-letter/non-digit characters stripped.</li>
 *   <li>Lowercased — covers English words in mixed-language lessons.</li>
 * </ol>
 */
@Service
public class TextComparisonService {

    public ComparisonResult compare(String originalText, String recognizedText) {
        List<String> originalTokens   = tokenize(originalText);
        List<String> recognizedTokens = tokenize(recognizedText);

        List<String> normalizedOriginal   = originalTokens.stream().map(this::normalize).toList();
        List<String> normalizedRecognized = recognizedTokens.stream().map(this::normalize).toList();

        boolean[] recognizedMatched = new boolean[normalizedRecognized.size()];

        List<String> correctWords  = new ArrayList<>();
        List<String> missingWords  = new ArrayList<>();

        for (int i = 0; i < normalizedOriginal.size(); i++) {
            String origNorm = normalizedOriginal.get(i);
            if (origNorm.isEmpty()) continue;

            boolean found = false;
            for (int j = 0; j < normalizedRecognized.size(); j++) {
                if (!recognizedMatched[j] && normalizedRecognized.get(j).equals(origNorm)) {
                    recognizedMatched[j] = true;
                    found = true;
                    break;
                }
            }
            (found ? correctWords : missingWords).add(originalTokens.get(i));
        }

        List<String> incorrectWords = new ArrayList<>();
        for (int j = 0; j < normalizedRecognized.size(); j++) {
            if (!recognizedMatched[j] && !normalizedRecognized.get(j).isEmpty()) {
                incorrectWords.add(recognizedTokens.get(j));
            }
        }

        int totalOriginal = (int) normalizedOriginal.stream().filter(w -> !w.isEmpty()).count();
        int accuracy = totalOriginal == 0
                ? 0
                : (int) Math.round((double) correctWords.size() / totalOriginal * 100.0);

        return new ComparisonResult(
                Math.max(0, Math.min(100, accuracy)),
                totalOriginal,
                Collections.unmodifiableList(correctWords),
                Collections.unmodifiableList(incorrectWords),
                Collections.unmodifiableList(missingWords)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Tokenization
    // ─────────────────────────────────────────────────────────────────────────────

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return List.of();
        // Collapse runs of whitespace first so splitting is clean
        String collapsed = text.trim().replaceAll("\\s+", " ");
        return Arrays.stream(collapsed.split("[\\s\\p{Punct}،؟!.:;،؟]+"))
                .filter(w -> !w.isEmpty())
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Normalization
    // ─────────────────────────────────────────────────────────────────────────────

    String normalize(String word) {
        if (word == null) return "";
        String t = word.trim().toLowerCase();

        // 1. Tatweel (kashida stretching — U+0640)
        t = t.replace("ـ", "");

        // 2. All Arabic tashkeel diacritics: U+064B (fathatan) → U+065F
        t = t.replaceAll("[ً-ٟ]", "");

        // 3. Superscript alef (U+0670)
        t = t.replace("ٰ", "");

        // 4. Unify all alef variants → plain alef (U+0627)
        t = t.replace('أ', 'ا'); // أ  — hamza above
        t = t.replace('إ', 'ا'); // إ  — hamza below
        t = t.replace('آ', 'ا'); // آ  — madda
        t = t.replace('ٱ', 'ا'); // ٱ  — wasla
        t = t.replace('ٲ', 'ا'); // ٲ  — wavy hamza above
        t = t.replace('ٳ', 'ا'); // ٳ  — wavy hamza below

        // 5. Waw with hamza → plain waw
        t = t.replace('ؤ', 'و'); // ؤ → و

        // 6. Yaa with hamza → plain yaa
        t = t.replace('ئ', 'ي'); // ئ → ي

        // 7. Taa marbuta → haa (children routinely drop the feminine marker)
        t = t.replace('ة', 'ه'); // ة → ه

        // 8. Alif maqsura → yaa
        t = t.replace('ى', 'ي'); // ى → ي

        // 9. Strip everything that is not a Unicode letter or digit
        t = t.replaceAll("[^\\p{L}\\p{N}]", "");

        return t;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Result type
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * @param accuracy       0–100 percentage of original words correctly read
     * @param totalWords     number of non-empty words in the original text (denominator)
     * @param correctWords   original words matched in the transcript
     * @param incorrectWords transcript words that had no match in the original
     * @param missingWords   original words absent from the transcript
     */
    public record ComparisonResult(
            int accuracy,
            int totalWords,
            List<String> correctWords,
            List<String> incorrectWords,
            List<String> missingWords
    ) {}
}
