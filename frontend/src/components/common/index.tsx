import React from 'react';
import { cn } from '@/utils';
import { Loader2, X } from 'lucide-react';

// ==================== BUTTON ====================
interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'outline';
  size?: 'sm' | 'md' | 'lg' | 'icon';
  loading?: boolean;
  icon?: React.ReactNode;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', loading, icon, children, disabled, ...props }, ref) => {
    const base = 'inline-flex items-center justify-center gap-2 font-medium rounded-lg transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-50 disabled:pointer-events-none';
    const variants = {
      primary: 'bg-primary text-primary-foreground hover:bg-primary/90 shadow-sm',
      secondary: 'bg-secondary text-secondary-foreground hover:bg-secondary/80',
      ghost: 'hover:bg-accent hover:text-accent-foreground',
      danger: 'bg-destructive text-destructive-foreground hover:bg-destructive/90 shadow-sm',
      outline: 'border border-border bg-transparent hover:bg-accent hover:text-accent-foreground',
    };
    const sizes = {
      sm: 'h-8 px-3 text-xs',
      md: 'h-10 px-4 text-sm',
      lg: 'h-11 px-6 text-base',
      icon: 'h-9 w-9 p-0',
    };
    return (
      <button
        ref={ref}
        className={cn(base, variants[variant], sizes[size], className)}
        disabled={disabled || loading}
        {...props}
      >
        {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : icon}
        {children}
      </button>
    );
  }
);
Button.displayName = 'Button';

// ==================== INPUT ====================
interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, leftIcon, rightIcon, id, ...props }, ref) => {
    const inputId = id ?? label?.toLowerCase().replace(/\s/g, '-');
    return (
      <div className="flex flex-col gap-1.5">
        {label && (
          <label htmlFor={inputId} className="text-sm font-medium text-foreground">
            {label}
          </label>
        )}
        <div className="relative">
          {leftIcon && (
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
              {leftIcon}
            </span>
          )}
          <input
            id={inputId}
            ref={ref}
            className={cn(
              'flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm ring-offset-background transition-colors',
              'placeholder:text-muted-foreground',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:border-primary',
              'disabled:cursor-not-allowed disabled:opacity-50',
              leftIcon && 'pl-9',
              rightIcon && 'pr-9',
              error && 'border-destructive focus-visible:ring-destructive',
              className
            )}
            {...props}
          />
          {rightIcon && (
            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground">
              {rightIcon}
            </span>
          )}
        </div>
        {error && <p className="text-xs text-destructive">{error}</p>}
      </div>
    );
  }
);
Input.displayName = 'Input';

// ==================== TEXTAREA ====================
interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
}

export const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, error, id, ...props }, ref) => {
    const inputId = id ?? label?.toLowerCase().replace(/\s/g, '-');
    return (
      <div className="flex flex-col gap-1.5">
        {label && (
          <label htmlFor={inputId} className="text-sm font-medium text-foreground">
            {label}
          </label>
        )}
        <textarea
          id={inputId}
          ref={ref}
          className={cn(
            'flex min-h-[80px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm',
            'placeholder:text-muted-foreground',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:border-primary',
            'disabled:cursor-not-allowed disabled:opacity-50 resize-none transition-colors',
            error && 'border-destructive',
            className
          )}
          {...props}
        />
        {error && <p className="text-xs text-destructive">{error}</p>}
      </div>
    );
  }
);
Textarea.displayName = 'Textarea';

// ==================== SELECT ====================
interface SelectOption { value: string; label: string; }
interface SelectProps {
  label?: string;
  value?: string;
  onChange?: (value: string) => void;
  options: SelectOption[];
  placeholder?: string;
  error?: string;
  className?: string;
}

export const Select: React.FC<SelectProps> = ({ label, value, onChange, options, placeholder, error, className }) => {
  return (
    <div className="flex flex-col gap-1.5">
      {label && <label className="text-sm font-medium text-foreground">{label}</label>}
      <select
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
        className={cn(
          'flex h-10 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:border-primary',
          'disabled:cursor-not-allowed disabled:opacity-50 transition-colors',
          error && 'border-destructive',
          className
        )}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  );
};

// ==================== CARD ====================
export const Card: React.FC<{ className?: string; children: React.ReactNode; onClick?: () => void }> = ({ className, children, onClick }) => (
  <div
    className={cn('card-base', onClick && 'cursor-pointer hover:shadow-md transition-shadow', className)}
    onClick={onClick}
  >
    {children}
  </div>
);

export const CardHeader: React.FC<{ className?: string; children: React.ReactNode }> = ({ className, children }) => (
  <div className={cn('p-5 pb-3', className)}>{children}</div>
);

export const CardContent: React.FC<{ className?: string; children: React.ReactNode }> = ({ className, children }) => (
  <div className={cn('px-5 pb-5', className)}>{children}</div>
);

export const CardTitle: React.FC<{ className?: string; children: React.ReactNode }> = ({ className, children }) => (
  <h3 className={cn('text-base font-semibold text-foreground', className)}>{children}</h3>
);

// ==================== BADGE ====================
interface BadgeProps {
  variant?: 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info';
  className?: string;
  children: React.ReactNode;
}

export const Badge: React.FC<BadgeProps> = ({ variant = 'default', className, children }) => {
  const variants = {
    default: 'bg-muted text-muted-foreground',
    primary: 'bg-primary/10 text-primary',
    success: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400',
    warning: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
    danger: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
    info: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  };
  return (
    <span className={cn('inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold', variants[variant], className)}>
      {children}
    </span>
  );
};

// ==================== AVATAR ====================
interface AvatarProps {
  src?: string;
  alt?: string;
  initials?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

export const Avatar: React.FC<AvatarProps> = ({ src, alt, initials, size = 'md', className }) => {
  const sizes = { sm: 'h-8 w-8 text-xs', md: 'h-10 w-10 text-sm', lg: 'h-12 w-12 text-base', xl: 'h-16 w-16 text-xl' };
  return (
    <div className={cn('relative rounded-full overflow-hidden flex-shrink-0 bg-primary/10 flex items-center justify-center', sizes[size], className)}>
      {src ? (
        <img src={src} alt={alt} className="h-full w-full object-cover" />
      ) : (
        <span className="font-semibold text-primary">{initials}</span>
      )}
    </div>
  );
};

// ==================== SKELETON ====================
export const Skeleton: React.FC<{ className?: string }> = ({ className }) => (
  <div className={cn('shimmer rounded-lg bg-muted', className)} />
);

// ==================== MODAL ====================
interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  children: React.ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  footer?: React.ReactNode;
}

export const Modal: React.FC<ModalProps> = ({ open, onClose, title, children, size = 'md', footer }) => {
  if (!open) return null;
  const sizes = { sm: 'max-w-sm', md: 'max-w-lg', lg: 'max-w-2xl', xl: 'max-w-4xl' };
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
      <div className={cn('relative w-full bg-card border border-border rounded-2xl shadow-2xl animate-fade-in', sizes[size])}>
        {title && (
          <div className="flex items-center justify-between p-5 border-b border-border">
            <h2 className="text-lg font-semibold text-foreground">{title}</h2>
            <button onClick={onClose} className="rounded-lg p-1.5 hover:bg-accent transition-colors">
              <X className="h-4 w-4" />
            </button>
          </div>
        )}
        <div className="p-5 max-h-[70vh] overflow-y-auto">{children}</div>
        {footer && <div className="p-5 border-t border-border bg-muted/30 rounded-b-2xl">{footer}</div>}
      </div>
    </div>
  );
};

// ==================== PROGRESS BAR ====================
interface ProgressBarProps {
  value: number;
  max?: number;
  className?: string;
  color?: string;
  showLabel?: boolean;
}

export const ProgressBar: React.FC<ProgressBarProps> = ({ value, max = 100, className, color = 'bg-primary', showLabel }) => {
  const pct = Math.min((value / max) * 100, 100);
  return (
    <div className={cn('flex items-center gap-2', className)}>
      <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
        <div className={cn('h-full rounded-full transition-all duration-500', color)} style={{ width: `${pct}%` }} />
      </div>
      {showLabel && <span className="text-xs font-medium text-muted-foreground w-8 text-right">{Math.round(pct)}%</span>}
    </div>
  );
};

// ==================== TABS ====================
interface TabItem { id: string; label: string; icon?: React.ReactNode; count?: number; }
interface TabsProps {
  items: TabItem[];
  active: string;
  onChange: (id: string) => void;
  className?: string;
}

export const Tabs: React.FC<TabsProps> = ({ items, active, onChange, className }) => (
  <div className={cn('flex gap-1 bg-muted/60 rounded-lg p-1', className)}>
    {items.map((item) => (
      <button
        key={item.id}
        onClick={() => onChange(item.id)}
        className={cn(
          'flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-all duration-150',
          active === item.id
            ? 'bg-card shadow-sm text-foreground'
            : 'text-muted-foreground hover:text-foreground'
        )}
      >
        {item.icon}
        {item.label}
        {item.count !== undefined && (
          <span className={cn('text-xs rounded-full px-1.5 py-0.5', active === item.id ? 'bg-primary text-primary-foreground' : 'bg-muted-foreground/20')}>
            {item.count}
          </span>
        )}
      </button>
    ))}
  </div>
);

// ==================== EMPTY STATE ====================
export const EmptyState: React.FC<{ icon?: React.ReactNode; title: string; description?: string; action?: React.ReactNode }> = ({
  icon, title, description, action,
}) => (
  <div className="flex flex-col items-center justify-center gap-3 py-16 text-center">
    {icon && <div className="text-muted-foreground/50 text-5xl mb-2">{icon}</div>}
    <h3 className="text-base font-semibold text-foreground">{title}</h3>
    {description && <p className="text-sm text-muted-foreground max-w-sm">{description}</p>}
    {action && <div className="mt-2">{action}</div>}
  </div>
);

// ==================== STAT CARD ====================
interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  trend?: { value: number; label?: string };
  color?: string;
  className?: string;
}

export const StatCard: React.FC<StatCardProps> = ({ title, value, subtitle, icon, trend, color = 'bg-primary/10 text-primary', className }) => (
  <div className={cn('stat-card', className)}>
    <div className="flex items-start justify-between">
      <div className={cn('p-2.5 rounded-xl', color)}>{icon}</div>
      {trend && (
        <span className={cn('text-xs font-semibold px-2 py-1 rounded-full', trend.value >= 0 ? 'text-emerald-600 bg-emerald-50 dark:bg-emerald-900/20 dark:text-emerald-400' : 'text-red-600 bg-red-50 dark:bg-red-900/20 dark:text-red-400')}>
          {trend.value >= 0 ? '+' : ''}{trend.value}%
        </span>
      )}
    </div>
    <div>
      <p className="text-xs text-muted-foreground uppercase tracking-wider font-medium">{title}</p>
      <p className="text-2xl font-bold text-foreground mt-0.5">{value}</p>
      {subtitle && <p className="text-xs text-muted-foreground mt-0.5">{subtitle}</p>}
    </div>
  </div>
);

// ==================== SEARCH INPUT ====================
export const SearchInput: React.FC<{ value: string; onChange: (v: string) => void; placeholder?: string; className?: string }> = ({
  value, onChange, placeholder = 'Search...', className,
}) => (
  <div className={cn('relative', className)}>
    <svg className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
    </svg>
    <input
      type="text"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      className="flex h-10 w-full rounded-lg border border-input bg-background pl-9 pr-4 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:border-primary transition-colors placeholder:text-muted-foreground"
    />
  </div>
);

// ==================== TOAST (simple) ====================
export const useToast = () => {
  const toast = ({ title, description, variant = 'default' }: { title: string; description?: string; variant?: 'default' | 'destructive' }) => {
    // Simple alert fallback — in production use a proper toast library
    if (variant === 'destructive') {
      console.error(title, description);
    } else {
      console.log(title, description);
    }
  };
  return { toast };
};
