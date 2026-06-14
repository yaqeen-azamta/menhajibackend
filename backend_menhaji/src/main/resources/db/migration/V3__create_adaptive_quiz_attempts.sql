-- =============================================================================
-- V3 — Create adaptive_quiz_attempts
--
-- One row per adaptive quiz session. Stores the full Gemini-generated (or DB
-- fallback) question list as JSON so answers can be evaluated without
-- re-calling the AI on submission.
--
-- correctAnswer fields inside generated_questions_json are NEVER sent to the
-- client — the payload builder (AdaptiveQuizPayload) omits them.
--
-- hint_usage_json: JSON object {"questionIndex": hintsUsed, ...}
-- SERIALIZABLE isolation on creation prevents duplicate IN_PROGRESS rows.
--
-- Note: quiz_source column is added in V5. It is omitted here so that V5
-- applies identically to both fresh installs and the existing live database.
-- =============================================================================

CREATE TABLE `adaptive_quiz_attempts` (
    `id`                       BIGINT      NOT NULL AUTO_INCREMENT,
    `student_id`               BIGINT      NOT NULL,
    `lesson_id`                BIGINT      NOT NULL,
    `status`                   ENUM ('GRADED','IN_PROGRESS','SUBMITTED') NOT NULL DEFAULT 'IN_PROGRESS',
    `difficulty_level`         INT         NOT NULL DEFAULT 1,
    `question_count`           INT         NOT NULL DEFAULT 0,
    `correct_count`            INT         NOT NULL DEFAULT 0,
    `score`                    DOUBLE      DEFAULT NULL COMMENT '0–100, set on submission',
    `focus_skills_json`        LONGTEXT    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL
                                   COMMENT 'JSON array of weak sub-skill names targeted in this attempt'
                                   CHECK (JSON_VALID(`focus_skills_json`)),
    `generated_questions_json` LONGTEXT    NOT NULL
                                   COMMENT 'Full List<GeneratedQuestion> including correctAnswer — server-side only',
    `total_hints_used`         INT         NOT NULL DEFAULT 0,
    `hint_usage_json`          LONGTEXT    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '{}' NULL
                                   COMMENT 'Map of questionIndex→hintsUsed for per-question rate limiting'
                                   CHECK (JSON_VALID(`hint_usage_json`)),
    `started_at`               DATETIME(6) NOT NULL,
    `completed_at`             DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_aqa_student`              (`student_id`),
    KEY `idx_aqa_student_lesson_status` (`student_id`, `lesson_id`, `status`),
    CONSTRAINT `fk_aqa_student`
        FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_aqa_lesson`
        FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
