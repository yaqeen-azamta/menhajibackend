package com.springboot.manhaji.controller;

import com.springboot.manhaji.dto.response.ApiResponse;
import com.springboot.manhaji.dto.response.LessonResponse;
import com.springboot.manhaji.dto.response.LessonSummaryResponse;
import com.springboot.manhaji.dto.response.SubjectResponse;
import com.springboot.manhaji.entity.Student;
import com.springboot.manhaji.service.LessonService;
import com.springboot.manhaji.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService  lessonService;
    private final StudentService studentService;

    /**
     * Returns subjects for a grade level, annotated with this student's completion status.
     * STUDENT: studentId param is optional — resolved from JWT automatically.
     * PARENT/ADMIN: pass ?studentId=<students.id> of the target student.
     */
    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getSubjects(
            @RequestParam Integer gradeLevel,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long resolvedStudentId = studentService.resolveStudent(authentication, studentId).getId();
        return ResponseEntity.ok(ApiResponse.success(
                lessonService.getSubjectsByGrade(gradeLevel, resolvedStudentId)));
    }

    /**
     * Returns lessons for a subject, annotated with this student's completion status.
     * STUDENT: studentId param is optional — resolved from JWT automatically.
     * PARENT/ADMIN: pass ?studentId=<students.id> of the target student.
     */
    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<ApiResponse<List<LessonSummaryResponse>>> getLessonsBySubject(
            @PathVariable Long subjectId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long resolvedStudentId = studentService.resolveStudent(authentication, studentId).getId();
        return ResponseEntity.ok(ApiResponse.success(
                lessonService.getLessonsBySubject(subjectId, resolvedStudentId)));
    }

    /**
     * Returns lesson detail and records the student's access.
     * STUDENT: studentId param is optional — resolved from JWT automatically.
     * PARENT/ADMIN: pass ?studentId=<students.id> of the target student.
     */
    @GetMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<LessonResponse>> getLessonDetail(
            @PathVariable Long lessonId,
            @RequestParam(required = false) Long studentId,
            Authentication authentication) {

        Long resolvedStudentId = studentService.resolveStudent(authentication, studentId).getId();
        return ResponseEntity.ok(ApiResponse.success(
                lessonService.getLessonDetail(lessonId, resolvedStudentId)));
    }
}
