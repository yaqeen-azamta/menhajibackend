import apiClient from './client';
import type {
  LoginRequest, AuthResponse, ApiResponse,
  TeacherDashboardResponse, ClassStudentSummary,
  StudentDetailResponse, SubjectSummary,
  QuestionBankResponse,
} from '@/types';

// ==================== AUTH ====================
// Adjust the path below to match your actual login endpoint.
// Common options: /auth/login  /auth/signin  /users/login
export const authApi = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>('/auth/login', data);
    return res.data;
  },
};

// ==================== TEACHER DASHBOARD ====================
// GET /api/teacher/dashboard
export const teacherApi = {
  getDashboard: async (): Promise<TeacherDashboardResponse> => {
    const res = await apiClient.get<ApiResponse<TeacherDashboardResponse>>('/teacher/dashboard');
    return res.data.data;
  },

  // GET /api/teacher/students
  getStudents: async (): Promise<ClassStudentSummary[]> => {
    const res = await apiClient.get<ApiResponse<ClassStudentSummary[]>>('/teacher/students');
    return res.data.data;
  },

  // GET /api/teacher/students/{studentId}
  getStudentDetail: async (studentId: number): Promise<StudentDetailResponse> => {
    const res = await apiClient.get<ApiResponse<StudentDetailResponse>>(`/teacher/students/${studentId}`);
    return res.data.data;
  },

  // GET /api/teacher/subjects
  getSubjects: async (): Promise<SubjectSummary[]> => {
    const res = await apiClient.get<ApiResponse<SubjectSummary[]>>('/teacher/subjects');
    return res.data.data;
  },

  // GET /api/teacher/subjects/{subjectId}/questions?difficulty=&lessonId=
  getQuestions: async (
    subjectId: number,
    difficulty?: number,
    lessonId?: number
  ): Promise<QuestionBankResponse> => {
    const params: Record<string, string> = {};
    if (difficulty !== undefined) params.difficulty = String(difficulty);
    if (lessonId !== undefined) params.lessonId = String(lessonId);
    const res = await apiClient.get<ApiResponse<QuestionBankResponse>>(
      `/teacher/subjects/${subjectId}/questions`,
      { params }
    );
    return res.data.data;
  },
};
