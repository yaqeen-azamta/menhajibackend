-- =============================================================================
-- V2 — Create student_skill_profiles
--
-- Tracks per-student, per-lesson, per-sub-skill accuracy and difficulty.
-- Populated by AdaptiveQuizService after each adaptive quiz submission.
-- Used to determine weak skills and target difficulty for future quizzes.
--
-- Unique constraint on (student_id, lesson_id, sub_skill) ensures one profile
-- row per skill per student per lesson; AdaptiveQuizService upserts into it.
-- =============================================================================

CREATE TABLE `student_skill_profiles` (
    `id`                   BIGINT      NOT NULL AUTO_INCREMENT,
    `student_id`           BIGINT      NOT NULL,
    `lesson_id`            BIGINT      NOT NULL,
    `sub_skill`            VARCHAR(64) NOT NULL COMMENT 'e.g. recognition, production, comprehension',
    `total_attempts`       INT         NOT NULL DEFAULT 0,
    `correct_answers`      INT         NOT NULL DEFAULT 0,
    `wrong_answers`        INT         NOT NULL DEFAULT 0,
    `consecutive_correct`  INT         NOT NULL DEFAULT 0,
    `consecutive_wrong`    INT         NOT NULL DEFAULT 0,
    `accuracy`             DOUBLE      NOT NULL DEFAULT 0.0 COMMENT '0.0–100.0 percentage',
    `current_difficulty`   INT         NOT NULL DEFAULT 1   COMMENT '1–5 adaptive level',
    `last_attempt_at`      DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ssp_student_lesson_skill` (`student_id`, `lesson_id`, `sub_skill`),
    KEY `idx_ssp_student`        (`student_id`),
    KEY `idx_ssp_student_lesson` (`student_id`, `lesson_id`),
    CONSTRAINT `fk_ssp_student`
        FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ssp_lesson`
        FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
