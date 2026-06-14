package com.springboot.manhaji.service;

import com.springboot.manhaji.dto.response.AvatarOptionResponse;
import com.springboot.manhaji.dto.response.RecommendedLessonResponse;
import com.springboot.manhaji.dto.response.StudentDashboardResponse;
import com.springboot.manhaji.dto.response.SubjectResponse;
import com.springboot.manhaji.entity.Parent;
import com.springboot.manhaji.entity.ParentStudent;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.entity.User;
import com.springboot.manhaji.exception.BadRequestException;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.exception.UnauthorizedException;
import com.springboot.manhaji.repository.ParentRepository;
import com.springboot.manhaji.repository.ParentStudentRepository;
import com.springboot.manhaji.repository.StudentRepository;
import com.springboot.manhaji.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository       studentRepository;
    private final LessonService           lessonService;
    private final UserRepository          userRepository;
    private final ParentRepository        parentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final PasswordEncoder         passwordEncoder;

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

    /**
     * Returns the dashboard for the resolved student.
     *
     * STUDENT accounts: {@code childStudentId} is ignored — the student is
     * resolved from the JWT's {@code userId} via {@code findByUserId}.
     *
     * PARENT accounts: {@code childStudentId} (student.id of the child) is
     * required. The parent–child link is verified before returning data.
     */
    public StudentDashboardResponse getDashboard(Authentication authentication, Long childStudentId) {
        Long userId = (Long) authentication.getPrincipal();
        Student student = resolveStudent(authentication, childStudentId);

        log.info("getDashboard: authenticatedUserId={}, requestedStudentId={}, resolvedStudentId={}, resolvedUserid={}",
                userId, childStudentId, student.getId(), student.getUser().getId());

        List<SubjectResponse> subjects =
                lessonService.getSubjectsByGrade(student.getGradeLevel(), student.getId());

        RecommendedLessonResponse recommendedLesson =
                lessonService.getRecommendedLesson(student.getGradeLevel(), student.getId());

        return StudentDashboardResponse.builder()
                .studentId(student.getId())
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

    /**
     * Updates the avatar for the authenticated student.
     * Only the student themselves can change their own avatar.
     */
    public void updateAvatar(Long userId, String avatarId) {

        Student student = requireDirectStudent(userId, "updateAvatar");

        log.info("updateAvatar: userId={}, student.id={}", userId, student.getId());
        student.setAvatarId(avatarId);
        studentRepository.save(student);
    }

    /**
     * Returns unlocked avatars.
     * Supports parent accounts: pass child's student.id as {@code childStudentId}.
     */
    public List<AvatarOptionResponse> getUnlockedAvatars(Authentication authentication, Long childStudentId) {
        Long userId = (Long) authentication.getPrincipal();
        Student student = resolveStudent(authentication, childStudentId);

        log.info("getUnlockedAvatars: authenticatedUserId={}, requestedStudentId={}, resolvedStudentId={}",
                userId, childStudentId, student.getId());

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

    /**
     * Updates the grade level for the authenticated student.
     * Only the student themselves can change their own grade.
     */
    @Transactional
    public void updateGrade(Long userId, int gradeLevel) {

        if (gradeLevel < 1 || gradeLevel > 12) {
            throw new BadRequestException("مستوى الصف يجب أن يكون بين 1 و 12");
        }

        Student student = requireDirectStudent(userId, "updateGrade");

        log.info("updateGrade: userId={}, student.id={}, newGrade={}", userId, student.getId(), gradeLevel);
        student.setGradeLevel(gradeLevel);
        studentRepository.save(student);
    }

    // ─── Password ─────────────────────────────────────────────────────────────

    /**
     * Changes the password for the authenticated user.
     * Only the account owner (student) can change their own password.
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {

        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("يجب أن تكون كلمة المرور 6 أحرف على الأقل");
        }

        // Confirm this is a student account (not a parent acting on a child)
        requireDirectStudent(userId, "changePassword");

        // userId IS the user.id (JWT principal)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        log.info("changePassword: userId={}", userId);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("كلمة المرور الحالية غير صحيحة");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ─── Resolution helpers ───────────────────────────────────────────────────

    /**
     * Canonical student resolver — used by every controller and service that
     * needs to identify which student the authenticated caller is acting on.
     *
     * STUDENT: resolves to the authenticated student; {@code requestedStudentId} is ignored.
     * PARENT:  verifies the parent–child link for {@code requestedStudentId}.
     *          Auto-resolves to the only child when {@code requestedStudentId} is null
     *          and the parent has exactly one linked child.
     * ADMIN:   returns the student by {@code requestedStudentId} with no ownership check.
     *          {@code requestedStudentId} is required for ADMIN callers.
     */
    public Student resolveStudent(Authentication authentication, Long requestedStudentId) {
        Long userId = (Long) authentication.getPrincipal();

        if (hasRole(authentication, "ROLE_ADMIN")) {
            if (requestedStudentId == null)
                throw new BadRequestException("يرجى تحديد معرّف الطالب");
            return studentRepository.findById(requestedStudentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", requestedStudentId));
        }

        if (hasRole(authentication, "ROLE_PARENT")) {
            Parent parent = parentRepository.findByUserId(userId)
                    .orElseThrow(() -> {
                        log.warn("resolveStudent: no Parent found for userId={}", userId);
                        return new ResourceNotFoundException("Parent", userId);
                    });

            if (requestedStudentId != null) {
                if (!parentStudentRepository.existsByParentIdAndStudentId(parent.getId(), requestedStudentId)) {
                    log.warn("resolveStudent: parent.id={} is not linked to student.id={}",
                            parent.getId(), requestedStudentId);
                    throw new UnauthorizedException("الطالب غير مرتبط بهذا الحساب");
                }
                Student child = studentRepository.findById(requestedStudentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Student", requestedStudentId));
                log.debug("resolveStudent: parent path — userId={}, parent.id={}, student.id={}",
                        userId, parent.getId(), child.getId());
                return child;
            }

            // Auto-resolve when parent has exactly one child
            List<ParentStudent> links = parentStudentRepository.findByParentId(parent.getId());
            if (links.isEmpty()) throw new ResourceNotFoundException("Student", userId);
            if (links.size() == 1) {
                Student child = links.get(0).getStudent();
                log.debug("resolveStudent: auto-resolved single child — parent.id={}, student.id={}",
                        parent.getId(), child.getId());
                return child;
            }
            throw new BadRequestException(
                    "يرجى تحديد معرّف الطالب باستخدام المعامل studentId (لديك أكثر من طالب مرتبط)");
        }

        // STUDENT (or any unrecognised role — fail-safe to direct lookup)
        Student s = studentRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("resolveStudent: no Student found for userId={}", userId);
                    return new ResourceNotFoundException("Student", userId);
                });
        log.debug("resolveStudent: direct student match — userId={}, student.id={}", userId, s.getId());
        return s;
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    /**
     * Ensures the caller is a direct student (not a parent acting on a child).
     * Used to guard mutation endpoints that must only be performed by the
     * student's own account.
     */
    private Student requireDirectStudent(Long userId, String operation) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("{}: no direct student for userId={} — parent accounts cannot perform this action",
                            operation, userId);
                    return new UnauthorizedException("هذه العملية متاحة للطلاب فقط");
                });
    }
}
