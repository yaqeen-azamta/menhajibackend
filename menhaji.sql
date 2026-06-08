-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 08, 2026 at 01:39 PM
-- Server version: 10.4.28-MariaDB
-- PHP Version: 8.2.4

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `menhaji`
--

-- --------------------------------------------------------

--
-- Table structure for table `activities`
--

CREATE TABLE `activities` (
  `id` int(11) NOT NULL,
  `lesson_id` int(11) DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  `content` text DEFAULT NULL,
  `correct_answer` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `activity_attempts`
--

CREATE TABLE `activity_attempts` (
  `id` int(11) NOT NULL,
  `student_id` int(11) DEFAULT NULL,
  `activity_id` int(11) DEFAULT NULL,
  `answer` text DEFAULT NULL,
  `is_correct` tinyint(1) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `admins`
--

CREATE TABLE `admins` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `admin_name` varchar(255) DEFAULT NULL,
  `permissions` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `answer_attempts`
--

CREATE TABLE `answer_attempts` (
  `attempt_id` int(11) NOT NULL,
  `student_id` int(11) DEFAULT NULL,
  `question_id` int(11) DEFAULT NULL,
  `answer_text` text DEFAULT NULL,
  `audio_url` varchar(255) DEFAULT NULL,
  `drawing_data` text DEFAULT NULL,
  `position_data` text DEFAULT NULL,
  `is_correct` tinyint(1) DEFAULT NULL,
  `response_time` float DEFAULT NULL,
  `created_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `attempts`
--

CREATE TABLE `attempts` (
  `id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `quiz_id` bigint(20) NOT NULL,
  `status` varchar(50) NOT NULL DEFAULT 'IN_PROGRESS',
  `score` double DEFAULT NULL,
  `submitted_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `books`
--

CREATE TABLE `books` (
  `book_id` int(11) NOT NULL,
  `subject` varchar(50) NOT NULL,
  `grade` int(11) NOT NULL,
  `semester` int(11) NOT NULL,
  `book_name` varchar(200) NOT NULL,
  `year` int(11) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `cover_image` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `books`
--

INSERT INTO `books` (`book_id`, `subject`, `grade`, `semester`, `book_name`, `year`, `is_active`, `cover_image`) VALUES
(1, 'English', 1, 1, 'English Palestine 1A', 2025, 1, '/uploads/images/grade1/books/english11.png'),
(2, 'Math', 1, 1, 'Math ', 2026, 1, '/uploads/images/grade1/books/math11.jpeg'),
(3, 'arabic', 1, 1, 'arabic', 2026, 1, '/uploads/images/grade1/books/arabic11.jpg'),
(4, 'Deen', 1, 1, 'deen', 2026, 1, '/uploads/images/grade1/books/deen11.jpg'),
(5, 'national', 1, 1, 'national', 2026, 1, '/uploads/images/grade1/books/tansheaa11.jpg');

-- --------------------------------------------------------

--
-- Table structure for table `learning_paths`
--

CREATE TABLE `learning_paths` (
  `id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `recommendations` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`recommendations`)),
  `generated_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `lessons`
--

CREATE TABLE `lessons` (
  `id` bigint(20) NOT NULL,
  `audio_url` varchar(255) DEFAULT NULL,
  `content` text DEFAULT NULL,
  `grade_level` int(11) NOT NULL,
  `image_urls` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`image_urls`)),
  `objectives` text DEFAULT NULL,
  `order_index` int(11) NOT NULL,
  `semester_number` int(11) NOT NULL,
  `style_narration` varchar(255) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `subject_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `lessons`
--

INSERT INTO `lessons` (`id`, `audio_url`, `content`, `grade_level`, `image_urls`, `objectives`, `order_index`, `semester_number`, `style_narration`, `title`, `subject_id`) VALUES
(1, NULL, 'Learning greetings and introducing yourself.', 1, NULL, NULL, 1, 1, NULL, 'Hello!', 1);

-- --------------------------------------------------------

--
-- Table structure for table `parents`
--

CREATE TABLE `parents` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `parent_name` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `parents`
--

INSERT INTO `parents` (`id`, `user_id`, `parent_name`, `phone`) VALUES
(1, 1, 'roqayya', NULL),
(2, 4, 'yaqeen', NULL),
(3, 7, 'yaqeeeen', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `parent_student`
--

CREATE TABLE `parent_student` (
  `id` bigint(20) NOT NULL,
  `parent_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `relationship` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `parent_student`
--

INSERT INTO `parent_student` (`id`, `parent_id`, `student_id`, `relationship`) VALUES
(1, 1, 1, 'parent'),
(2, 2, 3, 'parent'),
(3, 2, 4, 'parent'),
(4, 3, 5, 'parent');

-- --------------------------------------------------------

--
-- Table structure for table `progress`
--

CREATE TABLE `progress` (
  `id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `lesson_id` bigint(20) NOT NULL,
  `mastery_level` double NOT NULL DEFAULT 0,
  `completion_status` varchar(50) NOT NULL DEFAULT 'NOT_STARTED',
  `last_accessed_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `progress`
--

INSERT INTO `progress` (`id`, `student_id`, `lesson_id`, `mastery_level`, `completion_status`, `last_accessed_at`, `completed_at`) VALUES
(1, 3, 1, 100, 'COMPLETED', '2026-05-29 01:06:06', '2026-05-29 01:06:06'),
(2, 6, 1, 100, 'COMPLETED', '2026-06-07 18:22:27', '2026-06-05 16:16:32');

-- --------------------------------------------------------

--
-- Table structure for table `progress_reports`
--

CREATE TABLE `progress_reports` (
  `id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `summary` text DEFAULT NULL,
  `risk_level` varchar(20) DEFAULT NULL,
  `generated_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `questions`
--

CREATE TABLE `questions` (
  `id` bigint(20) NOT NULL,
  `audio_url` varchar(512) DEFAULT NULL,
  `correct_answer` varchar(255) NOT NULL,
  `difficulty_level` int(11) NOT NULL,
  `image_url` varchar(512) DEFAULT NULL,
  `options` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`options`)),
  `question_text` text NOT NULL,
  `sub_skill` varchar(32) DEFAULT NULL,
  `type` enum('TRUE_FALSE','MCQ','SHORT_ANSWER','WRITE_ANSWER','IMAGE_MCQ','TRACING','READING') DEFAULT NULL,
  `lesson_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `questions`
--

INSERT INTO `questions` (`id`, `audio_url`, `correct_answer`, `difficulty_level`, `image_url`, `options`, `question_text`, `sub_skill`, `type`, `lesson_id`) VALUES
(105, NULL, 'M320 120 L320 760', 1, '/uploads/images/grade1/english/number1.png', NULL, 'Trace the number 1', 'writing', 'TRACING', 1),
(111, NULL, 'تفاحة', 1, NULL, NULL, 'اقرأ الكلمة التالية بصوت واضح: تفاحة', 'Pronunciation', 'READING', 1);

-- --------------------------------------------------------

--
-- Table structure for table `question_types`
--

CREATE TABLE `question_types` (
  `type_id` int(11) NOT NULL,
  `type_name` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `quizzes`
--

CREATE TABLE `quizzes` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `gamified` bit(1) NOT NULL,
  `generated_from_lesson` bit(1) NOT NULL,
  `title` varchar(255) NOT NULL,
  `lesson_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `quiz_attempts`
--

CREATE TABLE `quiz_attempts` (
  `id` int(11) NOT NULL,
  `student_id` int(11) DEFAULT NULL,
  `lesson_id` int(11) DEFAULT NULL,
  `score` int(11) DEFAULT NULL,
  `total_questions` int(11) DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `quiz_questions`
--

CREATE TABLE `quiz_questions` (
  `quiz_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `quiz_results`
--

CREATE TABLE `quiz_results` (
  `result_id` int(11) NOT NULL,
  `student_id` int(11) NOT NULL,
  `lesson_id` int(11) NOT NULL,
  `score` int(11) NOT NULL,
  `total_questions` int(11) NOT NULL,
  `taken_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `reading_assessment_results`
--

CREATE TABLE `reading_assessment_results` (
  `id` bigint(20) NOT NULL,
  `accuracy` int(11) NOT NULL,
  `correct_word_count` int(11) NOT NULL,
  `correct_words_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`correct_words_json`)),
  `created_at` datetime(6) NOT NULL,
  `incorrect_word_count` int(11) NOT NULL,
  `incorrect_words_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`incorrect_words_json`)),
  `language` varchar(10) NOT NULL,
  `missing_word_count` int(11) NOT NULL,
  `missing_words_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`missing_words_json`)),
  `original_text` text NOT NULL,
  `pronunciation_detail_json` longtext DEFAULT NULL,
  `pronunciation_score` double DEFAULT NULL,
  `recognized_text` text DEFAULT NULL,
  `student_id` bigint(20) NOT NULL,
  `total_words` int(11) NOT NULL,
  `transcription_engine` varchar(50) NOT NULL,
  `lesson_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `reading_assessment_results`
--

INSERT INTO `reading_assessment_results` (`id`, `accuracy`, `correct_word_count`, `correct_words_json`, `created_at`, `incorrect_word_count`, `incorrect_words_json`, `language`, `missing_word_count`, `missing_words_json`, `original_text`, `pronunciation_detail_json`, `pronunciation_score`, `recognized_text`, `student_id`, `total_words`, `transcription_engine`, `lesson_id`) VALUES
(1, 0, 0, '[]', '2026-06-07 17:28:42.000000', 0, '[]', 'ar', 5, '[\"Learning\",\"greetings\",\"and\",\"introducing\",\"yourself\"]', 'Learning greetings and introducing yourself.', NULL, NULL, NULL, 9, 5, 'unavailable', 1),
(2, 0, 0, '[]', '2026-06-07 17:48:13.000000', 0, '[]', 'ar', 5, '[\"Learning\",\"greetings\",\"and\",\"introducing\",\"yourself\"]', 'Learning greetings and introducing yourself.', NULL, NULL, NULL, 9, 5, 'unavailable', 1),
(3, 0, 0, '[]', '2026-06-07 18:09:19.000000', 0, '[]', 'ar', 1, '[\"تفاحة\"]', 'تفاحة', NULL, NULL, NULL, 9, 1, 'unavailable', 1),
(4, 0, 0, '[]', '2026-06-07 18:22:22.000000', 0, '[]', 'ar', 1, '[\"تفاحة\"]', 'تفاحة', NULL, NULL, NULL, 9, 1, 'unavailable', 1);

-- --------------------------------------------------------

--
-- Table structure for table `schools`
--

CREATE TABLE `schools` (
  `id` bigint(20) NOT NULL,
  `address` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `schools`
--

INSERT INTO `schools` (`id`, `address`, `name`) VALUES
(1, 'Ramallah', 'KidLearn School');

-- --------------------------------------------------------

--
-- Table structure for table `skills`
--

CREATE TABLE `skills` (
  `id` bigint(20) NOT NULL,
  `description` text DEFAULT NULL,
  `name` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `students`
--

CREATE TABLE `students` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `student_name` varchar(255) NOT NULL,
  `grade_level` int(11) DEFAULT NULL,
  `avatar_id` varchar(255) DEFAULT NULL,
  `current_streak` int(11) NOT NULL DEFAULT 0,
  `total_points` int(11) NOT NULL DEFAULT 0,
  `current_lesson_id` bigint(20) DEFAULT NULL,
  `school_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `students`
--

INSERT INTO `students` (`id`, `user_id`, `student_name`, `grade_level`, `avatar_id`, `current_streak`, `total_points`, `current_lesson_id`, `school_id`) VALUES
(1, 2, 'يقين', 1, 'frog', 0, 0, NULL, NULL),
(2, 3, 'مصطفى', 1, 'lion', 0, 0, NULL, NULL),
(3, 5, 'karmel', 1, 'dog', 0, 0, NULL, NULL),
(4, 6, 'dareen', 1, 'koala', 0, 0, NULL, NULL),
(5, 8, 'محمد', 1, 'lion', 0, 0, NULL, NULL),
(6, 9, 'رقية', 1, 'koala', 0, 0, NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `student_progress`
--

CREATE TABLE `student_progress` (
  `id` int(11) NOT NULL,
  `student_id` int(11) DEFAULT NULL,
  `lesson_id` int(11) DEFAULT NULL,
  `correct_answers` int(11) DEFAULT 0,
  `wrong_answers` int(11) DEFAULT 0,
  `level` varchar(20) DEFAULT NULL,
  `last_activity` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `student_question_answers`
--

CREATE TABLE `student_question_answers` (
  `id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `lesson_id` bigint(20) DEFAULT NULL,
  `quiz_id` bigint(20) DEFAULT NULL,
  `answer_text` text DEFAULT NULL,
  `selected_option` text DEFAULT NULL,
  `audio_url` varchar(500) DEFAULT NULL,
  `is_correct` int(11) DEFAULT NULL,
  `score` double DEFAULT 0,
  `attempt_number` int(11) DEFAULT 1,
  `answered_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `student_question_answers`
--

INSERT INTO `student_question_answers` (`id`, `student_id`, `question_id`, `lesson_id`, `quiz_id`, `answer_text`, `selected_option`, `audio_url`, `is_correct`, `score`, `attempt_number`, `answered_at`) VALUES
(1, 3, 12, 1, NULL, NULL, NULL, NULL, 1, 5, 1, '2026-05-22 20:27:13'),
(2, 3, 105, 1, NULL, NULL, NULL, NULL, 1, 5, 1, '2026-05-22 20:27:20'),
(3, 12, 12, 1, NULL, NULL, NULL, NULL, 1, 5, 1, '2026-05-23 10:06:01'),
(4, 12, 105, 1, NULL, NULL, NULL, NULL, 1, 5, 1, '2026-05-23 10:06:22'),
(5, 5, 12, 1, NULL, NULL, NULL, NULL, 1, 5, 1, '2026-05-28 22:06:00'),
(6, 5, 105, 1, NULL, NULL, NULL, NULL, 1, 5, 1, '2026-05-28 22:06:06'),
(7, 9, 12, 1, NULL, NULL, NULL, NULL, 1, 5, 1, '2026-06-06 14:18:37'),
(8, 9, 105, 1, NULL, NULL, NULL, NULL, 1, 5, 1, '2026-06-07 15:22:15'),
(9, 9, 109, 1, NULL, NULL, NULL, NULL, 0, 0, 1, '2026-06-06 14:18:49');

-- --------------------------------------------------------

--
-- Table structure for table `student_responses`
--

CREATE TABLE `student_responses` (
  `id` bigint(20) NOT NULL,
  `audio_ref` varchar(255) DEFAULT NULL,
  `evaluated_text` text DEFAULT NULL,
  `feedback` text DEFAULT NULL,
  `is_correct` bit(1) DEFAULT NULL,
  `spoken_text` text DEFAULT NULL,
  `attempt_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `subjects`
--

CREATE TABLE `subjects` (
  `id` bigint(20) NOT NULL,
  `grade_level` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `cover_image` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `subjects`
--

INSERT INTO `subjects` (`id`, `grade_level`, `name`, `cover_image`) VALUES
(1, 1, 'English', '/uploads/images/grade1/books/english11.png'),
(3, 1, 'Math', '/uploads/images/grade1/books/math11.jpeg'),
(4, 1, 'arabic', '/uploads/images/grade1/books/arabic11.jpg'),
(5, 1, 'Deen', '/uploads/images/grade1/books/deen11.jpg'),
(6, 1, 'national', '/uploads/images/grade1/books/tansheaa11.jpg');

-- --------------------------------------------------------

--
-- Table structure for table `subscriptions`
--

CREATE TABLE `subscriptions` (
  `id` bigint(20) NOT NULL,
  `end_date` date NOT NULL,
  `start_date` date NOT NULL,
  `status` enum('ACTIVE','CANCELLED','EXPIRED','TRIAL') NOT NULL,
  `subscription_type` enum('MONTHLY','SEMESTER','YEARLY') NOT NULL,
  `school_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `teachers`
--

CREATE TABLE `teachers` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `teacher_name` varchar(255) NOT NULL,
  `subject` varchar(255) DEFAULT NULL,
  `specialization` varchar(255) DEFAULT NULL,
  `grade_level` int(11) DEFAULT NULL,
  `school_id` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tracing_answers`
--

CREATE TABLE `tracing_answers` (
  `id` bigint(20) NOT NULL,
  `attempt_number` int(11) NOT NULL,
  `client_accuracy` double DEFAULT NULL,
  `drawing_points_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`drawing_points_json`)),
  `feedback` text DEFAULT NULL,
  `final_accuracy` double NOT NULL,
  `is_correct` bit(1) NOT NULL,
  `score` int(11) NOT NULL,
  `server_accuracy` double DEFAULT NULL,
  `stars` int(11) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  `question_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `tracing_questions`
--

CREATE TABLE `tracing_questions` (
  `id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `character_type` enum('LETTER_AR','LETTER_LOWERCASE_EN','LETTER_UPPERCASE_EN','NUMBER') NOT NULL,
  `display_name` varchar(120) DEFAULT NULL,
  `guide_image_url` varchar(512) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `language` enum('ARABIC','ENGLISH','NUMBERS') NOT NULL,
  `expected_accuracy` double NOT NULL,
  `expected_points_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL CHECK (json_valid(`expected_points_json`)),
  `stroke_count` int(11) NOT NULL,
  `stroke_order_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL CHECK (json_valid(`stroke_order_json`)),
  `svg_path` text NOT NULL,
  `tolerance_percentage` double NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `tracing_questions`
--

INSERT INTO `tracing_questions` (`id`, `question_id`, `character_type`, `display_name`, `guide_image_url`, `created_at`, `language`, `expected_accuracy`, `expected_points_json`, `stroke_count`, `stroke_order_json`, `svg_path`, `tolerance_percentage`) VALUES
(2, 105, 'NUMBER', 'Trace Number 1', '/uploads/images/grade1/english/number1.png', '2026-05-19 22:20:18', 'ENGLISH', 0, '', 0, NULL, '', 0);

-- --------------------------------------------------------

--
-- Table structure for table `units`
--

CREATE TABLE `units` (
  `unit_id` int(11) NOT NULL,
  `book_id` int(11) NOT NULL,
  `unit_title` varchar(200) NOT NULL,
  `unit_order` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `units`
--

INSERT INTO `units` (`unit_id`, `book_id`, `unit_title`, `unit_order`) VALUES
(1, 1, 'Hello!', 1);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` varchar(50) NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT 1,
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `email`, `phone`, `password_hash`, `role`, `is_active`, `last_login_at`, `created_at`) VALUES
(1, 'roro@gmail.com', NULL, '$2a$10$108SituICAeHc4Jeu2nAVuqyfoWIb.3qgAwxIqkZkPToFtlbpjDo.', 'PARENT', 1, '2026-05-23 17:30:05', '2026-05-23 17:08:59'),
(2, 'yaqeenAzamta@gmail.com', NULL, '$2a$10$mrLZ8Rv4dfBQ2ywN1Q7m9uG9vdIFoxLw4.P3XAMrK6wP8WgYl7uFW', 'STUDENT', 1, NULL, '2026-05-23 17:09:41'),
(3, 'mu@gmail.com', NULL, '$2a$10$23BQTCakrsr53EwJR/ykqO1/rFWnVHfh01Nsq995tlET3Uw5SGEpe', 'STUDENT', 1, '2026-05-23 17:46:01', '2026-05-23 17:34:22'),
(4, 'yaaaa@gmail.com', NULL, '$2a$10$RAA4/RJ0mIzna4n2wBLaGOi4aMD0FylYVwqEYRv4aYFmlxgMPMTDm', 'PARENT', 1, '2026-05-29 02:16:45', '2026-05-29 01:04:15'),
(5, 'ka@gmail.com', NULL, '$2a$10$M7C.2U8ZtfixhB79qWxldOtZ3D1GoKnXZZ2YyxidcXhsYBStUYh8K', 'STUDENT', 1, NULL, '2026-05-29 01:05:49'),
(6, 'ddddddd@gmail.com', NULL, '$2a$10$ceTzkB2Z9HZc6U7UM8a73elBuKOmTqZikGK.lSAx7xFKm2.bZiE..', 'STUDENT', 1, NULL, '2026-05-29 01:47:22'),
(7, 'yyyy@gmail.com', NULL, '$2a$10$bMhKpcjNkMIP/0qAgywlfe6JCIMVaIgskbLeFBD9fMONbtow6wMMa', 'PARENT', 1, NULL, '2026-06-02 21:04:01'),
(8, 'moh@gmail.com', NULL, '$2a$10$uW.0UDzsLNPVL63mMmgZLOStBJC69ADLfqGu94C3hblptKyqgBcem', 'STUDENT', 1, '2026-06-02 21:23:40', '2026-06-02 21:04:30'),
(9, 'ror@gmail.com', NULL, '$2a$10$wyDS5/.jGDTfdF10M/GqUOhDg3nX.ZWeYFeDR7lefjn7KOVRKWpsS', 'STUDENT', 1, '2026-06-07 18:22:04', '2026-06-02 21:24:57');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `activities`
--
ALTER TABLE `activities`
  ADD PRIMARY KEY (`id`),
  ADD KEY `lesson_id` (`lesson_id`);

--
-- Indexes for table `activity_attempts`
--
ALTER TABLE `activity_attempts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `student_id` (`student_id`),
  ADD KEY `activity_id` (`activity_id`);

--
-- Indexes for table `admins`
--
ALTER TABLE `admins`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`);

--
-- Indexes for table `answer_attempts`
--
ALTER TABLE `answer_attempts`
  ADD PRIMARY KEY (`attempt_id`),
  ADD KEY `student_id` (`student_id`),
  ADD KEY `question_id` (`question_id`);

--
-- Indexes for table `attempts`
--
ALTER TABLE `attempts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_attempt_student` (`student_id`),
  ADD KEY `idx_attempt_quiz` (`quiz_id`),
  ADD KEY `idx_attempt_student_quiz_status` (`student_id`,`quiz_id`,`status`);

--
-- Indexes for table `books`
--
ALTER TABLE `books`
  ADD PRIMARY KEY (`book_id`);

--
-- Indexes for table `learning_paths`
--
ALTER TABLE `learning_paths`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `student_id` (`student_id`);

--
-- Indexes for table `lessons`
--
ALTER TABLE `lessons`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKe94a0k21xpi7glv89af90lwjv` (`subject_id`);

--
-- Indexes for table `parents`
--
ALTER TABLE `parents`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`);

--
-- Indexes for table `parent_student`
--
ALTER TABLE `parent_student`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_parent_student` (`parent_id`,`student_id`),
  ADD UNIQUE KEY `UKa75262ayu1e8c972aolwoip4f` (`parent_id`,`student_id`),
  ADD KEY `fk_ps_student` (`student_id`);

--
-- Indexes for table `progress`
--
ALTER TABLE `progress`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_progress_student_lesson` (`student_id`,`lesson_id`),
  ADD UNIQUE KEY `UKjrhj085ru2touom233ic7hogh` (`student_id`,`lesson_id`),
  ADD KEY `idx_progress_student` (`student_id`),
  ADD KEY `idx_progress_lesson` (`lesson_id`);

--
-- Indexes for table `progress_reports`
--
ALTER TABLE `progress_reports`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_report_student` (`student_id`);

--
-- Indexes for table `questions`
--
ALTER TABLE `questions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKmoaj9c9k8ncsywujtcaujs6rt` (`lesson_id`);

--
-- Indexes for table `question_types`
--
ALTER TABLE `question_types`
  ADD PRIMARY KEY (`type_id`);

--
-- Indexes for table `quizzes`
--
ALTER TABLE `quizzes`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKbdv8uggpsin6pnkx0d80ryqey` (`lesson_id`);

--
-- Indexes for table `quiz_attempts`
--
ALTER TABLE `quiz_attempts`
  ADD PRIMARY KEY (`id`),
  ADD KEY `student_id` (`student_id`),
  ADD KEY `lesson_id` (`lesson_id`);

--
-- Indexes for table `quiz_questions`
--
ALTER TABLE `quiz_questions`
  ADD KEY `FKev41c723fx659v28pjycox15o` (`question_id`),
  ADD KEY `FKanfmgf6ksbdnv7ojb0pfve54q` (`quiz_id`);

--
-- Indexes for table `quiz_results`
--
ALTER TABLE `quiz_results`
  ADD PRIMARY KEY (`result_id`),
  ADD KEY `student_id` (`student_id`),
  ADD KEY `lesson_id` (`lesson_id`);

--
-- Indexes for table `reading_assessment_results`
--
ALTER TABLE `reading_assessment_results`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_rar_student` (`student_id`),
  ADD KEY `idx_rar_lesson` (`lesson_id`),
  ADD KEY `idx_rar_student_lesson` (`student_id`,`lesson_id`),
  ADD KEY `idx_rar_student_created` (`student_id`,`created_at`);

--
-- Indexes for table `schools`
--
ALTER TABLE `schools`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `skills`
--
ALTER TABLE `skills`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `students`
--
ALTER TABLE `students`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`),
  ADD KEY `fk_students_lessons` (`current_lesson_id`),
  ADD KEY `fk_students_schools` (`school_id`);

--
-- Indexes for table `student_progress`
--
ALTER TABLE `student_progress`
  ADD PRIMARY KEY (`id`),
  ADD KEY `student_id` (`student_id`),
  ADD KEY `lesson_id` (`lesson_id`);

--
-- Indexes for table `student_question_answers`
--
ALTER TABLE `student_question_answers`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `student_responses`
--
ALTER TABLE `student_responses`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK49wqhbw2rceed039sby832yvv` (`attempt_id`),
  ADD KEY `FK5sstgutiayg4h10omdyn06ksk` (`question_id`);

--
-- Indexes for table `subjects`
--
ALTER TABLE `subjects`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `subscriptions`
--
ALTER TABLE `subscriptions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK7kow9vaej22ebjq9mr783i9hs` (`school_id`);

--
-- Indexes for table `teachers`
--
ALTER TABLE `teachers`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`),
  ADD KEY `fk_teachers_schools` (`school_id`);

--
-- Indexes for table `tracing_answers`
--
ALTER TABLE `tracing_answers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_tracing_answer_student` (`student_id`),
  ADD KEY `idx_tracing_answer_question` (`question_id`),
  ADD KEY `idx_tracing_answer_student_question` (`student_id`,`question_id`);

--
-- Indexes for table `tracing_questions`
--
ALTER TABLE `tracing_questions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK1yo2mb7dvdajewo9gfkfe2a5v` (`question_id`);

--
-- Indexes for table `units`
--
ALTER TABLE `units`
  ADD PRIMARY KEY (`unit_id`),
  ADD KEY `book_id` (`book_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD UNIQUE KEY `phone` (`phone`),
  ADD KEY `idx_user_role` (`role`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `activities`
--
ALTER TABLE `activities`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `activity_attempts`
--
ALTER TABLE `activity_attempts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `admins`
--
ALTER TABLE `admins`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `answer_attempts`
--
ALTER TABLE `answer_attempts`
  MODIFY `attempt_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `attempts`
--
ALTER TABLE `attempts`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `books`
--
ALTER TABLE `books`
  MODIFY `book_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `learning_paths`
--
ALTER TABLE `learning_paths`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `lessons`
--
ALTER TABLE `lessons`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `parents`
--
ALTER TABLE `parents`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `parent_student`
--
ALTER TABLE `parent_student`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `progress`
--
ALTER TABLE `progress`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `progress_reports`
--
ALTER TABLE `progress_reports`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `questions`
--
ALTER TABLE `questions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=112;

--
-- AUTO_INCREMENT for table `question_types`
--
ALTER TABLE `question_types`
  MODIFY `type_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `quizzes`
--
ALTER TABLE `quizzes`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `quiz_attempts`
--
ALTER TABLE `quiz_attempts`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `quiz_results`
--
ALTER TABLE `quiz_results`
  MODIFY `result_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `reading_assessment_results`
--
ALTER TABLE `reading_assessment_results`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `schools`
--
ALTER TABLE `schools`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `skills`
--
ALTER TABLE `skills`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `students`
--
ALTER TABLE `students`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `student_progress`
--
ALTER TABLE `student_progress`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `student_question_answers`
--
ALTER TABLE `student_question_answers`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT for table `student_responses`
--
ALTER TABLE `student_responses`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `subjects`
--
ALTER TABLE `subjects`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `subscriptions`
--
ALTER TABLE `subscriptions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `teachers`
--
ALTER TABLE `teachers`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `tracing_answers`
--
ALTER TABLE `tracing_answers`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `tracing_questions`
--
ALTER TABLE `tracing_questions`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `units`
--
ALTER TABLE `units`
  MODIFY `unit_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `admins`
--
ALTER TABLE `admins`
  ADD CONSTRAINT `fk_admins_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `attempts`
--
ALTER TABLE `attempts`
  ADD CONSTRAINT `fk_attempt_quiz` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_attempt_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `learning_paths`
--
ALTER TABLE `learning_paths`
  ADD CONSTRAINT `fk_lp_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `lessons`
--
ALTER TABLE `lessons`
  ADD CONSTRAINT `FKe94a0k21xpi7glv89af90lwjv` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`);

--
-- Constraints for table `parents`
--
ALTER TABLE `parents`
  ADD CONSTRAINT `fk_parents_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `parent_student`
--
ALTER TABLE `parent_student`
  ADD CONSTRAINT `fk_ps_parent` FOREIGN KEY (`parent_id`) REFERENCES `parents` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_ps_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `progress`
--
ALTER TABLE `progress`
  ADD CONSTRAINT `fk_progress_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_progress_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `progress_reports`
--
ALTER TABLE `progress_reports`
  ADD CONSTRAINT `fk_report_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `questions`
--
ALTER TABLE `questions`
  ADD CONSTRAINT `FKmoaj9c9k8ncsywujtcaujs6rt` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`);

--
-- Constraints for table `quizzes`
--
ALTER TABLE `quizzes`
  ADD CONSTRAINT `FKbdv8uggpsin6pnkx0d80ryqey` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`);

--
-- Constraints for table `quiz_questions`
--
ALTER TABLE `quiz_questions`
  ADD CONSTRAINT `FKanfmgf6ksbdnv7ojb0pfve54q` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`),
  ADD CONSTRAINT `FKev41c723fx659v28pjycox15o` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`);

--
-- Constraints for table `reading_assessment_results`
--
ALTER TABLE `reading_assessment_results`
  ADD CONSTRAINT `FKqem7kw46bn4ng2u0fg3oio0ai` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`);

--
-- Constraints for table `students`
--
ALTER TABLE `students`
  ADD CONSTRAINT `fk_students_lessons` FOREIGN KEY (`current_lesson_id`) REFERENCES `lessons` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_students_schools` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_students_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `student_responses`
--
ALTER TABLE `student_responses`
  ADD CONSTRAINT `FK49wqhbw2rceed039sby832yvv` FOREIGN KEY (`attempt_id`) REFERENCES `attempts` (`id`),
  ADD CONSTRAINT `FK5sstgutiayg4h10omdyn06ksk` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`);

--
-- Constraints for table `subscriptions`
--
ALTER TABLE `subscriptions`
  ADD CONSTRAINT `FK7kow9vaej22ebjq9mr783i9hs` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`);

--
-- Constraints for table `teachers`
--
ALTER TABLE `teachers`
  ADD CONSTRAINT `fk_teachers_schools` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_teachers_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `tracing_answers`
--
ALTER TABLE `tracing_answers`
  ADD CONSTRAINT `FKghxq7hc32tbkm82uyuw6abgns` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`);

--
-- Constraints for table `tracing_questions`
--
ALTER TABLE `tracing_questions`
  ADD CONSTRAINT `FK1yo2mb7dvdajewo9gfkfe2a5v` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
