import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/store/authStore';

import { LoginPage }          from '@/pages/auth/LoginPage';
import { DashboardPage }      from '@/pages/dashboard/DashboardPage';
import { StudentsPage }       from '@/pages/students/StudentsPage';
import { StudentProfilePage } from '@/pages/students/StudentProfilePage';
import { LessonsPage }        from '@/pages/lessons/LessonsPage';
import { QuestionsPage }      from '@/pages/questions/QuestionsPage';
import { AnalyticsPage }      from '@/pages/analytics/AnalyticsPage';
import { SettingsPage }       from '@/pages/settings/SettingsPage';
import { ProtectedRoute }     from '@/components/layout/ProtectedRoute';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
  },
});

export const App: React.FC = () => {
  const { isDark } = useAuthStore();

  useEffect(() => {
    if (isDark) document.documentElement.classList.add('dark');
    else document.documentElement.classList.remove('dark');
  }, [isDark]);

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard"          element={<DashboardPage />} />
            <Route path="/students"           element={<StudentsPage />} />
            <Route path="/students/:id"       element={<StudentProfilePage />} />
            <Route path="/subjects"           element={<LessonsPage />} />
            <Route path="/questions"          element={<QuestionsPage />} />
            <Route path="/analytics"          element={<AnalyticsPage />} />
            <Route path="/settings"           element={<SettingsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
};
