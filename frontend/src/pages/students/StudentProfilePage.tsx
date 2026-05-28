import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { TopBar } from '@/components/layout/AppLayout';
import { Card, CardHeader, CardContent, CardTitle, Skeleton, EmptyState, ProgressBar } from '@/components/common';
import { teacherApi } from '@/api/services';
import { derivePerformanceLevel, getPerformanceBadge, getInitials, formatPoints, formatRelativeTime } from '@/utils';
import { ArrowLeft, Flame, Target, BookOpen, Zap, CheckCircle, Clock, TrendingUp } from 'lucide-react';
import { RadarChart, Radar, PolarGrid, PolarAngleAxis, ResponsiveContainer } from 'recharts';
import type { SubjectMasterySummary } from '@/types';

export const StudentProfilePage: React.FC = () => {
  const { id } = useParams<{ id: string }>();

  const { data: student, isLoading } = useQuery({
    queryKey: ['student-detail', id],
    queryFn: () => teacherApi.getStudentDetail(Number(id)),
    enabled: !!id,
  });

  if (isLoading) {
    return (
      <div className="space-y-5 animate-fade-in">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-40 rounded-2xl" />
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-28" />)}</div>
      </div>
    );
  }

  if (!student) return <div className="text-center py-20 text-muted-foreground">Student not found.</div>;

  const level = derivePerformanceLevel(student.overallMastery);
  const perf = getPerformanceBadge(level);

  const radarData = (student.subjectBreakdown ?? []).map((sb: SubjectMasterySummary) => ({
    subject: sb.subjectName,
    score: sb.masteryPercent ?? 0,
    fullMark: 100,
  }));

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex items-center gap-3">
        <Link to="/students" className="p-2 rounded-lg hover:bg-accent transition-colors">
          <ArrowLeft className="h-4 w-4" />
        </Link>
        <TopBar title="Student Profile" subtitle="Detailed performance overview" />
      </div>

      {/* Hero */}
      <Card className="overflow-hidden">
        <div className="bg-gradient-to-r from-primary/10 via-primary/5 to-transparent p-6">
          <div className="flex items-start gap-5">
            <div className="h-20 w-20 rounded-2xl bg-primary/20 flex items-center justify-center flex-shrink-0 text-2xl font-bold text-primary ring-4 ring-primary/10">
              {getInitials(student.fullName)}
            </div>
            <div className="flex-1">
              <div className="flex items-center gap-3 flex-wrap">
                <h2 className="text-xl font-bold text-foreground">{student.fullName}</h2>
                <span className={perf.class}>{perf.label}</span>
              </div>
              <p className="text-muted-foreground text-sm mt-1">
                {student.email ?? 'No email'} · Grade {student.gradeLevel ?? '–'}
                {student.phone && <span> · {student.phone}</span>}
              </p>
              <div className="flex items-center gap-5 mt-4 flex-wrap">
                <div className="flex items-center gap-1.5 text-orange-500">
                  <Flame className="h-4 w-4" />
                  <span className="font-bold">{student.currentStreak ?? 0}</span>
                  <span className="text-xs text-muted-foreground">day streak</span>
                </div>
                <div className="flex items-center gap-1.5 text-amber-500">
                  <Zap className="h-4 w-4" />
                  <span className="font-bold">{formatPoints(student.totalPoints)}</span>
                  <span className="text-xs text-muted-foreground">pts</span>
                </div>
                <div className="flex items-center gap-1.5 text-muted-foreground text-sm">
                  <Clock className="h-4 w-4" />
                  Last active: {formatRelativeTime(student.lastLoginAt)}
                </div>
              </div>
            </div>
          </div>
        </div>
      </Card>

      {/* Stat cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { label: 'Total Points',       value: formatPoints(student.totalPoints),                         icon: <Zap className="h-5 w-5" />,       color: 'bg-amber-100 text-amber-600 dark:bg-amber-900/20 dark:text-amber-400' },
          { label: 'Overall Mastery',    value: `${student.overallMastery?.toFixed(1) ?? 0}%`,             icon: <Target className="h-5 w-5" />,     color: 'bg-primary/10 text-primary' },
          { label: 'Lessons Completed',  value: student.lessonsCompleted ?? 0,                             icon: <BookOpen className="h-5 w-5" />,   color: 'bg-blue-100 text-blue-600 dark:bg-blue-900/20 dark:text-blue-400' },
          { label: 'Quiz Attempts',      value: student.totalAttempts ?? 0,                                icon: <CheckCircle className="h-5 w-5" />, color: 'bg-emerald-100 text-emerald-600 dark:bg-emerald-900/20 dark:text-emerald-400' },
        ].map((s) => (
          <div key={s.label} className="stat-card">
            <div className={`p-2.5 rounded-xl w-fit ${s.color}`}>{s.icon}</div>
            <div>
              <p className="text-xs text-muted-foreground uppercase tracking-wider font-medium">{s.label}</p>
              <p className="text-2xl font-bold text-foreground">{s.value}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Subject mastery bars */}
        <Card>
          <CardHeader><CardTitle>Subject Mastery</CardTitle></CardHeader>
          <CardContent>
            {(student.subjectBreakdown ?? []).length === 0 ? (
              <EmptyState icon={<Target />} title="No subject data yet" />
            ) : (
              <div className="space-y-4">
                {student.subjectBreakdown.map((sb: SubjectMasterySummary) => (
                  <div key={sb.subjectId}>
                    <div className="flex justify-between text-sm mb-1.5">
                      <span className="font-medium text-foreground">{sb.subjectName}</span>
                      <span className="text-muted-foreground font-semibold">
                        {sb.masteryPercent?.toFixed(1) ?? 0}%
                        {sb.totalLessons ? <span className="text-xs ml-1">({sb.lessonsCompleted}/{sb.totalLessons} lessons)</span> : null}
                      </span>
                    </div>
                    <ProgressBar
                      value={sb.masteryPercent ?? 0}
                      color={(sb.masteryPercent ?? 0) >= 70 ? 'bg-emerald-500' : (sb.masteryPercent ?? 0) >= 50 ? 'bg-amber-500' : 'bg-red-500'}
                    />
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Radar chart */}
        {radarData.length > 0 ? (
          <Card>
            <CardHeader><CardTitle>Performance Radar</CardTitle></CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={220}>
                <RadarChart data={radarData}>
                  <PolarGrid stroke="hsl(var(--border))" />
                  <PolarAngleAxis dataKey="subject" tick={{ fontSize: 11, fill: 'hsl(var(--muted-foreground))' }} />
                  <Radar name="Mastery" dataKey="score" stroke="hsl(158 64% 40%)" fill="hsl(158 64% 40%)" fillOpacity={0.25} />
                </RadarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        ) : (
          // Show extra stats card when radar can't render
          <Card>
            <CardHeader><CardTitle>Quiz Performance</CardTitle></CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center justify-between p-3 rounded-xl bg-muted/50">
                <span className="text-sm text-muted-foreground">Total Attempts</span>
                <span className="font-bold text-foreground">{student.totalAttempts ?? 0}</span>
              </div>
              <div className="flex items-center justify-between p-3 rounded-xl bg-muted/50">
                <span className="text-sm text-muted-foreground">Average Score</span>
                <span className="font-bold text-foreground">{student.averageScore?.toFixed(1) ?? 0}%</span>
              </div>
              <div className="flex items-center justify-between p-3 rounded-xl bg-muted/50">
                <span className="text-sm text-muted-foreground">Lessons In Progress</span>
                <span className="font-bold text-foreground">{student.lessonsInProgress ?? 0}</span>
              </div>
              <div className="flex items-center justify-between p-3 rounded-xl bg-muted/50">
                <span className="text-sm text-muted-foreground">Member Since</span>
                <span className="font-bold text-foreground">{student.createdAt ? new Date(student.createdAt).toLocaleDateString() : '–'}</span>
              </div>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  );
};
