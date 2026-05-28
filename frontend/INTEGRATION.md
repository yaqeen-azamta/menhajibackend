# Backend Integration Notes

## How the frontend calls your Spring Boot backend

### Login
```
POST /api/auth/login
Body: { "username": "...", "password": "..." }
Expected response: { "token": "...", "userId": 1, "role": "TEACHER", "fullName": "..." }
```

> ⚠️ **Check your actual login endpoint path.** If yours is `/api/auth/signin` or `/api/users/login`,
> update it in `src/api/services.ts` → `authApi.login`.

---

### All teacher endpoints use `Authentication authentication`

Your `TeacherController` reads the teacher ID as:
```java
Long teacherId = (Long) authentication.getPrincipal();
```

This means your **JWT filter must set the principal as the teacher's `Long` ID** when parsing the token.
If it currently sets `username` as the principal, update either:
- The JWT filter to store the user ID, or
- The controller to look up the teacher by username first.

---

### Endpoint mapping

| Frontend calls | Your Spring Boot endpoint |
|----------------|--------------------------|
| `GET /api/teacher/dashboard` | `@GetMapping("/dashboard")` in TeacherController |
| `GET /api/teacher/students` | `@GetMapping("/students")` |
| `GET /api/teacher/students/{id}` | `@GetMapping("/students/{studentId}")` |
| `GET /api/teacher/subjects` | `@GetMapping("/subjects")` |
| `GET /api/teacher/subjects/{id}/questions?difficulty=&lessonId=` | `@GetMapping("/subjects/{subjectId}/questions")` |

All of these already exist in your `TeacherController.java` — **no changes needed**.

---

### CORS

Add this to your Spring Boot `SecurityConfig` or `WebMvcConfig`:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    return new UrlBasedCorsConfigurationSource() {{ registerCorsConfiguration("/**", config); }};
}
```

---

### ApiResponse wrapper

Your backend wraps all responses:
```java
ApiResponse.success(teacherService.getDashboard(teacherId))
// → { "success": true, "data": { ... }, "message": null }
```

The frontend already unwraps `.data.data` for all calls. If your auth login
endpoint does NOT wrap in `ApiResponse`, that's already handled (login reads `res.data` directly).
