import { ArrowLeft, Compass, LockKeyhole } from 'lucide-react'
import { Link } from 'react-router-dom'

export function AccessDeniedPage() {
  return (
    <div className="full-state">
      <span><LockKeyhole size={32} /></span>
      <p className="error-code">403</p>
      <h1>This area isn’t available to your role</h1>
      <p>Your account is working, but you don’t have permission for this action.</p>
      <Link className="button primary" to="/dashboard"><ArrowLeft size={17} />Back to overview</Link>
    </div>
  )
}

export function NotFoundPage() {
  return (
    <div className="full-state">
      <span><Compass size={32} /></span>
      <p className="error-code">404</p>
      <h1>We couldn’t find that page</h1>
      <p>The link may be outdated, or the item may no longer exist.</p>
      <Link className="button primary" to="/dashboard"><ArrowLeft size={17} />Back to overview</Link>
    </div>
  )
}
