package com.springboot.manhaji.service;

import com.springboot.manhaji.dto.request.TeacherQuestionRequest;
import com.springboot.manhaji.dto.response.TeacherQuestionResponse;
import com.springboot.manhaji.entity.Lesson;
import com.springboot.manhaji.entity.Subject;
import com.springboot.manhaji.entity.Teacher;
import com.springboot.manhaji.entity.TeacherQuestion;
import com.springboot.manhaji.entity.enums.QuestionType;
import com.springboot.manhaji.exception.BadRequestException;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.LessonRepository;
import com.springboot.manhaji.repository.SubjectRepository;
import com.springboot.manhaji.repository.TeacherQuestionRepository;
import com.springboot.manhaji.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TeacherQuestionService {

    private final TeacherQuestionRepository teacherQuestionRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public TeacherQuestionResponse createQuestion(Long userId, TeacherQuestionRequest request) {
        Teacher teacher = resolveTeacher(userId);
        Subject subject = resolveSubject(request.getSubjectId());
        Lesson lesson = resolveLesson(request.getLessonId());

        validateLessonBelongsToSubject(lesson, subject);
        validateCorrectAnswer(request);

        TeacherQuestion question = TeacherQuestion.builder()
                .questionText(request.getQuestionText())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .optionC(request.getOptionC())
                .optionD(request.getOptionD())
                .correctAnswer(request.getCorrectAnswer())
                .difficultyLevel(request.getDifficultyLevel())
                .type(request.getType() != null ? request.getType() : QuestionType.MCQ)
                .gradeLevel(subject.getGradeLevel())
                .teacher(teacher)
                .subject(subject)
                .lesson(lesson)
                .build();

        TeacherQuestion saved = teacherQuestionRepository.save(question);
        log.info("Teacher userId={} created TeacherQuestion id={}", userId, saved.getId());
        return toResponse(saved);
    }

    public List<TeacherQuestionResponse> getAllQuestions(Long userId) {
        Teacher teacher = resolveTeacher(userId);
        return teacherQuestionRepository
                .findByTeacherIdOrderByCreatedAtDesc(teacher.getId())
                .stream().map(this::toResponse).toList();
    }

    public List<TeacherQuestionResponse> getQuestionsByGrade(Long userId, Integer gradeLevel) {
        Teacher teacher = resolveTeacher(userId);
        return teacherQuestionRepository
                .findByTeacherIdAndGradeLevelOrderByCreatedAtDesc(teacher.getId(), gradeLevel)
                .stream().map(this::toResponse).toList();
    }

    public List<TeacherQuestionResponse> getQuestionsBySubject(Long userId, Long subjectId) {
        Teacher teacher = resolveTeacher(userId);
        return teacherQuestionRepository
                .findByTeacherIdAndSubjectIdOrderByCreatedAtDesc(teacher.getId(), subjectId)
                .stream().map(this::toResponse).toList();
    }

    public List<TeacherQuestionResponse> getQuestionsByLesson(Long userId, Long lessonId) {
        Teacher teacher = resolveTeacher(userId);
        return teacherQuestionRepository
                .findByTeacherIdAndLessonIdOrderByCreatedAtDesc(teacher.getId(), lessonId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public TeacherQuestionResponse updateQuestion(Long userId, Long questionId, TeacherQuestionRequest request) {
        Teacher teacher = resolveTeacher(userId);
        TeacherQuestion question = teacherQuestionRepository
                .findByIdAndTeacherId(questionId, teacher.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TeacherQuestion", questionId));

        Subject subject = resolveSubject(request.getSubjectId());
        Lesson lesson = resolveLesson(request.getLessonId());

        validateLessonBelongsToSubject(lesson, subject);
        validateCorrectAnswer(request);

        question.setQuestionText(request.getQuestionText());
        question.setOptionA(request.getOptionA());
        question.setOptionB(request.getOptionB());
        question.setOptionC(request.getOptionC());
        question.setOptionD(request.getOptionD());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setDifficultyLevel(request.getDifficultyLevel());
        question.setType(request.getType() != null ? request.getType() : question.getType());
        question.setSubject(subject);
        question.setLesson(lesson);
        question.setGradeLevel(subject.getGradeLevel());

        TeacherQuestion updated = teacherQuestionRepository.save(question);
        log.info("Teacher userId={} updated TeacherQuestion id={}", userId, updated.getId());
        return toResponse(updated);
    }

    @Transactional
    public void deleteQuestion(Long userId, Long questionId) {
        Teacher teacher = resolveTeacher(userId);
        TeacherQuestion question = teacherQuestionRepository
                .findByIdAndTeacherId(questionId, teacher.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TeacherQuestion", questionId));
        teacherQuestionRepository.delete(question);
        log.info("Teacher userId={} deleted TeacherQuestion id={}", userId, questionId);
    }

    // ── private helpers ────────────────────────────────────────────────────────

    // userId from JWT is User.id; resolve to Teacher profile via the OneToOne link.
    private Teacher resolveTeacher(Long userId) {
        return teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", userId));
    }

    private Subject resolveSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));
    }

    private Lesson resolveLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", lessonId));
    }

    private void validateLessonBelongsToSubject(Lesson lesson, Subject subject) {
        if (!lesson.getSubject().getId().equals(subject.getId())) {
            throw new BadRequestException(
                    "Lesson id=" + lesson.getId() + " does not belong to Subject id=" + subject.getId());
        }
    }

    private void validateCorrectAnswer(TeacherQuestionRequest request) {
        Set<String> validOptions = new LinkedHashSet<>();
        validOptions.add(request.getOptionA());
        validOptions.add(request.getOptionB());
        if (request.getOptionC() != null && !request.getOptionC().isBlank()) {
            validOptions.add(request.getOptionC());
        }
        if (request.getOptionD() != null && !request.getOptionD().isBlank()) {
            validOptions.add(request.getOptionD());
        }
        boolean valid = validOptions.stream()
                .anyMatch(opt -> opt.equalsIgnoreCase(request.getCorrectAnswer().trim()));
        if (!valid) {
            throw new BadRequestException(
                    "correctAnswer must exactly match one of the provided options (A, B, C, or D)");
        }
    }

    private TeacherQuestionResponse toResponse(TeacherQuestion q) {
        return TeacherQuestionResponse.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .optionA(q.getOptionA())
                .optionB(q.getOptionB())
                .optionC(q.getOptionC())
                .optionD(q.getOptionD())
                .correctAnswer(q.getCorrectAnswer())
                .difficultyLevel(q.getDifficultyLevel())
                .type(q.getType().name())
                .gradeLevel(q.getGradeLevel())
                .subjectId(q.getSubject().getId())
                .subjectName(q.getSubject().getName())
                .lessonId(q.getLesson().getId())
                .lessonTitle(q.getLesson().getTitle())
                .teacherId(q.getTeacher().getId())
                .teacherName(q.getTeacher().getTeacherName())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }
}
