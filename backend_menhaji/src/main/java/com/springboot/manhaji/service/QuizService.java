package com.springboot.manhaji.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.manhaji.config.QuizConfigProperties;
import com.springboot.manhaji.dto.question.QuestionOption;
import com.springboot.manhaji.dto.request.ReadingSubmitRequest;
import com.springboot.manhaji.dto.request.SubmitAnswerRequest;
import com.springboot.manhaji.dto.request.TracingSubmitRequest;
import com.springboot.manhaji.dto.response.*;
import com.springboot.manhaji.entity.*;
import com.springboot.manhaji.entity.enums.AttemptStatus;
import com.springboot.manhaji.entity.enums.CompletionStatus;
import com.springboot.manhaji.entity.enums.QuestionType;
import com.springboot.manhaji.exception.BadRequestException;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.*;
import com.springboot.manhaji.service.ai.GeminiService;
import com.springboot.manhaji.service.ai.PronunciationScoringService;
import com.springboot.manhaji.service.ai.WhisperService;
import com.springboot.manhaji.support.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.springboot.manhaji.dto.question.QuestionOption;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final StudentResponseRepository responseRepository;
    private final StudentRepository studentRepository;
    private final ProgressRepository progressRepository;
    private final ObjectMapper objectMapper;
    private final GeminiService geminiService;
    private final WhisperService whisperService;
    private final PronunciationScoringService pronunciationScoringService;
    private final Messages messages;
    private final QuizConfigProperties quizConfig;
    private int getPointsByDifficulty(Integer difficulty) {
    if (difficulty == null) return 5;

    return switch (difficulty) {
        case 1 -> 5;
        case 2 -> 10;
        case 3 -> 15;
        case 4 -> 20;
        case 5 -> 25;
        default -> 5;
    };
}

    public QuizResponse getQuizByLesson(Long lessonId) {
        List<Quiz> quizzes = quizRepository.findByLessonId(lessonId);
        if (quizzes.isEmpty()) {
            throw new ResourceNotFoundException("Quiz", lessonId);
        }
        Quiz quiz = quizzes.get(0);
        return buildQuizResponse(quiz);
    }

    @Transactional
    public AttemptResponse startAttempt(Long quizId, Long userId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", quizId));

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", userId));

        Optional<Attempt> inProgress = attemptRepository.findByStudentIdAndQuizIdAndStatus(
                student.getId(), quizId, AttemptStatus.IN_PROGRESS);
        if (inProgress.isPresent()) {
            return buildAttemptResponse(inProgress.get(), quiz);
        }

        Attempt attempt = new Attempt();
        attempt.setStudent(student);
        attempt.setQuiz(quiz);
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt = attemptRepository.save(attempt);

        return AttemptResponse.builder()
                .attemptId(attempt.getId())
                .quizId(quizId)
                .status("IN_PROGRESS")
                .totalQuestions(quiz.getQuestions().size())
                .correctAnswers(0)
                .pointsEarned(0)
                .answers(new ArrayList<>())
                .build();
    }

    @Transactional
    public SubmitAnswerResponse submitAnswer(Long attemptId, SubmitAnswerRequest request, Long userId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt", attemptId));

        if (!attempt.getStudent().getUser().getId().equals(userId)) {
            throw new BadRequestException(messages.get("error.attempt.notYours"));
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException(messages.get("error.attempt.alreadyCompleted"));
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question", request.getQuestionId()));

        Map<String, Object> aiResult = aiEvaluateIfShortAnswer(question, request);
        boolean isCorrect = evaluateAnswer(question, request, aiResult);
        String feedback = generateFeedback(question, request, isCorrect, aiResult);
        int pointsEarned = isCorrect ? quizConfig.getPointsPerCorrect() : 0;

        StudentResponse response = new StudentResponse();
        response.setAttempt(attempt);
        response.setQuestion(question);
        response.setIsCorrect(isCorrect);
        response.setFeedback(feedback);
        response.setAudioRef(request.getAudioRef());

        if (question.getType() == QuestionType.SHORT_ANSWER) {
            response.setSpokenText(request.getSpokenText());
            response.setEvaluatedText(request.getAnswer());
        } else {
            response.setEvaluatedText(request.getAnswer());
        }

        responseRepository.save(response);

        return SubmitAnswerResponse.builder()
                .questionId(question.getId())
                .isCorrect(isCorrect)
                .feedback(feedback)
                .correctAnswer(question.getCorrectAnswer())
                .pointsEarned(pointsEarned)
                .build();
    }

    @Transactional
    public PronunciationScoreResponse submitPronunciation(
            Long attemptId, Long questionId, byte[] audioBytes, String language, Long userId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt", attemptId));

        if (!attempt.getStudent().getUser().getId().equals(userId)) {
            throw new BadRequestException(messages.get("error.attempt.notYours"));
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException(messages.get("error.attempt.alreadyCompleted"));
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

        String expected = question.getCorrectAnswer();
        String lang = (language != null && !language.isBlank()) ? language : "ar";
        if ("ar".equals(lang) && !containsArabic(expected)) {
            lang = "en";
        }

        if (!whisperService.isAvailable()) {
            log.warn("Pronunciation requested but Whisper/Gemini is not configured — returning fallback response");
            return PronunciationScoreResponse.builder()
                    .questionId(questionId)
                    .expectedText(expected)
                    .transcribedText("")
                    .score(0)
                    .rating(pronunciationScoringService.rating(0))
                    .feedback("خدمة النطق غير متاحة الآن. حاول لاحقاً.")
                    .isCorrect(false)
                    .pointsEarned(0)
                    .build();
        }

        String transcribed = whisperService.transcribe(audioBytes, lang);

        int score = pronunciationScoringService.score(expected, transcribed, lang);
        String rating = pronunciationScoringService.rating(score);
        String feedback = pronunciationScoringService.feedback(score, expected);
        boolean isCorrect = pronunciationScoringService.isCorrect(score);
        int pointsEarned = isCorrect ? quizConfig.getPointsPerCorrect() : 0;

        StudentResponse response = new StudentResponse();
        response.setAttempt(attempt);
        response.setQuestion(question);
        response.setIsCorrect(isCorrect);
        response.setFeedback(feedback);
        response.setSpokenText(transcribed);
        response.setEvaluatedText(transcribed);
        responseRepository.save(response);

        return PronunciationScoreResponse.builder()
                .questionId(questionId)
                .expectedText(expected)
                .transcribedText(transcribed)
                .score(score)
                .rating(rating)
                .feedback(feedback)
                .isCorrect(isCorrect)
                .pointsEarned(pointsEarned)
                .build();
    }

    @Transactional
    public SubmitAnswerResponse submitTracingResult(
            Long attemptId, TracingSubmitRequest request, Long userId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt", attemptId));

        if (!attempt.getStudent().getUser().getId().equals(userId)) {
            throw new BadRequestException(messages.get("error.attempt.notYours"));
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException(messages.get("error.attempt.alreadyCompleted"));
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question", request.getQuestionId()));

        if (question.getType() != QuestionType.TRACING) {
            throw new BadRequestException("Question is not a tracing question");
        }

        boolean isCorrect = Boolean.TRUE.equals(request.getIsCorrect());
        String feedback = request.getFeedback() != null ? request.getFeedback()
                : (isCorrect ? "أحسنت الكتابة!" : "استمر في التدريب");
        int pointsEarned = isCorrect ? quizConfig.getPointsPerCorrect() : 0;

        StudentResponse response = new StudentResponse();
        response.setAttempt(attempt);
        response.setQuestion(question);
        response.setIsCorrect(isCorrect);
        response.setFeedback(feedback);
        response.setEvaluatedText("score=" + request.getScore() + ",stars=" + request.getStars());
        responseRepository.save(response);

        return SubmitAnswerResponse.builder()
                .questionId(question.getId())
                .isCorrect(isCorrect)
                .feedback(feedback)
                .correctAnswer(question.getCorrectAnswer())
                .pointsEarned(pointsEarned)
                .build();
    }

    @Transactional
    public SubmitAnswerResponse submitReadingResult(
            Long attemptId, ReadingSubmitRequest request, Long userId) {

        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt", attemptId));

        if (!attempt.getStudent().getUser().getId().equals(userId)) {
            throw new BadRequestException(messages.get("error.attempt.notYours"));
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException(messages.get("error.attempt.alreadyCompleted"));
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question", request.getQuestionId()));

        if (question.getType() != QuestionType.READING) {
            throw new BadRequestException("Question is not a reading question");
        }

        boolean isCorrect = Boolean.TRUE.equals(request.getIsCorrect());
        int accuracy = request.getAccuracy() != null ? request.getAccuracy() : 0;
        int stars    = request.getStars()    != null ? request.getStars()    : 0;

        String feedback = isCorrect
                ? "قراءة ممتازة! دقة " + accuracy + "%"
                : "استمر في التدريب! دقة " + accuracy + "%";

        int pointsEarned = isCorrect ? quizConfig.getPointsPerCorrect() : 0;

        StudentResponse response = new StudentResponse();
        response.setAttempt(attempt);
        response.setQuestion(question);
        response.setIsCorrect(isCorrect);
        response.setFeedback(feedback);
        response.setEvaluatedText("accuracy=" + accuracy + "%,stars=" + stars);
        responseRepository.save(response);

        return SubmitAnswerResponse.builder()
                .questionId(question.getId())
                .isCorrect(isCorrect)
                .feedback(feedback)
                .correctAnswer("READING")
                .pointsEarned(pointsEarned)
                .build();
    }

    @Transactional
    public AttemptResponse completeAttempt(Long attemptId, Long userId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt", attemptId));

        if (!attempt.getStudent().getUser().getId().equals(userId)) {
            throw new BadRequestException(messages.get("error.attempt.notYours"));
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new BadRequestException(messages.get("error.attempt.alreadyCompleted"));
        }

        Quiz quiz = attempt.getQuiz();
        List<StudentResponse> responses = responseRepository.findByAttemptId(attemptId);

        LinkedHashMap<Long, StudentResponse> latestPerQuestion = new LinkedHashMap<>();
        for (StudentResponse r : responses) {
            latestPerQuestion.put(r.getQuestion().getId(), r);
        }
        Collection<StudentResponse> dedupedResponses = latestPerQuestion.values();

        int totalQuestions = quiz.getQuestions().size();
        int correctAnswers = (int) dedupedResponses.stream().filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();
        double score = totalQuestions > 0 ? (correctAnswers * 100.0) / totalQuestions : 0;
       int pointsEarned = 0;

for (StudentResponse response : dedupedResponses) {
    if (Boolean.TRUE.equals(response.getIsCorrect())) {

        Integer difficulty =
                response.getQuestion().getDifficultyLevel();

        pointsEarned +=
                getPointsByDifficulty(difficulty);
    }
}

        attempt.setStatus(AttemptStatus.GRADED);
        attempt.setScore(score);
        attempt.setSubmittedAt(LocalDateTime.now());
        attemptRepository.save(attempt);

        Student student = attempt.getStudent();
        student.setTotalPoints(student.getTotalPoints() + pointsEarned);
        studentRepository.save(student);

        updateLessonProgress(student, quiz.getLesson(), score);

        List<AnswerFeedback> feedbacks = dedupedResponses.stream().map(r -> AnswerFeedback.builder()
                .questionId(r.getQuestion().getId())
                .questionText(r.getQuestion().getQuestionText())
                .studentAnswer(r.getEvaluatedText())
                .correctAnswer(r.getQuestion().getCorrectAnswer())
                .isCorrect(Boolean.TRUE.equals(r.getIsCorrect()))
                .feedback(r.getFeedback())
                .build()
        ).toList();

        return AttemptResponse.builder()
                .attemptId(attemptId)
                .quizId(quiz.getId())
                .status("GRADED")
                .score(score)
                .totalQuestions(totalQuestions)
                .correctAnswers(correctAnswers)
                .pointsEarned(pointsEarned)
                .submittedAt(attempt.getSubmittedAt())
                .answers(feedbacks)
                .build();
    }

    public Map<String, Object> getHint(Long questionId, int level) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

        int maxLevel = quizConfig.getMaxHintLevel();
        level = Math.max(1, Math.min(maxLevel, level));
        String hint = geminiService.generateHint(
                question.getQuestionText(), question.getCorrectAnswer(), level, "ar");

        return Map.of(
                "hint", hint,
                "hintLevel", level,
                "remainingHints", maxLevel - level
        );
    }

    // --- Helper methods ---

    private Map<String, Object> aiEvaluateIfShortAnswer(Question question, SubmitAnswerRequest request) {
        if (question.getType() != QuestionType.SHORT_ANSWER) return null;
        if (!geminiService.isAvailable()) return null;
        String studentAnswer = (request.getAnswer() != null ? request.getAnswer() :
                                request.getSpokenText() != null ? request.getSpokenText() : "").trim();
        try {
            return geminiService.evaluateShortAnswer(
                    question.getQuestionText(), question.getCorrectAnswer().trim(), studentAnswer, "ar");
        } catch (Exception e) {
            log.warn("Gemini evaluation failed, falling back to string matching: {}", e.getMessage());
            return null;
        }
    }

    private boolean evaluateAnswer(Question question, SubmitAnswerRequest request,
                                    Map<String, Object> aiResult) {
        String correctAnswer = question.getCorrectAnswer().trim();
        String studentAnswer = (request.getAnswer() != null ? request.getAnswer() :
                               request.getSpokenText() != null ? request.getSpokenText() : "").trim();

        // READING questions go through /reading endpoint — never via text evaluation
        if (question.getType() == QuestionType.READING) return false;

        if (question.getType() == QuestionType.MCQ || question.getType() == QuestionType.TRUE_FALSE) {
            return correctAnswer.equalsIgnoreCase(studentAnswer);
        }

        if (question.getType() == QuestionType.FILL_BLANK || question.getType() == QuestionType.ORDERING) {
            String normalizedCorrect = normalizeArabic(correctAnswer);
            String normalizedStudent = normalizeArabic(studentAnswer);
            if (normalizedCorrect.equals(normalizedStudent)) return true;
            if (normalizedStudent.contains(normalizedCorrect) ||
                normalizedCorrect.contains(normalizedStudent)) return true;
            return false;
        }

        if (question.getType() == QuestionType.SHORT_ANSWER) {
            if (aiResult != null && aiResult.get("isCorrect") instanceof Boolean isCorrectResult) {
                return isCorrectResult;
            }

            String normalizedCorrect = normalizeArabic(correctAnswer);
            String normalizedStudent = normalizeArabic(studentAnswer);

            if (normalizedCorrect.equals(normalizedStudent)) return true;
            if (normalizedStudent.contains(normalizedCorrect) ||
                normalizedCorrect.contains(normalizedStudent)) return true;
        }

        return false;
    }

    private String normalizeArabic(String text) {
        if (text == null) return "";
        return text
                .replaceAll("[\\u064B-\\u065F\\u0670]", "")
                .replaceAll("[آأإ]", "ا")
                .replace('ة', 'ه')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private String generateFeedback(Question question, SubmitAnswerRequest request, boolean isCorrect,
                                     Map<String, Object> aiResult) {
        if (question.getType() == QuestionType.SHORT_ANSWER
                && aiResult != null && aiResult.get("feedback") != null) {
            return (String) aiResult.get("feedback");
        }

        if (isCorrect) {
            return "أحسنت! إجابة صحيحة 🌟";
        }
        return "إجابة خاطئة. الإجابة الصحيحة هي: " + question.getCorrectAnswer();
    }

    private void updateLessonProgress(Student student, Lesson lesson, double score) {
        Optional<Progress> existing = progressRepository.findByStudentIdAndLessonId(
                student.getId(), lesson.getId());

        Progress progress;
        if (existing.isPresent()) {
            progress = existing.get();
        } else {
            progress = new Progress();
            progress.setStudent(student);
            progress.setLesson(lesson);
        }

        progress.setMasteryLevel(score);
        progress.setLastAccessedAt(LocalDateTime.now());

        if (score >= quizConfig.getMasteryThreshold()) {
            progress.setCompletionStatus(CompletionStatus.MASTERED);
            progress.setCompletedAt(LocalDateTime.now());
        } else if (score >= quizConfig.getCompletionThreshold()) {
            progress.setCompletionStatus(CompletionStatus.COMPLETED);
            progress.setCompletedAt(LocalDateTime.now());
        } else {
            progress.setCompletionStatus(CompletionStatus.IN_PROGRESS);
        }

        progressRepository.save(progress);
    }

    private QuizResponse buildQuizResponse(Quiz quiz) {
        List<QuestionResponse> questionResponses = quiz.getQuestions().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(this::buildQuestionResponse)
                .toList();

        List<String> lessonImageUrls = parseImageUrls(quiz.getLesson().getImageUrls());

        return QuizResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .gamified(quiz.getGamified())
                .totalQuestions(quiz.getQuestions().size())
                .questions(questionResponses)
                .lessonContent(quiz.getLesson().getContent())
                .lessonObjectives(quiz.getLesson().getObjectives())
                .lessonImageUrls(lessonImageUrls)
                .build();
    }

    private List<String> parseImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(imageUrlsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private QuestionResponse buildQuestionResponse(Question question) {
        List<QuestionOption> options = null;

        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            try {
                options = objectMapper.readValue(
                        question.getOptions(),
                        new TypeReference<List<QuestionOption>>() {});
            } catch (Exception e) {
                options = List.of();
            }
        }

        if (question.getType() == QuestionType.TRUE_FALSE && options == null) {
            options = List.of(
                    new QuestionOption("صح", ""),
                    new QuestionOption("خطأ", "")
            );
        }

        return QuestionResponse.builder()
                .id(question.getId())
                .type(question.getType().name())
                .questionText(question.getQuestionText())
                .options(options)
                .difficultyLevel(question.getDifficultyLevel())
                .subSkill(question.getSubSkill())
                .imageUrl(question.getImageUrl())
                .audioUrl(question.getAudioUrl())
                .build();
    }

    private AttemptResponse buildAttemptResponse(Attempt attempt, Quiz quiz) {
        List<StudentResponse> responses = responseRepository.findByAttemptId(attempt.getId());
        int correctAnswers = (int) responses.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect())).count();

        return AttemptResponse.builder()
                .attemptId(attempt.getId())
                .quizId(quiz.getId())
                .status(attempt.getStatus().name())
                .score(attempt.getScore())
                .totalQuestions(quiz.getQuestions().size())
                .correctAnswers(correctAnswers)
                .pointsEarned(correctAnswers * quizConfig.getPointsPerCorrect())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }

    private boolean containsArabic(String text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0600 && c <= 0x06FF) return true;
        }
        return false;
    }
}
