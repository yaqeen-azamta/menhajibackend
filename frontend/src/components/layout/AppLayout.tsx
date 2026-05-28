import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { cn } from '@/utils';
import { getInitials } from '@/utils';
import { useAuthStore } from '@/store/authStore';
import {
  LayoutDashboard, Users, BookOpen, HelpCircle, BarChart3,
  Settings, LogOut, Moon, Sun, ChevronRight, X, GraduationCap,
} from 'lucide-react';

const navItems = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/students',  icon: Users,          label: 'Students'  },
  { to: '/subjects',  icon: BookOpen,        label: 'Subjects'  },
  { to: '/questions', icon: HelpCircle,      label: 'Questions' },
  { to: '/analytics', icon: BarChart3,       label: 'Analytics' },
];

const Sidebar: React.FC<{ collapsed: boolean; onToggle: () => void }> = ({ collapsed, onToggle }) => {
  const { auth, logout, isDark, toggleDark } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => { logout(); navigate('/login'); };

  const name = auth?.fullName || 'Teacher';
  const role = auth?.role || '';

  return (
    <aside className={cn(
      'fixed inset-y-0 left-0 z-40 flex flex-col bg-sidebar border-r border-sidebar-border transition-all duration-300',
      collapsed ? 'w-16' : 'w-60'
    )}>
      {/* Logo */}
      <div className="flex items-center gap-3 h-16 px-4 border-b border-sidebar-border flex-shrink-0">
        <div className="flex-shrink-0 h-9 w-9 rounded-xl bg-primary flex items-center justify-center shadow-lg">
          <GraduationCap className="h-5 w-5 text-white" />
        </div>
        {!collapsed && (
          <div className="overflow-hidden">
            <p className="text-white font-bold text-base leading-tight">Manhaji</p>
            <p className="text-sidebar-foreground text-xs">Teacher Portal</p>
          </div>
        )}
        <button onClick={onToggle}
          className={cn('ml-auto p-1.5 rounded-lg text-sidebar-foreground hover:bg-sidebar-accent hover:text-white transition-colors', collapsed && 'mx-auto ml-0')}>
          {collapsed ? <ChevronRight className="h-4 w-4" /> : <X className="h-4 w-4" />}
        </button>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto py-4 px-2 space-y-1">
        {!collapsed && <p className="px-3 mb-2 text-xs font-semibold text-sidebar-foreground/50 uppercase tracking-wider">Main</p>}
        {navItems.map(({ to, icon: Icon, label }) => (
          <NavLink key={to} to={to}
            className={({ isActive }) => cn('nav-item', isActive ? 'nav-item-active' : 'nav-item-inactive', collapsed && 'justify-center px-0')}>
            <Icon className="h-5 w-5 flex-shrink-0" />
            {!collapsed && <span className="flex-1">{label}</span>}
          </NavLink>
        ))}

        {!collapsed && <p className="px-3 mb-2 mt-6 text-xs font-semibold text-sidebar-foreground/50 uppercase tracking-wider">Account</p>}
        <button onClick={toggleDark} className={cn('nav-item nav-item-inactive w-full', collapsed && 'justify-center px-0')}>
          {isDark ? <Sun className="h-5 w-5 flex-shrink-0" /> : <Moon className="h-5 w-5 flex-shrink-0" />}
          {!collapsed && <span>{isDark ? 'Light Mode' : 'Dark Mode'}</span>}
        </button>
        <NavLink to="/settings"
          className={({ isActive }) => cn('nav-item', isActive ? 'nav-item-active' : 'nav-item-inactive', collapsed && 'justify-center px-0')}>
          <Settings className="h-5 w-5 flex-shrink-0" />
          {!collapsed && <span>Settings</span>}
        </NavLink>
      </nav>

      {/* User */}
      <div className={cn('border-t border-sidebar-border p-3 flex items-center gap-3', collapsed && 'justify-center')}>
        <div className="h-8 w-8 rounded-full bg-primary/20 flex items-center justify-center flex-shrink-0 ring-2 ring-primary/30">
          <span className="text-xs font-bold text-primary">{getInitials(name)}</span>
        </div>
        {!collapsed && (
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-white truncate">{name}</p>
            <p className="text-xs text-sidebar-foreground truncate capitalize">{role.toLowerCase()}</p>
          </div>
        )}
        {!collapsed && (
          <button onClick={handleLogout} className="p-1.5 rounded-lg text-sidebar-foreground hover:bg-red-500/20 hover:text-red-400 transition-colors" title="Logout">
            <LogOut className="h-4 w-4" />
          </button>
        )}
      </div>
    </aside>
  );
};

export const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [collapsed, setCollapsed] = useState(false);
  return (
    <div className="min-h-screen bg-background">
      <Sidebar collapsed={collapsed} onToggle={() => setCollapsed(!collapsed)} />
      <main className={cn('transition-all duration-300 min-h-screen', collapsed ? 'ml-16' : 'ml-60')}>
        <div className="p-6 max-w-7xl mx-auto">{children}</div>
      </main>
    </div>
  );
};

export const TopBar: React.FC<{ title: string; subtitle?: string; actions?: React.ReactNode }> = ({ title, subtitle, actions }) => (
  <div className="flex items-center justify-between mb-6 animate-fade-in">
    <div>
      <h1 className="text-2xl font-bold text-foreground">{title}</h1>
      {subtitle && <p className="text-sm text-muted-foreground mt-0.5">{subtitle}</p>}
    </div>
    {actions && <div className="flex items-center gap-2">{actions}</div>}
  </div>
);
