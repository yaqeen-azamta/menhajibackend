import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { AppLayout } from '@/components/layout/AppLayout';

export const ProtectedRoute: React.FC = () => {
  const { isAuthenticated } = useAuthStore();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return <AppLayout><Outlet /></AppLayout>;
};
