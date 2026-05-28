# Manhaji Teacher Portal

A production-grade React + TypeScript teacher web dashboard for the Manhaji educational platform.

---

## 🏗 Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend Framework | React 18 + TypeScript |
| Build Tool | Vite 5 |
| Styling | TailwindCSS + custom design system |
| UI Components | Radix primitives + custom shadcn-style components |
| State Management | Zustand (auth/theme) + TanStack Query (server state) |
| HTTP Client | Axios with JWT interceptors |
| Forms | React Hook Form + Zod validation |
| Charts | Recharts |
| Routing | React Router v6 |

---

## 📁 Folder Structure

```
src/
├── api/
│   ├── client.ts          # Axios instance + JWT interceptors
│   └── services.ts        # All API functions (auth, lessons, questions, students...)
├── components/
│   ├── common/
│   │   └── index.tsx      # Button, Input, Card, Modal, Avatar, Badge, Skeleton, etc.
│   └── layout/
│       ├── AppLayout.tsx  # Sidebar + main layout
│       └── ProtectedRoute.tsx
├── pages/
│   ├── auth/LoginPage.tsx
│   ├── dashboard/DashboardPage.tsx
│   ├── students/
│   │   ├── StudentsPage.tsx
│   │   └── StudentProfilePage.tsx
│   ├── lessons/LessonsPage.tsx
│   ├── questions/QuestionsPage.tsx
│   ├── analytics/AnalyticsPage.tsx
│   └── settings/SettingsPage.tsx
├── store/
│   └── authStore.ts       # Zustand auth store (JWT, user, dark mode)
├── types/
│   └── index.ts           # All TypeScript types/interfaces
└── utils/
    └── index.ts           # Helper functions
```

---

## 🚀 Setup

### 1. Install dependencies
```bash
npm install
```

### 2. Configure environment
```bash
cp .env.example .env
# Edit .env — set VITE_API_URL if needed
```

### 3. Run in development
```bash
npm run dev
# Opens at http://localhost:3000
# API requests to /api/* are proxied to http://localhost:8080
```

### 4. Build for production
```bash
npm run build
# Output: dist/
```

---

## 🔧 Backend Integration

### Proxy (Development)
`vite.config.ts` is preconfigured to proxy `/api` → `http://localhost:8080`. No CORS issues during dev.

### Spring Boot Endpoints Required

The frontend expects these REST endpoints:

#### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signin` | Login — returns `{ token, roles, ... }` |
| POST | `/api/auth/signout` | Logout |

#### Teacher
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/teachers/me` | Teacher profile |
| GET | `/api/teachers/me/stats` | Dashboard stats |
| GET | `/api/teachers/me/grades` | Assigned grades |
| GET | `/api/teachers/me/subjects` | Subjects (optional ?gradeId) |
| GET | `/api/teachers/me/students` | Students (paginated, searchable) |
| GET | `/api/teachers/me/analytics/subjects` | Subject analytics |
| GET | `/api/teachers/me/analytics/quizzes` | Quiz analytics |
| GET | `/api/teachers/me/analytics/rankings` | Student rankings |

#### Students
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/students/{id}` | Student summary |
| GET | `/api/students/{id}/profile` | Full student profile |

#### Lessons
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/subjects/{subjectId}/lessons` | Lessons by subject |
| GET | `/api/lessons/{id}` | Single lesson |
| POST | `/api/lessons` | Create lesson |
| PUT | `/api/lessons/{id}` | Update lesson |
| DELETE | `/api/lessons/{id}` | Delete lesson |
| PUT | `/api/subjects/{subjectId}/lessons/reorder` | Reorder lessons |
| POST | `/api/lessons/{id}/image` | Upload image |
| POST | `/api/lessons/{id}/audio` | Upload audio |

#### Questions
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/lessons/{lessonId}/questions` | Questions by lesson |
| GET | `/api/questions/{id}` | Single question |
| POST | `/api/questions` | Create question |
| PUT | `/api/questions/{id}` | Update question |
| DELETE | `/api/questions/{id}` | Delete question |
| POST | `/api/questions/{id}/image` | Upload image |
| POST | `/api/questions/{id}/audio` | Upload audio |

### Backend Java files included
Inside `manhaji-backend/`:
- `controller/TeacherController.java`
- `controller/StudentController.java`
- `service/TeacherService.java`
- `dto/TeacherDTOs.java`
- `repository/UserRepository.java`
- `repository/StudentProgressRepository.java`

Copy these files into your Spring Boot project at the appropriate package paths. Adjust package names to match your existing project.

---

## 🎨 Design System

### Colors
- **Primary**: Emerald green (`hsl(158 64% 40%)`)
- **Sidebar**: Dark slate (`hsl(222 47% 11%)`)
- **Dark mode**: Fully supported via CSS variables

### Typography
- **English**: Sora (Google Fonts)
- **Arabic**: Cairo (Google Fonts, RTL-ready)
- **Mono**: JetBrains Mono

### Performance Levels
| Level | Color | Threshold |
|-------|-------|-----------|
| Excelling | 🟢 Green | ≥ 80% mastery |
| On Track | 🔵 Blue | ≥ 60% mastery |
| Needs Help | 🟡 Amber | ≥ 40% mastery |
| Struggling | 🔴 Red | < 40% mastery |

---

## 🔐 Security

- JWT token stored in `localStorage` under `manhaji_token`
- Axios interceptor attaches `Authorization: Bearer <token>` to every request
- 401 responses automatically clear auth and redirect to `/login`
- Role check on login: only `ROLE_TEACHER` or `ROLE_ADMIN` can access the portal
- All backend endpoints secured with Spring Security `@PreAuthorize`

---

## 📱 Responsive Design

- Sidebar collapses to icon-only mode on demand
- Tables hide secondary columns on mobile
- Cards stack vertically on small screens
- All charts use `ResponsiveContainer` from Recharts

---

## 🌙 Dark Mode

Toggled via the sidebar button. Preference persisted in `localStorage` via Zustand persist middleware. CSS variables defined in `index.css` handle all color transitions automatically.

---

## ❓ Question Types Supported

All 13 question types from the backend are handled:

| Type | UI Behavior |
|------|-------------|
| MCQ | Selectable radio options |
| TRUE_FALSE | True/False radio |
| FILL_BLANK | Text input |
| SHORT_ANSWER | Text area |
| WRITE_ANSWER | Text area |
| ORDERING | Draggable list |
| REORDER_WORDS | Draggable list |
| PRONUNCIATION | Voice recording notice |
| VOICE_ANSWER | Voice recording notice |
| LISTEN_AND_CHOOSE | Audio upload + MCQ options |
| IMAGE_MATCH | Image upload notice |
| DRAG_DROP | Draggable list |
| TRACING | Tracing image upload notice |
