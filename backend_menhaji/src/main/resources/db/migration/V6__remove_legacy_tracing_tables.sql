-- =============================================================================
-- V6 — Remove legacy tracing_questions and tracing_answers tables
--
-- Context: Tracing questions are now handled entirely through the normal
-- Question entity (QuestionType.TRACING). The dedicated tracing_questions
-- table and its companion tracing_answers table are no longer mapped by
-- any JPA entity and are safe to drop.
--
-- Drop order: tracing_answers first (FK to questions), then tracing_questions
-- (FK to questions). Neither table is referenced as a foreign key target by
-- any other table, so no other tables need altering first.
-- =============================================================================

DROP TABLE IF EXISTS `tracing_answers`;
DROP TABLE IF EXISTS `tracing_questions`;
