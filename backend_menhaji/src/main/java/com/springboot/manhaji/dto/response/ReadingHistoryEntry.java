package com.springboot.manhaji.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReadingHistoryEntry {

    /** Primary key of the ReadingAssessmentResult row. */
    private Long id;

    /** Lesson this attempt belongs to. */
    private Long lessonId;
    private String lessonTitle;

    /** Snapshot of lesson.content used in this attempt. */
    private String originalText;

    /** Gemini transcript. Null when the speech service was unavailable. */
    private String recognizedText;

    // ─── Scores ────────────────────────────────────────────────────────────────

    /** Word-level accuracy: 0–100. */
    private int accuracy;

    /** Total non-empty words in the original text. */
    private int totalWords;

    /** Number of original words the student read correctly. */
    private int correctWordCount;

    /**
     * Number of extra words the student said that were not in the original.
     * Useful for detecting over-reading or word substitution patterns.
     */
    private int incorrectWordCount;

    /** Number of original words the student skipped. */
    private int missingWordCount;

    // ─── Attempt metadata ──────────────────────────────────────────────────────

    /**
     * AI service that produced the transcript.
     * Known values: "gemini", "whisper", "azure-stt", "google-stt", "unavailable".
     */
    private String transcriptionEngine;

    /** "ar" or "en". */
    private String language;

    // ─── Timestamp ─────────────────────────────────────────────────────────────

    private LocalDateTime createdAt;
}
