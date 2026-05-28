package com.springboot.manhaji.service;

import com.springboot.manhaji.dto.response.AdminStatsResponse;
import com.springboot.manhaji.dto.response.LessonSummary;
import com.springboot.manhaji.dto.response.QuestionBankItem;
import com.springboot.manhaji.dto.response.QuestionBankResponse;
import com.springboot.manhaji.dto.response.SubjectSummary;
import com.springboot.manhaji.dto.response.UserSummaryResponse;
import com.springboot.manhaji.entity.Admin;
import com.springboot.manhaji.entity.Parent;
import com.springboot.manhaji.entity.Question;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.entity.Subject;
import com.springboot.manhaji.entity.Teacher;
import com.springboot.manhaji.entity.User;
import com.springboot.manhaji.entity.enums.AttemptStatus;
import com.springboot.manhaji.entity.enums.CompletionStatus;
import com.springboot.manhaji.entity.enums.Role;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.AdminRepository;
import com.springboot.manhaji.repository.AttemptRepository;
import com.springboot.manhaji.repository.LessonRepository;
import com.springboot.manhaji.repository.ParentRepository;
import com.springboot.manhaji.repository.ProgressRepository;
import com.springboot.manhaji.repository.QuestionRepository;
import com.springboot.manhaji.repository.StudentRepository;
import com.springboot.manhaji.repository.SubjectRepository;
import com.springboot.manhaji.repository.TeacherRepository;
import com.springboot.manhaji.repository.UserRepository;
import com.springboot.manhaji.service.support.QuestionBankMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final AdminRepository adminRepository;
    private final SubjectRepository subjectRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final ProgressRepository progressRepository;
    private final QuestionBankMapper questionBankMapper;

    public AdminStatsResponse getStats() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        long activeThisWeek = studentRepository.findAll().stream()
                .filter(s -> s.getUser().getLastLoginAt() != null
                        && s.getUser().getLastLoginAt().isAfter(weekAgo))
                .count();

        long completedAttempts = attemptRepository.findAll().stream()
                .filter(a -> a.getStatus() == AttemptStatus.GRADED)
                .count();

        long completedLessons = progressRepository.findAll().stream()
                .filter(p -> p.getCompletionStatus() == CompletionStatus.COMPLETED)
                .count();

        return AdminStatsResponse.builder()
                .totalStudents(studentRepository.count())
                .totalTeachers(teacherRepository.count())
                .totalParents(parentRepository.count())
                .totalAdmins(adminRepository.count())
                .totalSubjects(subjectRepository.count())
                .totalLessons(lessonRepository.count())
                .totalAttempts(completedAttempts)
                .totalCompletedLessons(completedLessons)
                .activeStudentsThisWeek(activeThisWeek)
                .build();
    }

    public List<UserSummaryResponse> getAllUsers(Role roleFilter) {
        List<User> users = userRepository.findAll();
        return users.stream()
                .filter(u -> roleFilter == null || u.getRole() == roleFilter)
                .map(this::toSummary)
                .toList();
    }

    public List<SubjectSummary> getAllSubjects(Integer gradeFilter) {
        List<Subject> subjects = gradeFilter != null
                ? subjectRepository.findByGradeLevel(gradeFilter)
                : subjectRepository.findAll();
        return subjects.stream()
                .sorted(Comparator
                        .comparing(Subject::getGradeLevel,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Subject::getName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(questionBankMapper::toSubjectSummary)
                .toList();
    }

    public QuestionBankResponse getQuestionsForSubject(
            Long subjectId,
            Integer difficultyLevel,
            Long lessonId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId));

        List<Question> allForSubject = questionRepository.findAllBySubjectIdWithLesson(subjectId);
        List<LessonSummary> lessons = questionBankMapper.collectLessonFilters(allForSubject);

        List<QuestionBankItem> filtered = allForSubject.stream()
                .filter(q -> difficultyLevel == null
                        || difficultyLevel.equals(q.getDifficultyLevel()))
                .filter(q -> lessonId == null
                        || (q.getLesson() != null && lessonId.equals(q.getLesson().getId())))
                .map(questionBankMapper::toQuestionItem)
                .toList();

        return QuestionBankResponse.builder()
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .gradeLevel(subject.getGradeLevel())
                .lessons(lessons)
                .questions(filtered)
                .totalQuestionsInSubject(allForSubject.size())
                .build();
    }

    private UserSummaryResponse toSummary(User user) {
        String fullName = null;
        Integer gradeLevel = null;

        if (user.getRole() == Role.STUDENT) {
            Student s = studentRepository.findByUserId(user.getId()).orElse(null);
            if (s != null) {
                fullName = s.getStudentName();
                gradeLevel = s.getGradeLevel();
            }
        } else if (user.getRole() == Role.TEACHER) {
            fullName = teacherRepository.findByUserId(user.getId())
                    .map(Teacher::getTeacherName).orElse(null);
        } else if (user.getRole() == Role.PARENT) {
            fullName = parentRepository.findByUserId(user.getId())
                    .map(Parent::getParentName).orElse(null);
        } else if (user.getRole() == Role.ADMIN) {
            fullName = adminRepository.findByUserId(user.getId())
                    .map(Admin::getAdminName).orElse(null);
        }

        return UserSummaryResponse.builder()
                .userId(user.getId())
                .fullName(fullName)
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .gradeLevel(gradeLevel)
                .build();
    }
}
