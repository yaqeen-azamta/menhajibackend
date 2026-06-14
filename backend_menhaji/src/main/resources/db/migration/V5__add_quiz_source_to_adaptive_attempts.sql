-- =============================================================================
-- V5 — Add quiz_source to adaptive_quiz_attempts
--
-- Records whether questions came from Gemini AI generation or the DB fallback
-- (triggered when Gemini fails after one retry).
--
-- Values: 'GEMINI' | 'FALLBACK_DB'
--
-- This column is intentionally absent from V3 so that V5 runs on BOTH:
--   • Fresh databases  (V3 created the table without this column, V5 adds it)
--   • Existing databases (V1–V4 are baselined; V5 is the first migration to run)
-- =============================================================================

-- IF NOT EXISTS (MariaDB 10.0+ / MySQL 8.0.29+):
-- Makes this statement safe to run manually before Flyway has executed it.
-- When Flyway later applies V5, it finds the column already present and skips
-- the ALTER silently, rather than failing with "Duplicate column name".
ALTER TABLE `adaptive_quiz_attempts`
    ADD COLUMN IF NOT EXISTS `quiz_source` VARCHAR(16) NOT NULL DEFAULT 'GEMINI'
        COMMENT '"GEMINI" = AI-generated questions, "FALLBACK_DB" = sourced from question bank'
        AFTER `difficulty_level`;
