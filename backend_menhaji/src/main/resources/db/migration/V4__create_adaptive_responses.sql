-- =============================================================================
-- V4 — Create adaptive_responses
--
-- One row per question per adaptive quiz attempt. Written at submission time
-- by AdaptiveQuizService.submitAdaptiveQuiz(). Used for:
--   • Per-question feedback returned in AdaptiveQuizResult
--   • Teacher analytics: which questions students answered wrong
--   • Future: question-level difficulty calibration
--
-- question_type stores the raw string from GeneratedQuestion.type so the
-- column survives new question types without a schema change.
-- =============================================================================

CREATE TABLE `adaptive_responses` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT,
    `attempt_id`     BIGINT      NOT NULL,
    `question_index` INT         NOT NULL COMMENT '0-based position in the attempt question list',
    `question_text`  TEXT        NOT NULL,
    `question_type`  VARCHAR(32) NOT NULL COMMENT 'MCQ | TRUE_FALSE | SHORT_ANSWER | etc.',
    `sub_skill`      VARCHAR(64) DEFAULT NULL,
    `student_answer` TEXT        DEFAULT NULL,
    `correct_answer` TEXT        DEFAULT NULL,
    `is_correct`     BIT(1)      DEFAULT NULL,
    `feedback`       TEXT        DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_ar_attempt` (`attempt_id`),
    CONSTRAINT `fk_ar_attempt`
        FOREIGN KEY (`attempt_id`) REFERENCES `adaptive_quiz_attempts` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
