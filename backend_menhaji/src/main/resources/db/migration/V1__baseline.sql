-- =============================================================================
-- V1 — Baseline: full schema snapshot taken when Flyway was introduced.
--
-- On an EXISTING database (created by ddl-auto:update before Flyway):
--   This script is NEVER executed. spring.flyway.baseline-on-migrate=true
--   marks V1 as applied in flyway_schema_history without running it.
--
-- On a FRESH database (new developer environment or new deployment):
--   This script runs first and creates the complete pre-Flyway schema.
--   V2–V5 then apply on top.
--
-- Encoding: utf8mb4 / utf8mb4_general_ci (matches live DB)
-- Engine:   InnoDB
-- =============================================================================

-- ── 1. No-dependency tables ───────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS `schools` (
    `id`      BIGINT       NOT NULL AUTO_INCREMENT,
    `address` VARCHAR(255) DEFAULT NULL,
    `name`    VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `skills` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `description` TEXT   DEFAULT NULL,
    `name`        VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `users` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `email`         VARCHAR(255) DEFAULT NULL,
    `phone`         VARCHAR(255) DEFAULT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `role`          VARCHAR(50)  NOT NULL,
    `is_active`     TINYINT(1)   NOT NULL DEFAULT 1,
    `last_login_at` DATETIME     DEFAULT NULL,
    `created_at`    DATETIME     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `email` (`email`),
    UNIQUE KEY `phone` (`phone`),
    KEY `idx_user_role` (`role`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `subjects` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `grade_level` INT          NOT NULL,
    `name`        VARCHAR(255) NOT NULL,
    `cover_image` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- ── 2. First-level dependents ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS `lessons` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `audio_url`       VARCHAR(255) DEFAULT NULL,
    `content`         TEXT         DEFAULT NULL,
    `grade_level`     INT          NOT NULL,
    `image_urls`      LONGTEXT     CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL
                          CHECK (JSON_VALID(`image_urls`)),
    `objectives`      TEXT         DEFAULT NULL,
    `order_index`     INT          NOT NULL,
    `semester_number` INT          NOT NULL,
    `style_narration` VARCHAR(255) DEFAULT NULL,
    `title`           VARCHAR(255) NOT NULL,
    `subject_id`      BIGINT       NOT NULL,
    `audio_text_hash` VARCHAR(64)  DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `FKe94a0k21xpi7glv89af90lwjv` (`subject_id`),
    CONSTRAINT `FKe94a0k21xpi7glv89af90lwjv`
        FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `admins` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `admin_name`  VARCHAR(255) DEFAULT NULL,
    `permissions` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_id` (`user_id`),
    CONSTRAINT `fk_admins_users`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `teachers` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL,
    `teacher_name`    VARCHAR(255) NOT NULL,
    `subject`         VARCHAR(255) DEFAULT NULL,
    `specialization`  VARCHAR(255) DEFAULT NULL,
    `grade_level`     INT          DEFAULT NULL,
    `school_id`       BIGINT       DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_id` (`user_id`),
    KEY `fk_teachers_schools` (`school_id`),
    CONSTRAINT `fk_teachers_users`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_teachers_schools`
        FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `parents` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `parent_name` VARCHAR(255) NOT NULL,
    `phone`       VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_id` (`user_id`),
    CONSTRAINT `fk_parents_users`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `subscriptions` (
    `id`                BIGINT NOT NULL AUTO_INCREMENT,
    `end_date`          DATE   NOT NULL,
    `start_date`        DATE   NOT NULL,
    `status`            ENUM ('ACTIVE','CANCELLED','EXPIRED','TRIAL') NOT NULL,
    `subscription_type` ENUM ('MONTHLY','SEMESTER','YEARLY') NOT NULL,
    `school_id`         BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK7kow9vaej22ebjq9mr783i9hs` (`school_id`),
    CONSTRAINT `FK7kow9vaej22ebjq9mr783i9hs`
        FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- ── 3. Second-level dependents ────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS `students` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT       NOT NULL,
    `student_name`      VARCHAR(255) NOT NULL,
    `grade_level`       INT          DEFAULT NULL,
    `avatar_id`         VARCHAR(255) DEFAULT NULL,
    `current_streak`    INT          NOT NULL DEFAULT 0,
    `total_points`      INT          NOT NULL DEFAULT 0,
    `current_lesson_id` BIGINT       DEFAULT NULL,
    `school_id`         BIGINT       DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `user_id` (`user_id`),
    KEY `fk_students_lessons` (`current_lesson_id`),
    KEY `fk_students_schools` (`school_id`),
    CONSTRAINT `fk_students_users`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_students_lessons`
        FOREIGN KEY (`current_lesson_id`) REFERENCES `lessons` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_students_schools`
        FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `quizzes` (
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT,
    `created_at`             DATETIME(6)  NOT NULL,
    `gamified`               BIT(1)       NOT NULL,
    `generated_from_lesson`  BIT(1)       NOT NULL,
    `title`                  VARCHAR(255) NOT NULL,
    `lesson_id`              BIGINT       NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKbdv8uggpsin6pnkx0d80ryqey` (`lesson_id`),
    CONSTRAINT `FKbdv8uggpsin6pnkx0d80ryqey`
        FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `questions` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `audio_url`       VARCHAR(512) DEFAULT NULL,
    `correct_answer`  VARCHAR(255) NOT NULL,
    `difficulty_level` INT         NOT NULL,
    `image_url`       VARCHAR(512) DEFAULT NULL,
    `options`         LONGTEXT     CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL
                          CHECK (JSON_VALID(`options`)),
    `question_text`   TEXT         NOT NULL,
    `sub_skill`       VARCHAR(32)  DEFAULT NULL,
    `type`            ENUM ('TRUE_FALSE','MCQ','SHORT_ANSWER','WRITE_ANSWER',
                           'IMAGE_MCQ','TRACING','READING') DEFAULT NULL,
    `lesson_id`       BIGINT       NOT NULL,
    `audio_text_hash` VARCHAR(64)  DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `FKmoaj9c9k8ncsywujtcaujs6rt` (`lesson_id`),
    CONSTRAINT `FKmoaj9c9k8ncsywujtcaujs6rt`
        FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `reading_assessment_results` (
    `id`                       BIGINT      NOT NULL AUTO_INCREMENT,
    `accuracy`                 INT         NOT NULL,
    `correct_word_count`       INT         NOT NULL,
    `correct_words_json`       LONGTEXT    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL
                                   CHECK (JSON_VALID(`correct_words_json`)),
    `created_at`               DATETIME(6) NOT NULL,
    `incorrect_word_count`     INT         NOT NULL,
    `incorrect_words_json`     LONGTEXT    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL
                                   CHECK (JSON_VALID(`incorrect_words_json`)),
    `language`                 VARCHAR(10) NOT NULL,
    `missing_word_count`       INT         NOT NULL,
    `missing_words_json`       LONGTEXT    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL
                                   CHECK (JSON_VALID(`missing_words_json`)),
    `original_text`            TEXT        NOT NULL,
    `pronunciation_detail_json` LONGTEXT   DEFAULT NULL,
    `pronunciation_score`      DOUBLE      DEFAULT NULL,
    `recognized_text`          TEXT        DEFAULT NULL,
    `student_id`               BIGINT      NOT NULL,
    `total_words`              INT         NOT NULL,
    `transcription_engine`     VARCHAR(50) NOT NULL,
    `lesson_id`                BIGINT      NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_rar_student` (`student_id`),
    KEY `idx_rar_lesson` (`lesson_id`),
    KEY `idx_rar_student_lesson` (`student_id`, `lesson_id`),
    KEY `idx_rar_student_created` (`student_id`, `created_at`),
    CONSTRAINT `FKqem7kw46bn4ng2u0fg3oio0ai`
        FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `tracing_questions` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT,
    `question_id`          BIGINT       NOT NULL,
    `character_type`       ENUM ('LETTER_AR','LETTER_LOWERCASE_EN',
                                 'LETTER_UPPERCASE_EN','NUMBER') NOT NULL,
    `display_name`         VARCHAR(120) DEFAULT NULL,
    `guide_image_url`      VARCHAR(512) DEFAULT NULL,
    `created_at`           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `language`             ENUM ('ARABIC','ENGLISH','NUMBERS') NOT NULL,
    `expected_accuracy`    DOUBLE       NOT NULL,
    `expected_points_json` LONGTEXT     CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL
                               CHECK (JSON_VALID(`expected_points_json`)),
    `stroke_count`         INT          NOT NULL,
    `stroke_order_json`    LONGTEXT     CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL
                               CHECK (JSON_VALID(`stroke_order_json`)),
    `svg_path`             TEXT         NOT NULL,
    `tolerance_percentage` DOUBLE       NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK1yo2mb7dvdajewo9gfkfe2a5v` (`question_id`),
    CONSTRAINT `FK1yo2mb7dvdajewo9gfkfe2a5v`
        FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- ── 4. Third-level dependents ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS `attempts` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT,
    `student_id`   BIGINT      NOT NULL,
    `quiz_id`      BIGINT      NOT NULL,
    `status`       VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    `score`        DOUBLE      DEFAULT NULL,
    `submitted_at` DATETIME    DEFAULT NULL,
    `created_at`   DATETIME    NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_attempt_student` (`student_id`),
    KEY `idx_attempt_quiz` (`quiz_id`),
    KEY `idx_attempt_student_quiz_status` (`student_id`, `quiz_id`, `status`),
    CONSTRAINT `fk_attempt_student`
        FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_attempt_quiz`
        FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `quiz_questions` (
    `quiz_id`     BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    KEY `FKanfmgf6ksbdnv7ojb0pfve54q` (`quiz_id`),
    KEY `FKev41c723fx659v28pjycox15o` (`question_id`),
    CONSTRAINT `FKanfmgf6ksbdnv7ojb0pfve54q`
        FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`),
    CONSTRAINT `FKev41c723fx659v28pjycox15o`
        FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `progress` (
    `id`                BIGINT      NOT NULL AUTO_INCREMENT,
    `student_id`        BIGINT      NOT NULL,
    `lesson_id`         BIGINT      NOT NULL,
    `mastery_level`     DOUBLE      NOT NULL DEFAULT 0,
    `completion_status` VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    `last_accessed_at`  DATETIME    DEFAULT NULL,
    `completed_at`      DATETIME    DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_progress_student_lesson` (`student_id`, `lesson_id`),
    KEY `idx_progress_student` (`student_id`),
    KEY `idx_progress_lesson` (`lesson_id`),
    CONSTRAINT `fk_progress_student`
        FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_progress_lesson`
        FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `progress_reports` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT,
    `student_id`   BIGINT      NOT NULL,
    `period_start` DATE        NOT NULL,
    `period_end`   DATE        NOT NULL,
    `summary`      TEXT        DEFAULT NULL,
    `risk_level`   VARCHAR(20) DEFAULT NULL,
    `generated_at` DATETIME    NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_report_student` (`student_id`),
    CONSTRAINT `fk_report_student`
        FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `learning_paths` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT,
    `student_id`      BIGINT   NOT NULL,
    `recommendations` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL
                          CHECK (JSON_VALID(`recommendations`)),
    `generated_at`    DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `student_id` (`student_id`),
    CONSTRAINT `fk_lp_student`
        FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `student_question_answers` (
    `id`              BIGINT     NOT NULL AUTO_INCREMENT,
    `student_id`      BIGINT     NOT NULL,
    `question_id`     BIGINT     NOT NULL,
    `lesson_id`       BIGINT     DEFAULT NULL,
    `quiz_id`         BIGINT     DEFAULT NULL,
    `answer_text`     TEXT       DEFAULT NULL,
    `selected_option` TEXT       DEFAULT NULL,
    `audio_url`       VARCHAR(500) DEFAULT NULL,
    `is_correct`      BIT(1)     NOT NULL,
    `score`           DOUBLE     DEFAULT 0,
    `attempt_number`  INT        DEFAULT 1,
    `answered_at`     TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `feedback`        TEXT       DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_sqa_student_lesson` (`student_id`, `lesson_id`),
    KEY `idx_sqa_student_question` (`student_id`, `question_id`),
    KEY `FKcdrse8qqb2140ixootkd58rth` (`lesson_id`),
    CONSTRAINT `FKcdrse8qqb2140ixootkd58rth`
        FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `tracing_answers` (
    `id`                  BIGINT      NOT NULL AUTO_INCREMENT,
    `attempt_number`      INT         NOT NULL,
    `client_accuracy`     DOUBLE      DEFAULT NULL,
    `drawing_points_json` LONGTEXT    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL
                              CHECK (JSON_VALID(`drawing_points_json`)),
    `feedback`            TEXT        DEFAULT NULL,
    `final_accuracy`      DOUBLE      NOT NULL,
    `is_correct`          BIT(1)      NOT NULL,
    `score`               INT         NOT NULL,
    `server_accuracy`     DOUBLE      DEFAULT NULL,
    `stars`               INT         NOT NULL,
    `student_id`          BIGINT      NOT NULL,
    `submitted_at`        DATETIME(6) NOT NULL,
    `question_id`         BIGINT      NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tracing_answer_student` (`student_id`),
    KEY `idx_tracing_answer_question` (`question_id`),
    KEY `idx_tracing_answer_student_question` (`student_id`, `question_id`),
    CONSTRAINT `FKghxq7hc32tbkm82uyuw6abgns`
        FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `teacher_questions` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `correct_answer`   VARCHAR(255) NOT NULL,
    `created_at`       DATETIME(6)  NOT NULL,
    `difficulty_level` INT          NOT NULL,
    `grade_level`      INT          NOT NULL,
    `optiona`          VARCHAR(255) NOT NULL,
    `optionb`          VARCHAR(255) NOT NULL,
    `optionc`          VARCHAR(255) DEFAULT NULL,
    `optiond`          VARCHAR(255) DEFAULT NULL,
    `question_text`    TEXT         NOT NULL,
    `type`             ENUM ('FILL_BLANK','IMAGE_MCQ','MCQ','ORDERING','PRONUNCIATION',
                            'READING','SHORT_ANSWER','TRACING','TRUE_FALSE') NOT NULL,
    `updated_at`       DATETIME(6)  NOT NULL,
    `lesson_id`        BIGINT       NOT NULL,
    `subject_id`       BIGINT       NOT NULL,
    `teacher_id`       BIGINT       NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tq_teacher` (`teacher_id`),
    KEY `idx_tq_lesson` (`lesson_id`),
    KEY `idx_tq_teacher_grade` (`teacher_id`, `grade_level`),
    KEY `FK5ybeo9gi6uqr5tt79e3166wqx` (`subject_id`),
    CONSTRAINT `FKmlkm8lpqm615rkmaw4sgoen2f`
        FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`),
    CONSTRAINT `FKlcj6e09xjv7b1wmoy435ydqdy`
        FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`),
    CONSTRAINT `FK5ybeo9gi6uqr5tt79e3166wqx`
        FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `parent_student` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `parent_id`    BIGINT       NOT NULL,
    `student_id`   BIGINT       NOT NULL,
    `relationship` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_parent_student` (`parent_id`, `student_id`),
    KEY `fk_ps_student` (`student_id`),
    CONSTRAINT `fk_ps_parent`
        FOREIGN KEY (`parent_id`) REFERENCES `parents` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ps_student`
        FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- ── 5. Fourth-level dependents ────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS `student_responses` (
    `id`             BIGINT NOT NULL AUTO_INCREMENT,
    `audio_ref`      VARCHAR(255) DEFAULT NULL,
    `evaluated_text` TEXT  DEFAULT NULL,
    `feedback`       TEXT  DEFAULT NULL,
    `is_correct`     BIT(1)       DEFAULT NULL,
    `spoken_text`    TEXT  DEFAULT NULL,
    `attempt_id`     BIGINT       NOT NULL,
    `question_id`    BIGINT       NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FK49wqhbw2rceed039sby832yvv` (`attempt_id`),
    KEY `FK5sstgutiayg4h10omdyn06ksk` (`question_id`),
    CONSTRAINT `FK49wqhbw2rceed039sby832yvv`
        FOREIGN KEY (`attempt_id`) REFERENCES `attempts` (`id`),
    CONSTRAINT `FK5sstgutiayg4h10omdyn06ksk`
        FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- ── 6. Legacy tables (not mapped to JPA entities; preserved from old system) ──

CREATE TABLE IF NOT EXISTS `activities` (
    `id`             INT         NOT NULL AUTO_INCREMENT,
    `lesson_id`      INT         DEFAULT NULL,
    `type`           VARCHAR(50) DEFAULT NULL,
    `content`        TEXT        DEFAULT NULL,
    `correct_answer` TEXT        DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `lesson_id` (`lesson_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `activity_attempts` (
    `id`          INT       NOT NULL AUTO_INCREMENT,
    `student_id`  INT       DEFAULT NULL,
    `activity_id` INT       DEFAULT NULL,
    `answer`      TEXT      DEFAULT NULL,
    `is_correct`  TINYINT(1) DEFAULT NULL,
    `created_at`  DATETIME  DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `student_id` (`student_id`),
    KEY `activity_id` (`activity_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `answer_attempts` (
    `attempt_id`     INT   NOT NULL AUTO_INCREMENT,
    `student_id`     INT   DEFAULT NULL,
    `question_id`    INT   DEFAULT NULL,
    `answer_text`    TEXT  DEFAULT NULL,
    `audio_url`      VARCHAR(255) DEFAULT NULL,
    `drawing_data`   TEXT  DEFAULT NULL,
    `position_data`  TEXT  DEFAULT NULL,
    `is_correct`     TINYINT(1)   DEFAULT NULL,
    `response_time`  FLOAT        DEFAULT NULL,
    `created_at`     DATETIME     DEFAULT NULL,
    PRIMARY KEY (`attempt_id`),
    KEY `student_id` (`student_id`),
    KEY `question_id` (`question_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `books` (
    `book_id`     INT          NOT NULL AUTO_INCREMENT,
    `subject`     VARCHAR(50)  NOT NULL,
    `grade`       INT          NOT NULL,
    `semester`    INT          NOT NULL,
    `book_name`   VARCHAR(200) NOT NULL,
    `year`        INT          DEFAULT NULL,
    `is_active`   TINYINT(1)   DEFAULT 1,
    `cover_image` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (`book_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `question_types` (
    `type_id`   INT         NOT NULL AUTO_INCREMENT,
    `type_name` VARCHAR(50) DEFAULT NULL,
    PRIMARY KEY (`type_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `quiz_attempts` (
    `id`              INT      NOT NULL AUTO_INCREMENT,
    `student_id`      INT      DEFAULT NULL,
    `lesson_id`       INT      DEFAULT NULL,
    `score`           INT      DEFAULT NULL,
    `total_questions` INT      DEFAULT NULL,
    `started_at`      DATETIME DEFAULT NULL,
    `finished_at`     DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `student_id` (`student_id`),
    KEY `lesson_id` (`lesson_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `quiz_results` (
    `result_id`       INT      NOT NULL AUTO_INCREMENT,
    `student_id`      INT      NOT NULL,
    `lesson_id`       INT      NOT NULL,
    `score`           INT      NOT NULL,
    `total_questions` INT      NOT NULL,
    `taken_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`result_id`),
    KEY `student_id` (`student_id`),
    KEY `lesson_id` (`lesson_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `student_progress` (
    `id`              INT         NOT NULL AUTO_INCREMENT,
    `student_id`      INT         DEFAULT NULL,
    `lesson_id`       INT         DEFAULT NULL,
    `correct_answers` INT         DEFAULT 0,
    `wrong_answers`   INT         DEFAULT 0,
    `level`           VARCHAR(20) DEFAULT NULL,
    `last_activity`   DATETIME    DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `student_id` (`student_id`),
    KEY `lesson_id` (`lesson_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `units` (
    `unit_id`    INT          NOT NULL AUTO_INCREMENT,
    `book_id`    INT          NOT NULL,
    `unit_title` VARCHAR(200) NOT NULL,
    `unit_order` INT          DEFAULT NULL,
    PRIMARY KEY (`unit_id`),
    KEY `book_id` (`book_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
