package com.springboot.manhaji.service;

import com.springboot.manhaji.config.JwtService;
import com.springboot.manhaji.dto.request.LoginRequest;
import com.springboot.manhaji.dto.request.PhoneLoginRequest;
import com.springboot.manhaji.dto.request.RegisterRequest;
import com.springboot.manhaji.dto.response.AuthResponse;
import com.springboot.manhaji.entity.*;
import com.springboot.manhaji.exception.BadRequestException;
import com.springboot.manhaji.exception.ResourceNotFoundException;
import com.springboot.manhaji.exception.UnauthorizedException;
import com.springboot.manhaji.repository.*;
import com.springboot.manhaji.support.Messages;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final AdminRepository adminRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Messages messages;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (request.getEmail() != null &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(
                    messages.get("error.auth.emailAlreadyRegistered"));
        }

        if (request.getPhone() != null &&
                userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException(
                    messages.get("error.auth.phoneAlreadyRegistered"));
        }

        if (request.getEmail() == null && request.getPhone() == null) {
            throw new BadRequestException(
                    messages.get("error.auth.emailOrPhoneRequired"));
        }

        // Step 1: create the auth User record
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setIsActive(true);
        User savedUser = userRepository.save(user);

        // Step 2: create the role-specific profile linked via user_id
        String fullName = request.getFullName();
        Integer gradeLevel = null;
        Long studentId = null;

        switch (request.getRole()) {
            case STUDENT -> {
                Student student = new Student();
                student.setUser(savedUser);
                student.setStudentName(fullName);
                student.setGradeLevel(request.getGradeLevel());
                student.setAvatarId(request.getAvatarId());
                gradeLevel = request.getGradeLevel();
                Student savedStudent = studentRepository.save(student);
                studentId = savedStudent.getId();

                if (request.getParentId() != null) {
                    Parent parent = parentRepository.findByUserId(request.getParentId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Parent", request.getParentId()));
                    ParentStudent link = new ParentStudent();
                    link.setParent(parent);
                    link.setStudent(savedStudent);
                    link.setRelationship("parent");
                    parentStudentRepository.save(link);
                }
            }
            case TEACHER -> {
                Teacher teacher = new Teacher();
                teacher.setUser(savedUser);
                teacher.setTeacherName(fullName);
                teacherRepository.save(teacher);
            }
            case PARENT -> {
                Parent parent = new Parent();
                parent.setUser(savedUser);
                parent.setParentName(fullName);
                parent.setPhone(request.getPhone());
                parentRepository.save(parent);
            }
            case ADMIN -> {
                Admin admin = new Admin();
                admin.setUser(savedUser);
                admin.setAdminName(fullName);
                adminRepository.save(admin);
            }
            default -> throw new BadRequestException(
                    messages.get("error.auth.invalidRole", request.getRole().name()));
        }

        return buildAuthResponse(savedUser, fullName, gradeLevel, request.getAvatarId(), studentId);
    }

    @Transactional
    public AuthResponse loginWithEmail(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException(
                        messages.get("error.auth.invalidEmailCredentials")));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException(
                    messages.get("error.auth.invalidEmailCredentials"));
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildAuthResponseForUser(user.getId());
    }

    @Transactional
    public AuthResponse loginWithPhone(PhoneLoginRequest request) {

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new UnauthorizedException(
                        messages.get("error.auth.invalidPhoneCredentials")));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException(
                    messages.get("error.auth.invalidPhoneCredentials"));
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return buildAuthResponseForUser(user.getId());
    }

    public AuthResponse refreshToken(String refreshToken) {

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new UnauthorizedException(
                    messages.get("error.auth.invalidRefreshToken"));
        }

        String subject = jwtService.extractSubject(refreshToken);

        User user = userRepository.findByEmail(subject)
                .orElseGet(() -> userRepository.findByPhone(subject)
                        .orElseThrow(() -> new UnauthorizedException("User not found")));

        return buildAuthResponseForUser(user.getId());
    }

    public User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    public AuthResponse buildAuthResponseForUser(Long userId) {
        User user = getCurrentUser(userId);

        String fullName = null;
        Integer gradeLevel = null;
        String avatarId = null;
        Long studentId = null;

        switch (user.getRole()) {
            case STUDENT -> {
                var s = studentRepository.findByUserId(userId).orElse(null);
                if (s != null) {
                    fullName = s.getStudentName();
                    gradeLevel = s.getGradeLevel();
                    avatarId = s.getAvatarId();
                    studentId = s.getId();
                }
            }
            case TEACHER ->
                fullName = teacherRepository.findByUserId(userId)
                        .map(Teacher::getTeacherName).orElse(null);
            case PARENT ->
                fullName = parentRepository.findByUserId(userId)
                        .map(Parent::getParentName).orElse(null);
            case ADMIN ->
                fullName = adminRepository.findByUserId(userId)
                        .map(Admin::getAdminName).orElse(null);
            default -> { /* SCHOOL or other roles have no profile entity */ }
        }

        return buildAuthResponse(user, fullName, gradeLevel, avatarId, studentId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(
            User user,
            String fullName,
            Integer gradeLevel,
            String avatarId,
            Long studentId) {

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .userId(user.getId())
                .studentId(studentId)
                .fullName(fullName)
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .gradeLevel(gradeLevel)
                .avatarId(avatarId)
                .build();
    }
}
