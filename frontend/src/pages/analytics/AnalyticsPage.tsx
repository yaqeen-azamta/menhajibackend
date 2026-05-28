import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { TopBar } from '@/components/layout/AppLayout';
import { Card, CardHeader, CardContent, CardTitle, Skeleton, EmptyState, ProgressBar, StatCard } from '@/components/common';
import { teacherApi } from '@/api/services';
import { derivePerformanceLevel, getPerformanceBadge, getInitials, formatPoints } from '@/utils';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  ScatterChart, Scatter, ZAxis,
} from 'recharts';
import { Trophy, Target, BarChart3, Users, Flame, BookOpen, TrendingUp } from 'lucide-react';
import type { ClassStudentSummary } from '@/types';

export const AnalyticsPage: React.FC = () => {
  const { data: students = [], isLoading: studentsLoading } = useQuery({
    queryKey: ['teacher-students'],
    queryFn: teacherApi.getStudents,
  });

  const { data: dashboard, isLoading: dashLoading } = useQuery({
    queryKey: ['teacher-dashboard'],
    queryFn: teacherApi.getDashboard,
  });

  const { data: subjects = [], isLoading: subjectsLoading } = useQuery({
    queryKey: ['teacher-subjects'],
    queryFn: teacherApi.getSubjects,
  });

  // Mastery distribution histogram
  const masteryBuckets = [
    { range: '0-20%',   count: 0 },
    { range: '20-40%',  count: 0 },
    { range: '40-60%',  count: 0 },
    { range: '60-80%',  count: 0 },
    { range: '80-100%', count: 0 },
  ];
  students.forEach((s) => {
    const m = s.averageMastery ?? 0;
    if (m < 20) masteryBuckets[0].count++;
    else if (m < 40) masteryBuckets[1].count++;
    else if (m < 60) masteryBuckets[2].count++;
    else if (m < 80) masteryBuckets[3].count++;
    else masteryBuckets[4].count++;
  });

  // Points vs mastery scatter data
  const scatterData = students.map((s) => ({
    points: s.totalPoints ?? 0,
    mastery: s.averageMastery ?? 0,
    name: s.fullName,
  }));

  // Ranked students
  const ranked = [...students].sort((a, b) => (b.totalPoints ?? 0) - (a.totalPoints ?? 0));

  const isLoading = studentsLoading || dashLoading;

  return (
    <div className="space-y-5 animate-fade-in">
      <TopBar title="Analytics" subtitle="Class performance insights" />

      {/* Summary stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {isLoading ? Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28" />) : (
          <>
            <StatCard
              title="Total Students"
              value={dashboard?.totalStudents ?? 0}
              icon={<Users className="h-5 w-5" />}
              color="bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400"
            />
            <StatCard
              title="Active This Week"
              value={dashboard?.activeThisWeek ?? 0}
              icon={<TrendingUp className="h-5 w-5" />}
              color="bg-primary/10 text-primary"
            />
            <StatCard
              title="Avg Mastery"
              value={`${dashboard?.averageMasteryAcrossClass?.toFixed(1) ?? 0}%`}
              icon={<Target className="h-5 w-5" />}
              color="bg-emerald-100 text-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-400"
            />
            <StatCard
              title="Lessons Completed"
              value={dashboard?.lessonsCompletedTotal ?? 0}
              icon={<BookOpen className="h-5 w-5" />}
              color="bg-amber-100 text-amber-600 dark:bg-amber-900/30 dark:text-amber-400"
            />
          </>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Mastery distribution */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5 text-primary" />
              <CardTitle>Mastery Distribution</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            {studentsLoading ? <Skeleton className="h-48" /> : (
              <ResponsiveContainer width="100%" height={200}>
                <BarChart data={masteryBuckets} margin={{ top: 5, right: 10, bottom: 5, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                  <XAxis dataKey="range" tick={{ fontSize: 10, fill: 'hsl(var(--muted-foreground))' }} axisLine={false} tickLine={false} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: 'hsl(var(--muted-foreground))' }} axisLine={false} tickLine={false} />
                  <Tooltip contentStyle={{ background: 'hsl(var(--card))', border: '1px solid hsl(var(--border))', borderRadius: 8, fontSize: 12 }} />
                  <Bar dataKey="count" name="Students" fill="hsl(158 64% 40%)" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        {/* Subjects overview */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-2">
              <BookOpen className="h-5 w-5 text-primary" />
              <CardTitle>Subjects Overview</CardTitle>
            </div>
          </CardHeader>
          <CardContent>
            {subjectsLoading ? <Skeleton className="h-48" /> : subjects.length === 0 ? (
              <EmptyState icon={<BookOpen />} title="No subjects" />
            ) : (
              <div className="space-y-4">
                {(subjects as any[]).map((sub) => (
                  <div key={sub.subjectId}>
                    <div className="flex justify-between text-sm mb-1.5">
                      <span className="font-medium text-foreground">{sub.subjectName}</span>
                      <span className="text-muted-foreground">{sub.totalQuestions ?? 0} questions</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                        <div className="h-full bg-primary rounded-full"
                          style={{ width: `${Math.min(((sub.totalLessons ?? 0) / 20) * 100, 100)}%` }} />
                      </div>
                      <span className="text-xs text-muted-foreground w-16 text-right">{sub.totalLessons ?? 0} lessons</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Student rankings */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Trophy className="h-5 w-5 text-amber-500" />
            <CardTitle>Student Rankings</CardTitle>
          </div>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          {studentsLoading ? (
            <div className="divide-y divide-border">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="flex items-center gap-4 px-5 py-3">
                  <Skeleton className="h-8 w-8 rounded-full" />
                  <div className="flex-1 space-y-1"><Skeleton className="h-4 w-36" /><Skeleton className="h-3 w-24" /></div>
                  <Skeleton className="h-4 w-16" />
                </div>
              ))}
            </div>
          ) : ranked.length === 0 ? (
            <EmptyState icon={<Users />} title="No student data" />
          ) : (
            <div className="divide-y divide-border">
              {ranked.map((s: ClassStudentSummary, idx: number) => {
                const level = derivePerformanceLevel(s.averageMastery);
                const perf = getPerformanceBadge(level);
                const medals = ['🥇', '🥈', '🥉'];
                return (
                  <Link key={s.studentId} to={`/students/${s.studentId}`}
                    className="flex items-center gap-4 px-5 py-3 hover:bg-muted/30 transition-colors">
                    <div className="w-8 text-center text-base flex-shrink-0">
                      {idx < 3 ? medals[idx] : <span className="text-sm font-bold text-muted-foreground">#{idx + 1}</span>}
                    </div>
                    <div className="h-9 w-9 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                      <span className="text-xs font-bold text-primary">{getInitials(s.fullName)}</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-semibold text-foreground">{s.fullName}</p>
                      <p className="text-xs text-muted-foreground">Grade {s.gradeLevel ?? '–'}</p>
                    </div>
                    <span className={perf.class}>{perf.label}</span>
                    <div className="text-right">
                      <p className="text-sm font-bold">{formatPoints(s.totalPoints)}</p>
                      <p className="text-xs text-muted-foreground">pts</p>
                    </div>
                    <div className="flex items-center gap-1 text-orange-500 text-sm font-bold w-12">
                      <Flame className="h-4 w-4" />{s.currentStreak ?? 0}
                    </div>
                    <div className="text-right w-16 hidden md:block">
                      <p className="text-sm font-semibold text-primary">{s.averageMastery?.toFixed(1) ?? 0}%</p>
                      <p className="text-xs text-muted-foreground">mastery</p>
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
