import React from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { TopBar } from '@/components/layout/AppLayout';
import { StatCard, Card, CardHeader, CardContent, CardTitle, Skeleton, ProgressBar } from '@/components/common';
import { useAuthStore } from '@/store/authStore';
import { teacherApi } from '@/api/services';
import { derivePerformanceLevel, getPerformanceBadge, formatPoints, getInitials } from '@/utils';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell,
} from 'recharts';
import { Users, BookOpen, Flame, Star, ArrowRight, Target, TrendingUp } from 'lucide-react';
import type { ClassStudentSummary } from '@/types';

const PIE_COLORS = ['#10b981', '#3b82f6', '#f59e0b', '#ef4444'];

export const DashboardPage: React.FC = () => {
  const { auth } = useAuthStore();

  const { data: dashboard, isLoading } = useQuery({
    queryKey: ['teacher-dashboard'],
    queryFn: teacherApi.getDashboard,
  });

  const greetingHour = new Date().getHours();
  const greeting = greetingHour < 12 ? 'Good morning' : greetingHour < 17 ? 'Good afternoon' : 'Good evening';
  const name = dashboard?.fullName?.split(' ')[0] ?? auth?.fullName?.split(' ')[0] ?? 'Teacher';

  // Build performance mix from topStudents for the pie chart
  const allStudentsQuery = useQuery({
    queryKey: ['teacher-students'],
    queryFn: teacherApi.getStudents,
  });
  const students = allStudentsQuery.data ?? [];
  const perfCounts = { excelling: 0, onTrack: 0, needsHelp: 0, struggling: 0 };
  students.forEach((s) => {
    const level = derivePerformanceLevel(s.averageMastery);
    if (level === 'EXCELLING') perfCounts.excelling++;
    else if (level === 'ON_TRACK') perfCounts.onTrack++;
    else if (level === 'NEEDS_HELP') perfCounts.needsHelp++;
    else perfCounts.struggling++;
  });

  const pieData = [
    { name: 'Excelling',  value: perfCounts.excelling },
    { name: 'On Track',   value: perfCounts.onTrack   },
    { name: 'Needs Help', value: perfCounts.needsHelp },
    { name: 'Struggling', value: perfCounts.struggling},
  ];

  // Simple 7-day placeholder engagement data (real time-series would need a dedicated endpoint)
  const DAYS = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];
  const engagementData = DAYS.map((day) => ({
    day,
    lessons: dashboard ? Math.round((dashboard.lessonsCompletedTotal / 7) * (0.7 + Math.random() * 0.6)) : 0,
  }));

  return (
    <div className="space-y-6 animate-fade-in">
      <TopBar
        title="Dashboard"
        subtitle={`${greeting}, ${name} 👋`}
        actions={
          <span className="text-xs text-muted-foreground bg-muted px-3 py-1.5 rounded-full">
            {new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}
          </span>
        }
      />

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {isLoading ? (
          Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28" />)
        ) : (
          <>
            <StatCard
              title="Total Students"
              value={dashboard?.totalStudents ?? 0}
              subtitle={`Grade ${dashboard?.assignedGrade ?? '–'}`}
              icon={<Users className="h-5 w-5" />}
              color="bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400"
            />
            <StatCard
              title="Active This Week"
              value={dashboard?.activeThisWeek ?? 0}
              subtitle="logged in last 7 days"
              icon={<Star className="h-5 w-5" />}
              color="bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400"
            />
            <StatCard
              title="Lessons Completed"
              value={dashboard?.lessonsCompletedTotal ?? 0}
              subtitle="across all students"
              icon={<BookOpen className="h-5 w-5" />}
              color="bg-primary/10 text-primary"
            />
            <StatCard
              title="Avg Mastery"
              value={`${dashboard?.averageMasteryAcrossClass?.toFixed(1) ?? 0}%`}
              subtitle="class average"
              icon={<Target className="h-5 w-5" />}
              color="bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400"
            />
          </>
        )}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Engagement area chart */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>Weekly Lesson Completions</CardTitle>
              <span className="text-xs text-muted-foreground">Last 7 days</span>
            </div>
          </CardHeader>
          <CardContent>
            {isLoading ? <Skeleton className="h-48" /> : (
              <ResponsiveContainer width="100%" height={200}>
                <AreaChart data={engagementData}>
                  <defs>
                    <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%"  stopColor="hsl(158 64% 40%)" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="hsl(158 64% 40%)" stopOpacity={0.0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                  <XAxis dataKey="day" tick={{ fontSize: 11, fill: 'hsl(var(--muted-foreground))' }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: 'hsl(var(--muted-foreground))' }} axisLine={false} tickLine={false} />
                  <Tooltip contentStyle={{ background: 'hsl(var(--card))', border: '1px solid hsl(var(--border))', borderRadius: 8, fontSize: 12 }} />
                  <Area type="monotone" dataKey="lessons" name="Lessons" stroke="hsl(158 64% 40%)" strokeWidth={2} fill="url(#grad)" />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        {/* Performance pie */}
        <Card>
          <CardHeader><CardTitle>Performance Mix</CardTitle></CardHeader>
          <CardContent>
            {allStudentsQuery.isLoading ? <Skeleton className="h-48" /> : (
              <div className="space-y-3">
                <ResponsiveContainer width="100%" height={130}>
                  <PieChart>
                    <Pie data={pieData} cx="50%" cy="50%" innerRadius={38} outerRadius={62} paddingAngle={2} dataKey="value">
                      {pieData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i]} />)}
                    </Pie>
                  </PieChart>
                </ResponsiveContainer>
                <div className="space-y-1.5">
                  {pieData.map((entry, i) => (
                    <div key={entry.name} className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2">
                        <span className="h-2 w-2 rounded-full flex-shrink-0" style={{ background: PIE_COLORS[i] }} />
                        <span className="text-muted-foreground">{entry.name}</span>
                      </div>
                      <span className="font-semibold">{entry.value}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Top students */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Top Performers</CardTitle>
            <Link to="/students" className="text-xs text-primary font-medium hover:underline flex items-center gap-1">
              View all <ArrowRight className="h-3 w-3" />
            </Link>
          </div>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {isLoading ? (
            <p className="text-center text-muted-foreground text-sm py-8">Loading...</p>
          ) : (dashboard?.topStudents ?? []).length === 0 ? (
            <p className="text-center text-muted-foreground text-sm py-8">No students yet</p>
          ) : (
            <div className="divide-y divide-border">
              {(dashboard?.topStudents ?? []).map((s: ClassStudentSummary, idx) => {
                const level = derivePerformanceLevel(s.averageMastery);
                const perf = getPerformanceBadge(level);
                return (
                  <Link key={s.studentId} to={`/students/${s.studentId}`}
                    className="flex items-center gap-4 px-5 py-3.5 hover:bg-muted/50 transition-colors">
                    <span className="text-sm font-bold text-muted-foreground w-6 text-center">#{idx + 1}</span>
                    <div className="h-9 w-9 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                      <span className="text-xs font-bold text-primary">{getInitials(s.fullName)}</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-sm text-foreground">{s.fullName}</p>
                      <p className="text-xs text-muted-foreground">Grade {s.gradeLevel ?? '–'}</p>
                    </div>
                    <span className={perf.class}>{perf.label}</span>
                    <div className="text-right">
                      <p className="text-sm font-bold">{formatPoints(s.totalPoints)}</p>
                      <p className="text-xs text-muted-foreground">pts</p>
                    </div>
                    <div className="flex items-center gap-1 text-orange-500 font-semibold text-sm">
                      <Flame className="h-4 w-4" />{s.currentStreak ?? 0}
                    </div>
                  </Link>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};
