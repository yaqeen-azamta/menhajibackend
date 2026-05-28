import React, { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { TopBar } from '@/components/layout/AppLayout';
import { Card, Select, Skeleton, EmptyState, Badge } from '@/components/common';
import { teacherApi } from '@/api/services';
import { getQuestionTypeLabel, getQuestionTypeIcon, difficultyLabel, difficultyColor, cn } from '@/utils';
import { HelpCircle, Volume2, Image } from 'lucide-react';
import type { QuestionBankItem, LessonSummary } from '@/types';

const DIFFICULTY_OPTIONS = [
  { value: '', label: 'All Difficulties' },
  { value: '1', label: 'Easy' },
  { value: '2', label: 'Medium' },
  { value: '3', label: 'Hard' },
];

const QuestionCard: React.FC<{ q: QuestionBankItem; index: number }> = ({ q, index }) => (
  <div className="flex items-start gap-4 px-5 py-4 hover:bg-muted/30 transition-colors border-b border-border last:border-0">
    <div className="h-10 w-10 rounded-xl bg-muted flex items-center justify-center flex-shrink-0 text-lg">
      {getQuestionTypeIcon(q.questionType)}
    </div>
    <div className="flex-1 min-w-0">
      <div className="flex items-start gap-2 flex-wrap">
        <span className="text-xs text-muted-foreground font-mono mt-0.5">#{index + 1}</span>
        <p className="text-sm font-medium text-foreground flex-1">{q.questionText}</p>
      </div>
      <div className="flex items-center gap-2 mt-1.5 flex-wrap">
        <span className="text-xs bg-muted text-muted-foreground px-2 py-0.5 rounded-full font-medium">
          {getQuestionTypeLabel(q.questionType)}
        </span>
        {q.difficultyLevel && (
          <span className={cn('text-xs font-semibold px-2 py-0.5 rounded-full', difficultyColor(q.difficultyLevel))}>
            {difficultyLabel(q.difficultyLevel)}
          </span>
        )}
        {q.lessonTitle && (
          <span className="text-xs text-primary/80 bg-primary/10 px-2 py-0.5 rounded-full">
            📖 {q.lessonTitle}
          </span>
        )}
      </div>

      {/* MCQ options preview */}
      {q.options && q.options.length > 0 && (
        <div className="mt-2 grid grid-cols-2 gap-1">
          {q.options.map((opt) => (
            <div key={opt.optionId}
              className={cn('text-xs px-2.5 py-1.5 rounded-lg border', opt.isCorrect
                ? 'bg-emerald-50 border-emerald-200 text-emerald-700 dark:bg-emerald-900/20 dark:border-emerald-800 dark:text-emerald-400 font-semibold'
                : 'bg-muted/50 border-border text-muted-foreground')}>
              {opt.isCorrect && '✓ '}{opt.optionText}
            </div>
          ))}
        </div>
      )}

      {/* Correct answer for non-MCQ */}
      {q.correctAnswer && (!q.options || q.options.length === 0) && (
        <div className="mt-1.5 text-xs text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-900/20 px-2.5 py-1.5 rounded-lg border border-emerald-200 dark:border-emerald-800">
          ✓ {q.correctAnswer}
        </div>
      )}
    </div>
  </div>
);

export const QuestionsPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const subjectId = searchParams.get('subjectId');
  const subjectName = searchParams.get('subjectName');

  const [difficulty, setDifficulty] = useState('');
  const [lessonFilter, setLessonFilter] = useState('');

  const { data: bank, isLoading } = useQuery({
    queryKey: ['question-bank', subjectId, difficulty, lessonFilter],
    queryFn: () => teacherApi.getQuestions(
      Number(subjectId),
      difficulty ? Number(difficulty) : undefined,
      lessonFilter ? Number(lessonFilter) : undefined,
    ),
    enabled: !!subjectId,
  });

  // Build lesson filter options from the bank
  const lessonOptions = [
    { value: '', label: 'All Lessons' },
    ...(bank?.lessons ?? []).map((l: LessonSummary) => ({ value: String(l.lessonId), label: l.lessonTitle })),
  ];

  const questions = bank?.questions ?? [];

  if (!subjectId) {
    return (
      <div className="space-y-5 animate-fade-in">
        <TopBar title="Questions" subtitle="Select a subject to browse questions" />
        <EmptyState
          icon={<HelpCircle />}
          title="No subject selected"
          description="Go to Subjects, select a subject, and click 'View Questions'."
        />
      </div>
    );
  }

  return (
    <div className="space-y-5 animate-fade-in">
      <TopBar
        title={subjectName ? `Questions: ${decodeURIComponent(subjectName)}` : 'Question Bank'}
        subtitle={bank ? `${bank.totalQuestionsInSubject} total questions` : 'Loading...'}
      />

      {/* Filters */}
      <div className="flex gap-3 flex-wrap">
        <Select value={difficulty} onChange={setDifficulty} options={DIFFICULTY_OPTIONS} className="w-44" />
        <Select value={lessonFilter} onChange={setLessonFilter} options={lessonOptions} className="w-56" />
        {(difficulty || lessonFilter) && (
          <button onClick={() => { setDifficulty(''); setLessonFilter(''); }}
            className="px-3 py-2 text-xs text-muted-foreground border border-border rounded-lg hover:bg-accent transition-colors">
            Clear filters
          </button>
        )}
        <div className="ml-auto flex items-center gap-2 text-sm text-muted-foreground">
          Showing <strong className="text-foreground">{questions.length}</strong> questions
        </div>
      </div>

      {/* Question type breakdown pills */}
      {bank && bank.questions.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {[...new Set(bank.questions.map((q) => q.questionType))].map((type) => {
            const count = bank.questions.filter((q) => q.questionType === type).length;
            return (
              <span key={type} className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border border-border text-muted-foreground bg-muted/50">
                {getQuestionTypeIcon(type)} {getQuestionTypeLabel(type)} ({count})
              </span>
            );
          })}
        </div>
      )}

      {/* Questions list */}
      <Card className="overflow-hidden">
        {isLoading ? (
          <div className="divide-y divide-border">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="flex items-start gap-4 px-5 py-4">
                <Skeleton className="h-10 w-10 rounded-xl" />
                <div className="flex-1 space-y-1.5">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-3 w-1/3" />
                </div>
              </div>
            ))}
          </div>
        ) : questions.length === 0 ? (
          <EmptyState icon={<HelpCircle />} title="No questions found" description="Try different filters or select another subject." />
        ) : (
          questions.map((q, i) => <QuestionCard key={q.questionId} q={q} index={i} />)
        )}
      </Card>
    </div>
  );
};
