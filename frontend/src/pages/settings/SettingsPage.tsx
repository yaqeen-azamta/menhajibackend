import React from 'react';
import { TopBar } from '@/components/layout/AppLayout';
import { Card, CardHeader, CardContent, CardTitle } from '@/components/common';
import { useAuthStore } from '@/store/authStore';
import { getInitials } from '@/utils';
import { Moon, Sun, User, Bell, Globe } from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const { auth, isDark, toggleDark } = useAuthStore();
  const name = auth?.fullName ?? 'Teacher';
  const email = auth?.email ?? '';
  const role = auth?.role ?? '';

  return (
    <div className="space-y-5 animate-fade-in max-w-2xl">
      <TopBar title="Settings" subtitle="Manage your preferences" />

      {/* Profile */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2"><User className="h-4 w-4 text-primary" /><CardTitle>Profile</CardTitle></div>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-4 mb-4">
            <div className="h-16 w-16 rounded-2xl bg-primary/10 flex items-center justify-center text-xl font-bold text-primary">
              {getInitials(name)}
            </div>
            <div>
              <p className="font-semibold text-foreground">{name}</p>
              {email && <p className="text-sm text-muted-foreground">{email}</p>}
              <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-primary/10 text-primary mt-1">
                {role}
              </span>
            </div>
          </div>
          <p className="text-xs text-muted-foreground">Contact your administrator to update profile information.</p>
        </CardContent>
      </Card>

      {/* Appearance */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            {isDark ? <Moon className="h-4 w-4 text-primary" /> : <Sun className="h-4 w-4 text-primary" />}
            <CardTitle>Appearance</CardTitle>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-foreground">Dark Mode</p>
              <p className="text-xs text-muted-foreground">Switch between light and dark theme</p>
            </div>
            <button onClick={toggleDark}
              className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${isDark ? 'bg-primary' : 'bg-muted-foreground/30'}`}>
              <span className={`inline-block h-4 w-4 transform rounded-full bg-white shadow-sm transition-transform ${isDark ? 'translate-x-6' : 'translate-x-1'}`} />
            </button>
          </div>
        </CardContent>
      </Card>

      {/* Notifications */}
      <Card>
        <CardHeader><div className="flex items-center gap-2"><Bell className="h-4 w-4 text-primary" /><CardTitle>Notifications</CardTitle></div></CardHeader>
        <CardContent className="space-y-3">
          {[
            { label: 'Student performance alerts', desc: 'Get notified when a student is struggling' },
            { label: 'Weekly class summary',        desc: 'Summary of class activity every Monday'  },
          ].map((item) => (
            <div key={item.label} className="flex items-center justify-between py-1">
              <div>
                <p className="text-sm font-medium text-foreground">{item.label}</p>
                <p className="text-xs text-muted-foreground">{item.desc}</p>
              </div>
              <button className="relative inline-flex h-6 w-11 items-center rounded-full bg-primary">
                <span className="inline-block h-4 w-4 transform rounded-full bg-white shadow-sm translate-x-6" />
              </button>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Language */}
      <Card>
        <CardHeader><div className="flex items-center gap-2"><Globe className="h-4 w-4 text-primary" /><CardTitle>Language</CardTitle></div></CardHeader>
        <CardContent>
          <div className="flex gap-3">
            {['English', 'العربية'].map((lang, i) => (
              <button key={lang}
                className={`flex-1 py-2.5 rounded-lg border text-sm font-medium transition-all ${i === 0 ? 'border-primary bg-primary/10 text-primary' : 'border-border text-muted-foreground hover:bg-accent'}`}>
                {lang}
              </button>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
