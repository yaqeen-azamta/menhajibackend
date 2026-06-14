-- =============================================================================
-- Reading Assessment Results — complete DDL
-- =============================================================================
-- Project uses: spring.jpa.hibernate.ddl-auto: update
-- Hibernate will auto-create / auto-alter this table on the first startup
-- after the entity is deployed.
--
-- Run this script manually ONLY for:
--   • production databases where ddl-auto is "validate" or "none"
--   • pre-creating the table before a first deployment
--   • adding columns to an existing reading_assessment_results table
--     (see ALTER TABLE section at the bottom)
--
-- Tested on: MySQL 8.0+ / MariaDB 10.6+
-- Encoding:  utf8mb4 / utf8mb4_unicode_ci  (required for Arabic)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- CREATE (fresh install)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reading_assessment_results
(
    -- ── Identity ────────────────────────────────────────────────────────────
    id                      BIGINT          NOT NULL AUTO_INCREMENT,

    -- ── Relationships ───────────────────────────────────────────────────────
    student_id              BIGINT          NOT NULL
        COMMENT 'Plain FK — no constraint to students table (matches TracingAnswer convention)',

    lesson_id               BIGINT          NOT NULL
        COMMENT 'FK to lessons.id — cascades on delete',

    -- ── Text content ────────────────────────────────────────────────────────
    original_text           TEXT            NOT NULL
        COMMENT 'Snapshot of lesson.content at attempt time — preserved if lesson is later edited',

    recognized_text         TEXT
        COMMENT 'Gemini transcript. NULL when speech service was unavailable',

    -- ── Scalar scores ───────────────────────────────────────────────────────
    accuracy                INT             NOT NULL
        COMMENT 'Word-level accuracy 0–100',

    total_words             INT             NOT NULL DEFAULT 0
        COMMENT 'Non-empty word count in original_text — denominator for accuracy comparisons across lessons',

    correct_word_count      INT             NOT NULL DEFAULT 0
        COMMENT 'Denormalized count of correctly-read words — enables AVG/SUM without JSON parsing',

    incorrect_word_count    INT             NOT NULL DEFAULT 0
        COMMENT 'Denormalized count of extra/substituted words — same reason',

    missing_word_count      INT             NOT NULL DEFAULT 0
        COMMENT 'Denormalized count of skipped original words — same reason',

    -- ── Word lists (JSON arrays) ─────────────────────────────────────────────
    correct_words_json      JSON
        COMMENT 'JSON array of correctly-read words. Example: ["الكلب","يلعب","في"]',

    incorrect_words_json    JSON
        COMMENT 'JSON array of extra/substituted words the student said. Identifies substitution patterns',

    missing_words_json      JSON
        COMMENT 'JSON array of original words the student skipped. Primary source for most-missed-words analytics',

    -- ── Attempt metadata ────────────────────────────────────────────────────
    transcription_engine    VARCHAR(50)     NOT NULL DEFAULT 'gemini'
        COMMENT 'AI service that produced recognized_text. Known values: gemini, whisper, azure-stt, google-stt, unavailable',

    language                VARCHAR(10)     NOT NULL DEFAULT 'ar'
        COMMENT '"ar" or "en" — required for language-specific analytics and Phase 2 pronunciation scoring',

    -- ── Phase 2: pronunciation scoring (reserved — NULL in Phase 1) ─────────
    pronunciation_score     DOUBLE
        COMMENT 'Phase 2: overall pronunciation score 0.0-100.0 from PronunciationAssessmentService. NULL until Phase 2',

    pronunciation_detail_json LONGTEXT
        COMMENT 'Phase 2: per-word phoneme JSON from PronunciationAssessmentService. LONGTEXT because per-phoneme data can be several KB. NULL until Phase 2',

    -- ── Timestamp ───────────────────────────────────────────────────────────
    created_at              DATETIME(6)     NOT NULL
        COMMENT 'Set by @PrePersist — never updated',

    -- ── Constraints ─────────────────────────────────────────────────────────
    PRIMARY KEY (id),

    INDEX idx_rar_student         (student_id)
        COMMENT 'Student history endpoints, total-attempt counts',

    INDEX idx_rar_lesson          (lesson_id)
        COMMENT 'Teacher view: all students on one lesson',

    INDEX idx_rar_student_lesson  (student_id, lesson_id)
        COMMENT 'Student+lesson queries, latest-attempt lookup, lesson-level progress',

    INDEX idx_rar_student_created (student_id, created_at)
        COMMENT 'Time-series progress charts per student (ordered scan within one student)',

    CONSTRAINT fk_rar_lesson
        FOREIGN KEY (lesson_id)
        REFERENCES lessons (id)
        ON DELETE CASCADE

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =============================================================================
-- ALTER TABLE — use these statements when adding columns to an existing table
-- (e.g. upgrading a running deployment that already has the base table)
-- =============================================================================

/*
-- Run each ALTER only if the column does not already exist.

ALTER TABLE reading_assessment_results
    ADD COLUMN IF NOT EXISTS total_words            INT          NOT NULL DEFAULT 0      AFTER accuracy,
    ADD COLUMN IF NOT EXISTS correct_word_count     INT          NOT NULL DEFAULT 0      AFTER total_words,
    ADD COLUMN IF NOT EXISTS incorrect_word_count   INT          NOT NULL DEFAULT 0      AFTER correct_word_count,
    ADD COLUMN IF NOT EXISTS missing_word_count     INT          NOT NULL DEFAULT 0      AFTER incorrect_word_count,
    ADD COLUMN IF NOT EXISTS correct_words_json     JSON                                 AFTER missing_word_count,
    ADD COLUMN IF NOT EXISTS incorrect_words_json   JSON                                 AFTER correct_words_json,
    ADD COLUMN IF NOT EXISTS missing_words_json     JSON                                 AFTER incorrect_words_json,
    ADD COLUMN IF NOT EXISTS transcription_engine   VARCHAR(50)  NOT NULL DEFAULT 'gemini' AFTER missing_words_json,
    ADD COLUMN IF NOT EXISTS language               VARCHAR(10)  NOT NULL DEFAULT 'ar'   AFTER transcription_engine,
    ADD COLUMN IF NOT EXISTS pronunciation_score    DOUBLE                               AFTER language,
    ADD COLUMN IF NOT EXISTS pronunciation_detail_json LONGTEXT                          AFTER pronunciation_score;

-- Add the new index (skip if it already exists)
ALTER TABLE reading_assessment_results
    ADD INDEX idx_rar_student_created (student_id, created_at);
*/


-- =============================================================================
-- Rollback
-- =============================================================================
-- DROP TABLE IF EXISTS reading_assessment_results;
