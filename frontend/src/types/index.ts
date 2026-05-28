// ============================================================
//  Types aligned 1-to-1 with the Manhaji Spring Boot backend
// ============================================================

// ==================== AUTH ====================
export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  role: string;
  fullName?: string;
  email?: string;
}

// ==================== TEACHER DASHBOARD ====================
export interface TeacherDashboardResponse {
  teacherId: number;
  fullName: string;
  department: string | null;
  assignedGrade: number | null;
  totalStudents: number;
  activeThisWeek: number;
  lessonsCompletedTotal: number;
  averageMasteryAcrossClass: number;
  topStudents: ClassStudentSummary[];
}

// ==================== STUDENTS ====================
export interface ClassStudentSummary {
  studentId: number;
  fullName: string;
  email: string | null;
  gradeLevel: number | null;
  totalPoints: number | null;
  currentStreak: number | null;
  lessonsCompleted: number | null;
  lessonsInProgress: number | null;
  averageMastery: number | null;
  lastLoginAt: string | null;
}

export interface StudentDetailResponse {
  studentId: number;
  fullName: string;
  email: string | null;
  phone: string | null;
  gradeLevel: number | null;
  totalPoints: number | null;
  currentStreak: number | null;
  lastLoginAt: string | null;
  createdAt: string | null;
  lessonsCompleted: number | null;
  lessonsInProgress: number | null;
  overallMastery: number | null;
  totalAttempts: number | null;
  averageScore: number | null;
  subjectBreakdown: SubjectMasterySummary[];
}

export interface SubjectMasterySummary {
  subjectId: number;
  subjectName: string;
  gradeLevel: number | null;
  masteryPercent: number | null;
  lessonsCompleted: number | null;
  totalLessons: number | null;
}

// ==================== SUBJECTS ====================
export interface SubjectSummary {
  subjectId: number;
  subjectName: string;
  gradeLevel: number | null;
  totalLessons: number | null;
  totalQuestions: number | null;
}

// ==================== QUESTION BANK ====================
export interface QuestionBankResponse {
  subjectId: number;
  subjectName: string;
  gradeLevel: number | null;
  lessons: LessonSummary[];
  questions: QuestionBankItem[];
  totalQuestionsInSubject: number;
}

export interface LessonSummary {
  lessonId: number;
  lessonTitle: string;
}

export interface QuestionBankItem {
  questionId: number;
  questionText: string;
  questionType: string;
  difficultyLevel: number | null;
  lessonId: number | null;
  lessonTitle: string | null;
  options: QuestionOption[] | null;
  correctAnswer: string | null;
}

export interface QuestionOption {
  optionId: number;
  optionText: string;
  isCorrect: boolean;
}

// ==================== API WRAPPER ====================
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
}

// ==================== UI HELPERS ====================
export type PerformanceLevel = 'EXCELLING' | 'ON_TRACK' | 'NEEDS_HELP' | 'STRUGGLING';

export function derivePerformanceLevel(mastery: number | null): PerformanceLevel {
  const m = mastery ?? 0;
  if (m >= 80) return 'EXCELLING';
  if (m >= 60) return 'ON_TRACK';
  if (m >= 40) return 'NEEDS_HELP';
  return 'STRUGGLING';
}
