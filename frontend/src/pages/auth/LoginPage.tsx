import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { authApi } from '@/api/services';
import { GraduationCap, Eye, EyeOff, Loader2 } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { setAuth, isAuthenticated } = useAuthStore();
  const [form, setForm] = useState({ username: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isAuthenticated) navigate('/dashboard', { replace: true });
  }, [isAuthenticated, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.username || !form.password) { setError('Please fill in all fields'); return; }
    setLoading(true);
    setError('');
    try {
      const auth = await authApi.login(form);
      if (auth.role !== 'TEACHER' && auth.role !== 'ADMIN') {
        setError('Access denied. This portal is for teachers only.');
        return;
      }
      setAuth(auth);
      navigate('/dashboard', { replace: true });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid username or password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 flex items-center justify-center p-4">
      <div className="absolute top-0 left-0 w-96 h-96 bg-primary/20 rounded-full blur-3xl -translate-x-1/2 -translate-y-1/2" />
      <div className="absolute bottom-0 right-0 w-96 h-96 bg-primary/10 rounded-full blur-3xl translate-x-1/2 translate-y-1/2" />

      <div className="relative w-full max-w-md">
        <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-3xl p-8 shadow-2xl animate-fade-in">
          <div className="flex flex-col items-center gap-3 mb-8">
            <div className="h-14 w-14 rounded-2xl bg-primary flex items-center justify-center shadow-lg shadow-primary/30">
              <GraduationCap className="h-7 w-7 text-white" />
            </div>
            <div className="text-center">
              <h1 className="text-2xl font-bold text-white">Manhaji</h1>
              <p className="text-white/50 text-sm">Teacher Portal</p>
            </div>
          </div>

          <h2 className="text-lg font-semibold text-white mb-1">Welcome back</h2>
          <p className="text-white/50 text-sm mb-6">Sign in to your teacher account</p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Username</label>
              <input
                type="text"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                placeholder="Enter your username"
                className="w-full h-11 rounded-xl bg-white/5 border border-white/10 text-white placeholder:text-white/30 px-4 text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary/50 transition-all"
                autoComplete="username"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-white/70 mb-1.5">Password</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  placeholder="Enter your password"
                  className="w-full h-11 rounded-xl bg-white/5 border border-white/10 text-white placeholder:text-white/30 px-4 pr-11 text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary/50 transition-all"
                  autoComplete="current-password"
                />
                <button type="button" onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white/70 transition-colors">
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            {error && (
              <div className="rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 text-sm px-4 py-3">
                {error}
              </div>
            )}

            <button type="submit" disabled={loading}
              className="w-full h-11 rounded-xl bg-primary hover:bg-primary/90 text-white font-semibold text-sm transition-all shadow-lg shadow-primary/20 disabled:opacity-70 flex items-center justify-center gap-2 mt-2">
              {loading ? <><Loader2 className="h-4 w-4 animate-spin" />Signing in...</> : 'Sign In'}
            </button>
          </form>

          <p className="text-center text-xs text-white/30 mt-6">Manhaji Educational Platform · Teachers Only</p>
        </div>
      </div>
    </div>
  );
};
