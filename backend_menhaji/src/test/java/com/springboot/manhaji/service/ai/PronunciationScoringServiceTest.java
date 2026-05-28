package com.springboot.manhaji.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the language-dispatch scoring paths.
 *
 * <p>Arabic path was already battle-tested via integration; these tests lock
 * in the Grade-1 English vocabulary behavior so that e.g. "apple" vs. "aple"
 * still awards a passing score — important for the demo because the Gemini
 * transcription often drops a repeated letter.
 */
class PronunciationScoringServiceTest {

    private PronunciationScoringService service;

    @BeforeEach
    void setUp() {
        service = new PronunciationScoringService();
    }

    @Nested
    @DisplayName("Arabic scoring")
    class ArabicScoring {

        @Test
        @DisplayName("exact match → 100")
        void exactMatch() {
            assertThat(service.score("رمان", "رمان", "ar")).isEqualTo(100);
        }

        @Test
        @DisplayName("diacritics normalized away → 100")
        void diacriticsIgnored() {
            assertThat(service.score("رُمّان", "رمان", "ar")).isEqualTo(100);
        }

        @Test
        @DisplayName("hamza variants folded → 100")
        void hamzaFolded() {
            assertThat(service.score("أرض", "ارض", "ar")).isEqualTo(100);
        }

        @Test
        @DisplayName("empty transcription → 0")
        void emptyInput() {
            assertThat(service.score("رمان", "", "ar")).isZero();
            assertThat(service.score("رمان", null, "ar")).isZero();
        }

        @Test
        @DisplayName("2-arg overload defaults to Arabic")
        void defaultLanguageIsArabic() {
            assertThat(service.score("رمان", "رمان")).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("English phonetic scoring")
    class EnglishScoring {

        @Test
        @DisplayName("exact match → 100")
        void exactMatch() {
            assertThat(service.score("apple", "apple", "en")).isEqualTo(100);
        }

        @Test
        @DisplayName("case insensitive")
        void caseInsensitive() {
            assertThat(service.score("Apple", "APPLE", "en")).isEqualTo(100);
        }

        @Test
        @DisplayName("'apple' vs 'aple' collapses to identical phonetic code")
        void collapseDoubleConsonants() {
            // 'apple' → 'apl' (silent e, collapse pp); 'aple' → 'apl' — identical
            assertThat(service.score("apple", "aple", "en")).isEqualTo(100);
        }

        @Test
        @DisplayName("'phone' treats 'ph' as 'f'")
        void phToF() {
            // 'phone' → 'fon'; 'fone' → 'fon'
            assertThat(service.score("phone", "fone", "en")).isEqualTo(100);
        }

        @Test
        @DisplayName("silent 'e' at end drops")
        void silentTrailingE() {
            // 'cake' → 'kak'; 'cak' → 'kak' after c→k and silent e drop
            assertThat(service.score("cake", "cak", "en")).isEqualTo(100);
        }

        @Test
        @DisplayName("'apple' vs 'banana' scores below passing")
        void differentWordsLowScore() {
            int s = service.score("apple", "banana", "en");
            assertThat(s).isLessThan(60);
        }

        @Test
        @DisplayName("punctuation stripped before scoring")
        void punctuationStripped() {
            assertThat(service.score("apple!", "apple.", "en")).isEqualTo(100);
        }

        @Test
        @DisplayName("completely wrong word → passing score denied")
        void wrongWord() {
            int s = service.score("apple", "elephant", "en");
            assertThat(service.isCorrect(s)).isFalse();
        }
    }

    @Nested
    @DisplayName("Star awards")
    class StarAwards {

        @Test
        void threeStarsAt90Plus() {
            assertThat(service.starsForScore(95)).isEqualTo(3);
        }

        @Test
        void twoStarsAt75Plus() {
            assertThat(service.starsForScore(80)).isEqualTo(2);
        }

        @Test
        void oneStarAt60Plus() {
            assertThat(service.starsForScore(65)).isEqualTo(1);
        }

        @Test
        void zeroStarsBelow60() {
            assertThat(service.starsForScore(30)).isZero();
        }
    }
}
