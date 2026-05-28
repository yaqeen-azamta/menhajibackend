import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import type { PerformanceLevel } from '@/types';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function getPerformanceBadge(level: PerformanceLevel) {
  const map = {
    EXCELLING:  { label: 'Excelling',  class: 'badge-excelling',  dot: 'bg-emerald-500' },
    ON_TRACK:   { label: 'On Track',   class: 'badge-ontrack',    dot: 'bg-blue-500'    },
    NEEDS_HELP: { label: 'Needs Help', class: 'badge-needshelp',  dot: 'bg-amber-500'   },
    STRUGGLING: { label: 'Struggling', class: 'badge-struggling', dot: 'bg-red-500'     },
  };
  return map[level] ?? map.ON_TRACK;
}

/** 1 → Easy, 2 → Medium, 3 → Hard */
export function difficultyLabel(level: number | null): string {
  if (level === 1) return 'Easy';
  if (level === 2) return 'Medium';
  if (level === 3) return 'Hard';
  return 'Unknown';
}

export function difficultyColor(level: number | null): string {
  if (level === 1) return 'text-emerald-600 bg-emerald-50 dark:bg-emerald-900/20 dark:text-emerald-400';
  if (level === 2) return 'text-amber-600 bg-amber-50 dark:bg-amber-900/20 dark:text-amber-400';
  if (level === 3) return 'text-red-600 bg-red-50 dark:bg-red-900/20 dark:text-red-400';
  return 'text-muted-foreground bg-muted';
}

export function getQuestionTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    MCQ: 'Multiple Choice', TRUE_FALSE: 'True / False', FILL_BLANK: 'Fill in the Blank',
    SHORT_ANSWER: 'Short Answer', WRITE_ANSWER: 'Write Answer', ORDERING: 'Ordering',
    REORDER_WORDS: 'Reorder Words', PRONUNCIATION: 'Pronunciation', VOICE_ANSWER: 'Voice Answer',
    LISTEN_AND_CHOOSE: 'Listen & Choose', IMAGE_MATCH: 'Image Match', DRAG_DROP: 'Drag & Drop',
    TRACING: 'Tracing',
  };
  return labels[type] || type;
}

export function getQuestionTypeIcon(type: string): string {
  const icons: Record<string, string> = {
    MCQ: '☑', TRUE_FALSE: '⚖', FILL_BLANK: '✏', SHORT_ANSWER: '💬',
    WRITE_ANSWER: '📝', ORDERING: '🔢', REORDER_WORDS: '🔀',
    PRONUNCIATION: '🗣', VOICE_ANSWER: '🎤', LISTEN_AND_CHOOSE: '👂',
    IMAGE_MATCH: '🖼', DRAG_DROP: '🖱', TRACING: '✍',
  };
  return icons[type] || '❓';
}

export function formatPoints(p: number | null): string {
  return (p ?? 0).toLocaleString();
}

/** Return initials from a full name string e.g. "Ahmed Ali" → "AA" */
export function getInitials(fullName: string | null | undefined): string {
  if (!fullName) return '?';
  return fullName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0].toUpperCase())
    .join('');
}

export function formatRelativeTime(dateStr: string | null | undefined): string {
  if (!dateStr) return 'Never';
  const date = new Date(dateStr);
  const now = new Date();
  const diffMin = Math.floor((now.getTime() - date.getTime()) / 60000);
  if (diffMin < 1) return 'Just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHrs = Math.floor(diffMin / 60);
  if (diffHrs < 24) return `${diffHrs}h ago`;
  const diffDays = Math.floor(diffHrs / 24);
  if (diffDays < 7) return `${diffDays}d ago`;
  return date.toLocaleDateString();
}
