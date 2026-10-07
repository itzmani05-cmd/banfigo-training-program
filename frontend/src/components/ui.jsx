import { Children, cloneElement, isValidElement } from 'react';
import Icon from './icons';

// Shared building blocks so every screen uses the same styles (tokens live in index.css)

export const inputClass =
  'w-full rounded-lg border border-line bg-paper px-3 py-2.5 text-body text-ink shadow-xs ' +
  'placeholder:text-subtle transition-colors hover:border-axis ' +
  'focus:border-brand focus:outline-none focus:ring-3 focus:ring-brand/15 ' +
  'disabled:cursor-not-allowed disabled:bg-wash disabled:text-subtle';

const BUTTON_VARIANTS = {
  primary: 'border-brand bg-brand text-paper shadow-sm hover:border-brand-dark hover:bg-brand-dark',
  secondary: 'border-line bg-paper text-ink shadow-xs hover:border-brand hover:text-brand',
  ghost: 'border-transparent bg-transparent text-muted hover:bg-brand-soft hover:text-brand',
  dark: 'border-ink bg-ink text-paper hover:bg-ink/85',
};

const BUTTON_SIZES = {
  md: 'px-4 py-2.5 text-body',
  sm: 'px-3 py-1.5 text-label',
};

export function Button({ variant = 'secondary', size = 'md', icon, className = '', type = 'button', children, ...props }) {
  return (
    <button
      type={type}
      className={
        'inline-flex cursor-pointer items-center justify-center gap-2 whitespace-nowrap rounded-lg border font-bold ' +
        'transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand ' +
        'disabled:cursor-not-allowed disabled:opacity-40 ' +
        `${BUTTON_VARIANTS[variant]} ${BUTTON_SIZES[size]} ${className}`
      }
      {...props}
    >
      {icon && <Icon name={icon} className="size-4" />}
      {children}
    </button>
  );
}

export function PageHeader({ title, description, children }) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0">
        <h2>{title}</h2>
        {description && <p className="mt-1 max-w-2xl text-muted">{description}</p>}
      </div>
      {children && <div className="flex flex-wrap gap-2">{children}</div>}
    </div>
  );
}

export function Card({ title, subtitle, actions, children, className = '' }) {
  return (
    <section className={`mb-6 overflow-hidden rounded-xl border border-line bg-paper shadow-sm ${className}`}>
      {(title || actions) && (
        <div className="flex flex-col gap-3 border-b border-line px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <div>
            {title && <h3>{title}</h3>}
            {subtitle && <p className="mt-0.5 text-label text-muted">{subtitle}</p>}
          </div>
          {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
        </div>
      )}
      <div className="p-4 sm:p-6">{children}</div>
    </section>
  );
}

export function StatTile({ label, value, icon, highlight = false, className = '' }) {
  return (
    <div
      className={
        `flex items-start justify-between gap-3 rounded-xl border p-5 shadow-sm ${className} ` +
        (highlight ? 'border-brand bg-brand text-paper' : 'border-line bg-paper')
      }
    >
      <div className="min-w-0">
        <div className={`text-label ${highlight ? 'text-paper/75' : 'text-muted'}`}>{label}</div>
        <div className="mt-2 truncate text-stat font-bold tabular-nums">{value}</div>
      </div>
      {icon && (
        <span
          className={
            'grid size-10 shrink-0 place-items-center rounded-lg ' +
            (highlight ? 'bg-paper/15 text-paper' : 'bg-brand-soft text-brand')
          }
        >
          <Icon name={icon} />
        </span>
      )}
    </div>
  );
}

// Shown instead of a form the user's roles don't allow, so they know why it's missing
export function ViewOnly({ children }) {
  return (
    <div className="mb-6 flex items-start gap-3 rounded-xl border border-brand/20 bg-brand-soft px-4 py-3 text-brand-dark sm:px-5">
      <Icon name="info" className="mt-px size-5 text-brand" />
      <p>{children}</p>
    </div>
  );
}

export function Field({ label, hint, children, className = '' }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-label font-bold">{label}</span>
      {children}
      {hint && <span className="mt-1 block text-caption text-subtle">{hint}</span>}
    </label>
  );
}

// Two columns on wider screens; children are Field elements
export function FormGrid({ children, onSubmit }) {
  const Tag = onSubmit ? 'form' : 'div';
  return (
    <Tag onSubmit={onSubmit} className="grid gap-5 sm:grid-cols-2">
      {children}
    </Tag>
  );
}

// Full-width buttons on phones, natural width from sm up
export function FormActions({ children }) {
  return (
    <div className="flex flex-col gap-2 border-t border-line pt-5 sm:col-span-2 sm:flex-row max-sm:[&>button]:w-full">
      {children}
    </div>
  );
}

export function Alert({ children }) {
  if (!children) return null;
  return (
    <div role="alert" className="mb-6 flex items-start gap-3 rounded-xl border border-ink bg-paper px-4 py-3 shadow-sm sm:px-5">
      <Icon name="alert" className="mt-px size-5" />
      <p>
        <span className="font-bold">Something went wrong. </span>
        {children}
      </p>
    </div>
  );
}

export function Empty({ children }) {
  return (
    <div className="flex flex-col items-center gap-2 py-10 text-center text-muted">
      <span className="grid size-10 place-items-center rounded-full bg-brand-soft text-brand">
        <Icon name="info" />
      </span>
      <p>{children}</p>
    </div>
  );
}

// columns: header labels; prefix a label with '>' to right-align it (for amounts), e.g. '>Balance'.
// Below the md breakpoint the table turns into stacked cards: each row becomes a block and every
// cell shows its column label next to the value (labels are passed to each Td automatically).
export function Table({ columns, children }) {
  const labels = (columns || []).map((c) => c.replace(/^>/, ''));
  const rows = Children.map(children, (row) =>
    isValidElement(row)
      ? cloneElement(row, {
          children: Children.toArray(row.props.children).map((cell, i) =>
            isValidElement(cell) ? cloneElement(cell, { label: labels[i] }) : cell,
          ),
        })
      : row,
  );

  return (
    // Negative margins let the table run edge to edge inside a Card, and flush with its top/bottom
    // when it's the first/last thing in the card
    <div className="-mx-4 first:-mt-4 last:-mb-4 sm:-mx-6 sm:first:-mt-6 sm:last:-mb-6 md:overflow-x-auto">
      <table
        className={
          'w-full border-collapse text-left ' +
          'max-md:block max-md:[&_tbody]:block max-md:[&_tr]:block max-md:[&_tr]:px-4 max-md:[&_tr]:py-3 sm:max-md:[&_tr]:px-6 ' +
          'md:[&_td:first-child]:pl-6 md:[&_td:last-child]:pr-6 md:[&_th:first-child]:pl-6 md:[&_th:last-child]:pr-6'
        }
      >
        {columns && (
          <thead className="max-md:hidden">
            <tr className="border-b border-line bg-wash">
              {columns.map((c, i) => (
                <th
                  key={i}
                  className={
                    'px-3 py-3 text-caption font-bold whitespace-nowrap text-muted uppercase tracking-wider ' +
                    (c.startsWith('>') ? 'text-right' : '')
                  }
                >
                  {labels[i]}
                </th>
              ))}
            </tr>
          </thead>
        )}
        <tbody className="divide-y divide-line">{rows}</tbody>
      </table>
    </div>
  );
}

export function Td({ children, className = '', label = '' }) {
  return (
    <td
      data-label={label}
      className={
        'px-3 py-3 align-middle ' +
        'max-md:flex max-md:items-start max-md:justify-between max-md:gap-4 max-md:px-0 max-md:py-1 max-md:text-right ' +
        'max-md:before:shrink-0 max-md:before:text-left max-md:before:text-label max-md:before:font-normal max-md:before:text-muted ' +
        'max-md:before:content-[attr(data-label)] ' +
        className
      }
    >
      {/* One wrapper so multi-element cells stay together in the mobile flex layout */}
      <div className="max-md:min-w-0">{children}</div>
    </td>
  );
}

// Label / value rows for a single record
export function DetailList({ items }) {
  return (
    <dl className="grid gap-x-6 gap-y-5 sm:grid-cols-2 lg:grid-cols-3">
      {items.map(([label, value]) => (
        <div key={label} className="min-w-0">
          <dt className="text-label text-muted">{label}</dt>
          <dd className="mt-1 font-bold break-words">{value}</dd>
        </div>
      ))}
    </dl>
  );
}

const BADGE_TONES = {
  solid: 'border-brand bg-brand text-paper',
  outline: 'border-brand/40 bg-paper text-brand',
  muted: 'border-line bg-wash text-muted',
  in: 'border-brand/20 bg-brand-soft text-brand',
  out: 'border-ink/15 bg-wash text-ink',
};

export function Badge({ tone = 'muted', children }) {
  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-caption font-bold whitespace-nowrap ${BADGE_TONES[tone]}`}
    >
      {children}
    </span>
  );
}

export function TransactionTypeBadge({ type }) {
  return <Badge tone={type === 'DEPOSIT' ? 'in' : 'out'}>{type}</Badge>;
}

// Segmented control; stretches to full width on phones
export function Tabs({ options, value, onChange }) {
  return (
    <div className="flex w-full rounded-lg border border-line bg-wash p-1 sm:inline-flex sm:w-auto">
      {options.map((o) => (
        <button
          key={o.value}
          type="button"
          onClick={() => onChange(o.value)}
          aria-pressed={value === o.value}
          className={
            'flex-1 cursor-pointer rounded-md px-3 py-1.5 text-body font-bold whitespace-nowrap transition-colors ' +
            (value === o.value ? 'bg-brand text-paper shadow-sm' : 'text-muted hover:text-brand')
          }
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}
