package com.springboot.manhaji.service;

import com.springboot.manhaji.dto.response.AvatarOptionResponse;
import com.springboot.manhaji.dto.response.RecommendedLessonResponse;
import com.springboot.manhaji.dto.response.StudentDashboardResponse;
import com.springboot.manhaji.dto.response.SubjectResponse;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.entity.User;
import com.springboot.manhaji.exception.BadRequestException;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.repository.StudentRepository;
import com.springboot.manhaji.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final LessonService lessonService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Avatar catalog (points-based unlock) ────────────────────────────────

    private record AvatarDef(String id, String emoji, String label, int minPoints) {}

    private static final List<AvatarDef> AVATAR_CATALOG = List.of(
        new AvatarDef("boy",    "🧒", "فتى",    0),
        new AvatarDef("girl",   "👧", "فتاة",   0),
        new AvatarDef("cat",    "🐱", "قطة",    0),
        new AvatarDef("lion",   "🦁", "أسد",    50),
        new AvatarDef("rabbit", "🐰", "أرنب",   50),
        new AvatarDef("owl",    "🦉", "بومة",   100),
        new AvatarDef("rocket", "🚀", "صاروخ",  200),
        new AvatarDef("star",   "🌟", "نجمة",   300),
        new AvatarDef("crown",  "👑", "تاج",    500),
        new AvatarDef("dragon", "🐲", "تنين",   700)
    );

    // ─── Dashboard ────────────────────────────────────────────────────────────

    public StudentDashboardResponse getDashboard(Long userId) {

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student", userId));

        List<SubjectResponse> subjects =
                lessonService.getSubjectsByGrade(
                        student.getGradeLevel(),
                        student.getId()
                );

        RecommendedLessonResponse recommendedLesson =
                lessonService.getRecommendedLesson(
                        student.getGradeLevel(),
                        student.getId()
                );

        return StudentDashboardResponse.builder()
                .studentId(student.getUser().getId())
                .fullName(student.getStudentName())
                .avatarId(student.getAvatarId())
                .gradeLevel(student.getGradeLevel())
                .currentStreak(student.getCurrentStreak())
                .totalPoints(student.getTotalPoints())
                .subjects(subjects)
                .dailyGoal(subjects.size())
                .recommendedLesson(recommendedLesson)
                .build();
    }

    // ─── Avatar ───────────────────────────────────────────────────────────────

    public void updateAvatar(Long userId, String avatarId) {

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student", userId));

        student.setAvatarId(avatarId);

        studentRepository.save(student);
    }

    public List<AvatarOptionResponse> getUnlockedAvatars(Long userId) {

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student", userId));

        int totalPoints = student.getTotalPoints() != null ? student.getTotalPoints() : 0;

        return AVATAR_CATALOG.stream()
                .filter(a -> totalPoints >= a.minPoints())
                .map(a -> AvatarOptionResponse.builder()
                        .id(a.id())
                        .emoji(a.emoji())
                        .label(a.label())
                        .build())
                .toList();
    }

    // ─── Grade ────────────────────────────────────────────────────────────────

    @Transactional
    public void updateGrade(Long userId, int gradeLevel) {

        if (gradeLevel < 1 || gradeLevel > 12) {
            throw new BadRequestException("مستوى الصف يجب أن يكون بين 1 و 12");
        }

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student", userId));

        student.setGradeLevel(gradeLevel);
        studentRepository.save(student);
    }

    // ─── Password ─────────────────────────────────────────────────────────────

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {

        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("يجب أن تكون كلمة المرور 6 أحرف على الأقل");
        }

        // Verify student record exists
        studentRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Student", userId));

        // userId IS the user.id (JWT principal)
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("كلمة المرور الحالية غير صحيحة");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
