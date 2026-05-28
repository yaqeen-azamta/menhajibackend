# Menhaji — منهجي

An intelligent Arabic-language learning platform for K-12 students, teachers, and parents.

---

## Project Structure

```
menhajifullcode/
│
├── backend_menhaji/          # Spring Boot REST API (Java 17)
│   ├── src/
│   │   └── main/
│   │       ├── java/com/springboot/manhaji/
│   │       │   ├── config/       # Security, JWT, app configuration
│   │       │   ├── controller/   # REST endpoints (Auth, Student, Teacher, Parent, Admin …)
│   │       │   ├── dto/          # Request / Response data transfer objects
│   │       │   ├── entity/       # JPA entities (User, Student, Teacher, Parent …)
│   │       │   ├── exception/    # Global exception handling
│   │       │   ├── repository/   # Spring Data JPA repositories
│   │       │   ├── service/      # Business logic layer
│   │       │   └── support/      # Utilities (Messages, i18n)
│   │       └── resources/
│   │           ├── application.yaml
│   │           └── messages/     # Arabic i18n message bundles
│   ├── migration.sql             # One-time DB setup script
│   ├── build.gradle.kts          # Gradle Kotlin DSL build file
│   └── gradlew                   # Gradle wrapper
│
├── frontend/                 # React + TypeScript Teacher Portal (Vite)
│   ├── src/
│   │   ├── api/              # Axios client + service calls
│   │   ├── components/       # Shared UI components (shadcn/ui + Radix)
│   │   ├── pages/            # Route-level page components
│   │   │   ├── auth/         # Login
│   │   │   ├── dashboard/    # Teacher dashboard
│   │   │   ├── students/     # Student list + profile view
│   │   │   ├── lessons/      # Lesson & subject browser
│   │   │   ├── questions/    # Question bank
│   │   │   └── analytics/    # Analytics view
│   │   ├── store/            # Zustand auth store
│   │   └── types/            # Shared TypeScript types
│   ├── index.html
│   ├── vite.config.ts        # Vite config (dev proxy → :8080)
│   └── package.json
│
└── README.md
```

---

## Tech Stack

| Layer     | Technology                                          |
|-----------|-----------------------------------------------------|
| Backend   | Java 17, Spring Boot 3, Spring Security, JWT, JPA  |
| Database  | MySQL 8                                             |
| Frontend  | React 18, TypeScript, Vite, Tailwind CSS, shadcn/ui |
| AI        | Google Gemini (progress reports, learning paths)    |
| Speech    | OpenAI Whisper (pronunciation scoring)              |

---

## Prerequisites

| Tool       | Minimum Version |
|------------|-----------------|
| Java       | 17              |
| MySQL      | 8.0             |
| Node.js    | 18              |
| npm        | 9               |

---

## Running the Backend

### 1. Set up the database

```bash
# Start MySQL and create the schema
mysql -u root -p -e "CREATE DATABASE manhaji CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Run the one-time migration (drops & recreates role tables)
mysql -u root -p manhaji < backend_menhaji/migration.sql
```

### 2. Configure environment

Create or edit `backend_menhaji/src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/manhaji?useSSL=false&serverTimezone=UTC
    username: root
    password: YOUR_PASSWORD

app:
  jwt:
    secret: YOUR_BASE64_SECRET_KEY   # 256-bit Base64-encoded key
    access-token-expiration: 86400000   # 24 hours (ms)
    refresh-token-expiration: 604800000 # 7 days (ms)
```

### 3. Start the server

```bash
cd backend_menhaji
./gradlew bootRun
```

The API will be available at `http://localhost:8080`.

---

## Running the Frontend

### 1. Install dependencies

```bash
cd frontend
npm install
```

### 2. Start the development server

```bash
npm run dev
```

The teacher portal opens at `http://localhost:3000`. API calls to `/api/*` are proxied to the backend at `http://localhost:8080`.

### 3. Build for production

```bash
npm run build
# Output goes to frontend/dist/
```

---

## API Overview

All endpoints are prefixed with `/api`.

| Method | Endpoint                         | Description                  | Auth     |
|--------|----------------------------------|------------------------------|----------|
| POST   | `/api/auth/register`             | Register any role            | Public   |
| POST   | `/api/auth/login`                | Login with email + password  | Public   |
| POST   | `/api/auth/login/phone`          | Login with phone + password  | Public   |
| POST   | `/api/auth/refresh`              | Refresh JWT                  | Public   |
| GET    | `/api/auth/me`                   | Get current user profile     | JWT      |
| GET    | `/api/student/dashboard`         | Student dashboard            | STUDENT  |
| PUT    | `/api/student/avatar`            | Update avatar                | STUDENT  |
| GET    | `/api/teacher/dashboard`         | Teacher dashboard            | TEACHER  |
| GET    | `/api/teacher/students`          | List class students          | TEACHER  |
| GET    | `/api/parent/dashboard`          | Parent dashboard             | PARENT   |
| GET    | `/api/progress/summary`          | Progress summary             | STUDENT  |
| POST   | `/api/progress/lesson/{id}/complete` | Mark lesson complete     | STUDENT  |
| GET    | `/api/progress/leaderboard`      | Grade leaderboard            | STUDENT  |
| GET    | `/api/quiz/lesson/{lessonId}`    | Get quiz for a lesson        | STUDENT  |
| POST   | `/api/tracing/submit`            | Submit tracing answer        | STUDENT  |
| GET    | `/api/admin/stats`               | Platform statistics          | ADMIN    |

---

## User Roles

| Role    | Description                                      |
|---------|--------------------------------------------------|
| STUDENT | Learner — accesses lessons, quizzes, tracing     |
| TEACHER | Educator — views class progress, question bank   |
| PARENT  | Guardian — monitors children's progress          |
| ADMIN   | Platform manager — views stats, manages users    |

---

## Database Schema (Composition Architecture)

```
users              — authentication only (email, phone, password, role)
 ├── students      — student profile (studentName, gradeLevel, avatarId, points …)
 ├── teachers      — teacher profile (teacherName, subject, assignedGrade …)
 ├── parents       — parent profile  (parentName, phone)
 └── admins        — admin profile   (adminName, permissions)

parent_student     — parent ↔ student relationship (with relationship type)
progress           — per-student lesson completion records
attempts           — quiz attempt records
learning_paths     — AI-generated personalized paths
progress_reports   — AI-generated progress summaries
```

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "Add your feature"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## License

This project is developed as part of a university Software Engineering course.
