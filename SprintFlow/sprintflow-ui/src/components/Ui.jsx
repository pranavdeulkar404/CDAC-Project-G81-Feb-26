import { AlertCircle, Inbox, LoaderCircle, X } from 'lucide-react'

export function PageHeader({ eyebrow, title, description, actions }) {
  return (
    <header className="page-header">
      <div>
        {eyebrow && <span className="eyebrow">{eyebrow}</span>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  )
}

export function Badge({ value, tone }) {
  const name = String(value || '').toLowerCase()
  const resolvedTone =
    tone ||
    (/(critical|blocked|open|reopened)/.test(name)
      ? 'danger'
      : /(high|on_hold|in_progress)/.test(name)
        ? 'warning'
        : /(completed|closed|verified|resolved|active)/.test(name)
          ? 'success'
          : 'neutral')
  return <span className={`badge badge-${resolvedTone}`}>{String(value || 'Not set').replaceAll('_', ' ')}</span>
}

export function Loading({ label = 'Loading your workspace…' }) {
  return (
    <div className="state-panel" aria-live="polite">
      <LoaderCircle className="spin" size={28} />
      <span>{label}</span>
    </div>
  )
}

export function Empty({ title = 'Nothing here yet', description = 'New items will appear here.' }) {
  return (
    <div className="state-panel">
      <Inbox size={34} />
      <strong>{title}</strong>
      <span>{description}</span>
    </div>
  )
}

export function ErrorState({ message, retry }) {
  return (
    <div className="state-panel error-state">
      <AlertCircle size={32} />
      <strong>We couldn’t load this view</strong>
      <span>{message}</span>
      {retry && <button onClick={retry}>Try again</button>}
    </div>
  )
}

export function Pagination({ page, totalPages, onPage }) {
  if (totalPages <= 1) return null
  return (
    <div className="pagination" aria-label="Pagination">
      <button disabled={page === 0} onClick={() => onPage(page - 1)}>Previous</button>
      <span>Page {page + 1} of {totalPages}</span>
      <button disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next</button>
    </div>
  )
}

export function ConfirmDialog({ open, title, message, confirmLabel = 'Confirm', danger, onConfirm, onClose }) {
  if (!open) return null
  return (
    <div className="dialog-backdrop" role="presentation" onMouseDown={onClose}>
      <div className="dialog" role="dialog" aria-modal="true" aria-labelledby="dialog-title" onMouseDown={(event) => event.stopPropagation()}>
        <button className="icon-button dialog-close" aria-label="Close" onClick={onClose}><X size={20} /></button>
        <h2 id="dialog-title">{title}</h2>
        <p>{message}</p>
        <div className="dialog-actions">
          <button className="button secondary" onClick={onClose}>Cancel</button>
          <button className={`button ${danger ? 'danger' : 'primary'}`} onClick={onConfirm}>{confirmLabel}</button>
        </div>
      </div>
    </div>
  )
}

export function FieldError({ children }) {
  return children ? <span className="field-error">{children}</span> : null
}

export function formatDate(value, includeTime = false) {
  if (!value) return 'Not set'
  const date = new Date(value)
  return new Intl.DateTimeFormat('en-IN', includeTime
    ? { dateStyle: 'medium', timeStyle: 'short' }
    : { dateStyle: 'medium' }).format(date)
}
