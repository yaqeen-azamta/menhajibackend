import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { TopBar } from '@/components/layout/AppLayout';
import { Card, Skeleton, EmptyState } from '@/components/common';
import { teacherApi } from '@/api/services';
import { cn } from '@/utils';
import { BookOpen, Layers, HelpCircle, ChevronRight, Hash } from 'lucide-react';
import type { SubjectSummary } from '@/types';

export const LessonsPage: React.FC = () => {
  const [selected, setSelected] = useState<SubjectSummary | null>(null);

  const { data: subjects = [], isLoading } = useQuery({
    queryKey: ['teacher-subjects'],
    queryFn: teacherApi.getSubjects,
    onSuccess: (data: SubjectSummary[]) => { if (data.length > 0 && !selected) setSelected(data[0]); },
  } as any);

  return (
    <div className="space-y-5 animate-fade-in">
      <TopBar title="Subjects & Lessons" subtitle="Browse subjects assigned to your grade" />

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-5">
        {/* Subject list */}
        <div className="lg:col-span-1">
          <div className="card-base overflow-hidden">
            <div className="px-4 py-3 border-b border-border bg-muted/30">
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Your Subjects</p>
            </div>
            {isLoading ? (
              <div className="p-3 space-y-2">{Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-14 rounded-lg" />)}</div>
            ) : subjects.length === 0 ? (
              <div className="p-4 text-sm text-muted-foreground text-center">No subjects assigned yet.</div>
            ) : (
              <div className="p-2 space-y-1">
                {(subjects as SubjectSummary[]).map((sub) => (
                  <button key={sub.subjectId} onClick={() => setSelected(sub)}
                    className={cn(
                      'w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-left transition-all',
                      selected?.subjectId === sub.subjectId
                        ? 'bg-primary text-primary-foreground'
                        : 'hover:bg-accent text-foreground'
                    )}>
                    <div className={cn('h-8 w-8 rounded-lg flex items-center justify-center flex-shrink-0', selected?.subjectId === sub.subjectId ? 'bg-white/20' : 'bg-primary/10')}>
                      <Layers className={cn('h-4 w-4', selected?.subjectId === sub.subjectId ? 'text-white' : 'text-primary')} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-sm truncate">{sub.subjectName}</p>
                      <p className={cn('text-xs', selected?.subjectId === sub.subjectId ? 'text-white/70' : 'text-muted-foreground')}>
                        {sub.totalLessons ?? 0} lessons · {sub.totalQuestions ?? 0} questions
                      </p>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Subject detail panel */}
        <div className="lg:col-span-3">
          {!selected ? (
            <EmptyState icon={<BookOpen />} title="Select a subject" description="Choose a subject from the left to view details." />
          ) : (
            <Card className="overflow-hidden">
              <div className="bg-gradient-to-r from-primary/10 via-primary/5 to-transparent px-6 py-5 border-b border-border">
                <div className="flex items-start justify-between">
                  <div>
                    <h2 className="text-lg font-bold text-foreground">{selected.subjectName}</h2>
                    <p className="text-sm text-muted-foreground mt-0.5">Grade {selected.gradeLevel ?? '–'}</p>
                  </div>
                  <Link
                    to={`/questions?subjectId=${selected.subjectId}&subjectName=${encodeURIComponent(selected.subjectName)}`}
                    className="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 transition-colors">
                    <HelpCircle className="h-4 w-4" /> View Questions
                  </Link>
                </div>

                <div className="grid grid-cols-2 gap-4 mt-5">
                  <div className="bg-white/60 dark:bg-white/5 rounded-xl p-4 text-center">
                    <p className="text-3xl font-bold text-primary">{selected.totalLessons ?? 0}</p>
                    <p className="text-xs text-muted-foreground mt-1 uppercase tracking-wider font-medium">Total Lessons</p>
                  </div>
                  <div className="bg-white/60 dark:bg-white/5 rounded-xl p-4 text-center">
                    <p className="text-3xl font-bold text-primary">{selected.totalQuestions ?? 0}</p>
                    <p className="text-xs text-muted-foreground mt-1 uppercase tracking-wider font-medium">Total Questions</p>
                  </div>
                </div>
              </div>

              <div className="p-6">
                <p className="text-sm text-muted-foreground text-center">
                  Lesson management (create, edit, reorder) is handled in your admin panel.<br />
                  Click <strong className="text-foreground">View Questions</strong> to browse and manage questions for this subject.
                </p>
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
};
