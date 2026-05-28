-- ============================================================
-- ONE-TIME migration script — composition architecture
-- Run against your MySQL database BEFORE deploying the app.
-- After running, start the app (Hibernate ddl-auto: update/validate).
-- ============================================================

-- ── Step 1: drop old tables ─────────────────────────────────────────────────
DROP TABLE IF EXISTS tracing_answers;
DROP TABLE IF EXISTS progress_reports;
DROP TABLE IF EXISTS learning_paths;
DROP TABLE IF EXISTS student_responses;
DROP TABLE IF EXISTS attempts;
DROP TABLE IF EXISTS progress;
DROP TABLE IF EXISTS parent_student;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS parents;
DROP TABLE IF EXISTS admins;
DROP TABLE IF EXISTS users;

-- ── Step 2: create users (auth only) ────────────────────────────────────────
CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    email          VARCHAR(255) UNIQUE,
    phone          VARCHAR(50)  UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(50)  NOT NULL,
    is_active      TINYINT(1)   NOT NULL DEFAULT 1,
    last_login_at  DATETIME,
    created_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_user_role (role)
);

-- ── Step 3: role-specific profile tables ────────────────────────────────────

CREATE TABLE students (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL UNIQUE,
    student_name      VARCHAR(255) NOT NULL,
    grade_level       INT,
    avatar_id         VARCHAR(255),
    current_streak    INT          NOT NULL DEFAULT 0,
    total_points      INT          NOT NULL DEFAULT 0,
    current_lesson_id BIGINT,
    school_id         BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_students_users   FOREIGN KEY (user_id)            REFERENCES users   (id) ON DELETE CASCADE,
    CONSTRAINT fk_students_lessons FOREIGN KEY (current_lesson_id)  REFERENCES lessons (id) ON DELETE SET NULL,
    CONSTRAINT fk_students_schools FOREIGN KEY (school_id)          REFERENCES schools (id) ON DELETE SET NULL
);

CREATE TABLE teachers (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL UNIQUE,
    teacher_name   VARCHAR(255) NOT NULL,
    subject        VARCHAR(255),
    specialization VARCHAR(255),
    grade_level    INT,
    school_id      BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_teachers_users   FOREIGN KEY (user_id)   REFERENCES users   (id) ON DELETE CASCADE,
    CONSTRAINT fk_teachers_schools FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE SET NULL
);

CREATE TABLE parents (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL UNIQUE,
    parent_name  VARCHAR(255) NOT NULL,
    phone        VARCHAR(50),
    PRIMARY KEY (id),
    CONSTRAINT fk_parents_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE admins (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL UNIQUE,
    admin_name   VARCHAR(255),
    permissions  VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_admins_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ── Step 4: parent-student relationship join table ───────────────────────────
CREATE TABLE parent_student (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id    BIGINT       NOT NULL,
    student_id   BIGINT       NOT NULL,
    relationship VARCHAR(100),
    PRIMARY KEY (id),
    UNIQUE KEY uq_parent_student (parent_id, student_id),
    CONSTRAINT fk_ps_parent  FOREIGN KEY (parent_id)  REFERENCES parents  (id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE
);

-- ── Step 5: dependent tables that reference students.id ─────────────────────
CREATE TABLE progress (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    student_id        BIGINT       NOT NULL,
    lesson_id         BIGINT       NOT NULL,
    mastery_level     DOUBLE       NOT NULL DEFAULT 0.0,
    completion_status VARCHAR(50)  NOT NULL DEFAULT 'NOT_STARTED',
    last_accessed_at  DATETIME,
    completed_at      DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_progress_student_lesson (student_id, lesson_id),
    INDEX idx_progress_student (student_id),
    INDEX idx_progress_lesson  (lesson_id),
    CONSTRAINT fk_progress_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_lesson  FOREIGN KEY (lesson_id)  REFERENCES lessons  (id) ON DELETE CASCADE
);

CREATE TABLE attempts (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    student_id   BIGINT      NOT NULL,
    quiz_id      BIGINT      NOT NULL,
    status       VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    score        DOUBLE,
    submitted_at DATETIME,
    created_at   DATETIME    NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_attempt_student            (student_id),
    INDEX idx_attempt_quiz               (quiz_id),
    INDEX idx_attempt_student_quiz_status(student_id, quiz_id, status),
    CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_quiz    FOREIGN KEY (quiz_id)    REFERENCES quizzes  (id) ON DELETE CASCADE
);

CREATE TABLE learning_paths (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    student_id      BIGINT NOT NULL UNIQUE,
    recommendations JSON,
    generated_at    DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lp_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE
);

CREATE TABLE progress_reports (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    student_id   BIGINT      NOT NULL,
    period_start DATE        NOT NULL,
    period_end   DATE        NOT NULL,
    summary      TEXT,
    risk_level   VARCHAR(20),
    generated_at DATETIME    NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_report_student (student_id),
    CONSTRAINT fk_report_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE
);
