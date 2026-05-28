import React, { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { TopBar } from '@/components/layout/AppLayout';
import { Card, SearchInput, Skeleton, EmptyState } from '@/components/common';
import { teacherApi } from '@/api/services';
import { derivePerformanceLevel, getPerformanceBadge, formatPoints, getInitials, formatRelativeTime } from '@/utils';
import { Users, Flame, ChevronRight } from 'lucide-react';
import type { ClassStudentSummary } from '@/types';

const PERF_FILTERS = ['All', 'EXCELLING', 'ON_TRACK', 'NEEDS_HELP', 'STRUGGLING'];

const StudentRow: React.FC<{ student: ClassStudentSummary }> = ({ student }) => {
  const level = derivePerformanceLevel(student.averageMastery);
  const perf = getPerformanceBadge(level);
  return (
    <Link to={`/students/${student.studentId}`}
      className="flex items-center gap-4 px-5 py-4 hover:bg-muted/50 transition-colors group">
      <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0 ring-2 ring-transparent group-hover:ring-primary/20 transition-all">
        <span className="text-sm font-bold text-primary">{getInitials(student.fullName)}</span>
      </div>
      <div className="flex-1 min-w-0">
        <p className="font-semibold text-sm text-foreground">{student.fullName}</p>
        <p className="text-xs text-muted-foreground">
          Grade {student.gradeLevel ?? '–'} · {student.email ?? 'No email'}
        </p>
      </div>
      <span className={perf.class}>{perf.label}</span>
      <div className="hidden md:block text-center w-24">
        <p className="text-sm font-semibold text-foreground">{student.averageMastery?.toFixed(1) ?? '–'}%</p>
        <p className="text-xs text-muted-foreground">mastery</p>
      </div>
      <div className="text-right w-20">
        <p className="text-sm font-bold">{formatPoints(student.totalPoints)}</p>
        <p className="text-xs text-muted-foreground">pts</p>
      </div>
      <div className="flex items-center gap-1 w-14">
        <Flame className={`h-4 w-4 ${(student.currentStreak ?? 0) > 0 ? 'text-orange-500' : 'text-muted-foreground'}`} />
        <span className={`text-sm font-semibold ${(student.currentStreak ?? 0) > 0 ? 'text-orange-500' : 'text-muted-foreground'}`}>
          {student.currentStreak ?? 0}d
        </span>
      </div>
      <div className="hidden lg:block text-right w-28">
        <p className="text-xs text-muted-foreground">{formatRelativeTime(student.lastLoginAt)}</p>
        <p className="text-xs text-muted-foreground">last active</p>
      </div>
      <ChevronRight className="h-4 w-4 text-muted-foreground group-hover:text-primary transition-colors" />
    </Link>
  );
};

export const StudentsPage: React.FC = () => {
  const [search, setSearch] = useState('');
  const [perfFilter, setPerfFilter] = useState('All');

  const { data: students = [], isLoading } = useQuery({
    queryKey: ['teacher-students'],
    queryFn: teacherApi.getStudents,
  });

  const filtered = useMemo(() => {
    let list = students;
    if (search) {
      const q = search.toLowerCase();
      list = list.filter((s) => s.fullName.toLowerCase().includes(q) || (s.email ?? '').toLowerCase().includes(q));
    }
    if (perfFilter !== 'All') {
      list = list.filter((s) => derivePerformanceLevel(s.averageMastery) === perfFilter);
    }
    return list;
  }, [students, search, perfFilter]);

  return (
    <div className="space-y-5 animate-fade-in">
      <TopBar title="Students" subtitle={`${students.length} students in your class`} />

      {/* Filters */}
      <div className="flex flex-wrap gap-3 items-center">
        <SearchInput value={search} onChange={setSearch} placeholder="Search by name or email..." className="flex-1 min-w-48 max-w-sm" />
        <div className="flex gap-1.5 flex-wrap">
          {PERF_FILTERS.map((f) => (
            <button key={f} onClick={() => setPerfFilter(f)}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold border transition-all ${
                perfFilter === f ? 'bg-primary text-primary-foreground border-primary' : 'border-border text-muted-foreground hover:bg-accent'
              }`}>
              {f === 'All' ? 'All' : f.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <Card className="overflow-hidden">
        <div className="flex items-center gap-4 px-5 py-3 border-b border-border bg-muted/30">
          <div className="w-10 flex-shrink-0" />
          <div className="flex-1 text-xs font-semibold text-muted-foreground uppercase tracking-wider">Student</div>
          <div className="w-28 text-xs font-semibold text-muted-foreground uppercase tracking-wider">Performance</div>
          <div className="hidden md:block w-24 text-xs font-semibold text-muted-foreground uppercase tracking-wider text-center">Mastery</div>
          <div className="w-20 text-xs font-semibold text-muted-foreground uppercase tracking-wider text-right">Points</div>
          <div className="w-14 text-xs font-semibold text-muted-foreground uppercase tracking-wider">Streak</div>
          <div className="hidden lg:block w-28 text-xs font-semibold text-muted-foreground uppercase tracking-wider">Last Active</div>
          <div className="w-4" />
        </div>

        {isLoading ? (
          <div className="divide-y divide-border">
            {Array.from({ length: 8 }).map((_, i) => (
              <div key={i} className="flex items-center gap-4 px-5 py-4">
                <Skeleton className="h-10 w-10 rounded-full" />
                <div className="flex-1 space-y-1"><Skeleton className="h-4 w-36" /><Skeleton className="h-3 w-24" /></div>
                <Skeleton className="h-5 w-24 rounded-full" />
                <Skeleton className="h-4 w-16" />
              </div>
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState icon={<Users />} title="No students found" description="Try adjusting your search or filter." />
        ) : (
          <div className="divide-y divide-border">
            {filtered.map((s) => <StudentRow key={s.studentId} student={s} />)}
          </div>
        )}
      </Card>
    </div>
  );
};
