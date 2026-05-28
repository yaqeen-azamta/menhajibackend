import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthResponse } from '@/types';

interface AuthState {
  token: string | null;
  auth: AuthResponse | null;
  isAuthenticated: boolean;
  isDark: boolean;

  setAuth: (auth: AuthResponse) => void;
  logout: () => void;
  toggleDark: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      auth: null,
      isAuthenticated: false,
      isDark: false,

      setAuth: (auth) => {
        localStorage.setItem('manhaji_token', auth.token);
        set({ token: auth.token, auth, isAuthenticated: true });
      },

      logout: () => {
        localStorage.removeItem('manhaji_token');
        set({ token: null, auth: null, isAuthenticated: false });
      },

      toggleDark: () =>
        set((state) => {
          const next = !state.isDark;
          if (next) document.documentElement.classList.add('dark');
          else document.documentElement.classList.remove('dark');
          return { isDark: next };
        }),
    }),
    {
      name: 'manhaji_auth',
      partialize: (state) => ({
        token: state.token,
        auth: state.auth,
        isAuthenticated: state.isAuthenticated,
        isDark: state.isDark,
      }),
    }
  )
);
