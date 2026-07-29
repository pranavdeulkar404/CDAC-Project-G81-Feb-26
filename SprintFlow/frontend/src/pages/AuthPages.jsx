import { ArrowLeft, CheckCircle2, Eye, EyeOff, LockKeyhole, Mail } from 'lucide-react'
import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { apiError } from '../api/client'
import { authService } from '../api/services'
import { FieldError } from '../components/Ui'
import { useAuth } from '../context/AuthContext'

function AuthLayout({ title, subtitle, children }) {
  return (
    <div className="auth-shell">
      <section className="auth-aside">
        <div className="auth-brand">
          <img src="/sprintflow-logo.png" alt="SprintFlow" />
        </div>
        <div className="auth-aside-copy">
          <span className="eyebrow light">A calmer way to ship</span>
          <h1>One clear view of every project, task, and fix.</h1>
          <p>Keep your team aligned without losing decisions across spreadsheets and chat threads.</p>
          <div className="auth-benefit"><CheckCircle2 />Focused project progress</div>
          <div className="auth-benefit"><CheckCircle2 />Assignments that reach the right person</div>
          <div className="auth-benefit"><CheckCircle2 />A complete history of team updates</div>
        </div>
        <small>Built for small software teams</small>
      </section>
      <main className="auth-main">
        <div className="auth-card">
          <div className="auth-card-heading">
            <div className="auth-icon"><img src="/sprintflow-mark.png" alt="" /></div>
            <h2>{title}</h2>
            <p>{subtitle}</p>
          </div>
          {children}
        </div>
      </main>
    </div>
  )
}

function PasswordInput({ value, onChange, label = 'Password', autoComplete = 'current-password' }) {
  const [visible, setVisible] = useState(false)
  return (
    <label className="field">
      <span>{label}</span>
      <div className="input-with-icon">
        <LockKeyhole size={18} />
        <input
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          autoComplete={autoComplete}
          required
        />
        <button type="button" aria-label={visible ? 'Hide password' : 'Show password'} onClick={() => setVisible(!visible)}>
          {visible ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </div>
    </label>
  )
}

export function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [params] = useSearchParams()
  const [form, setForm] = useState({ email: '', password: '' })
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (params.get('session') === 'expired') toast.error('Your session expired. Please sign in again.')
  }, [params])

  if (isAuthenticated) return <Navigate to="/dashboard" replace />

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      await login(form)
      toast.success('Welcome back')
      navigate(location.state?.from?.pathname || '/dashboard', { replace: true })
    } catch (error) {
      toast.error(apiError(error, 'Email or password is incorrect'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to continue to your team workspace.">
      <form className="auth-form" onSubmit={submit}>
        <label className="field">
          <span>Email address</span>
          <div className="input-with-icon"><Mail size={18} /><input type="email" autoComplete="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></div>
        </label>
        <PasswordInput value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <div className="form-between"><span /><Link to="/forgot-password">Forgot password?</Link></div>
        <button className="button primary wide" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
      <p className="auth-switch">New to SprintFlow? <Link to="/register">Create an account</Link></p>
    </AuthLayout>
  )
}

export function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' })
  const [busy, setBusy] = useState(false)
  const passwordError = form.password && !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,72}$/.test(form.password)
    ? 'Use at least 8 characters with uppercase, lowercase, number, and symbol.'
    : ''

  const submit = async (event) => {
    event.preventDefault()
    if (passwordError) return
    if (form.password !== form.confirm) {
      toast.error('Passwords do not match')
      return
    }
    setBusy(true)
    try {
      await authService.register({ name: form.name, email: form.email, password: form.password })
      sessionStorage.setItem('verification_email', form.email.trim().toLowerCase())
      toast.success('Account created. Check your email for the code.')
      navigate('/verify-email')
    } catch (error) {
      toast.error(apiError(error, 'Account could not be created'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title="Create your account" subtitle="Join your team and keep work moving in one place.">
      <form className="auth-form" onSubmit={submit}>
        <label className="field"><span>Full name</span><input required minLength="2" maxLength="120" autoComplete="name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></label>
        <label className="field"><span>Email address</span><input type="email" required autoComplete="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
        <PasswordInput label="Password" autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <FieldError>{passwordError}</FieldError>
        <PasswordInput label="Confirm password" autoComplete="new-password" value={form.confirm} onChange={(e) => setForm({ ...form, confirm: e.target.value })} />
        <button className="button primary wide" disabled={busy || Boolean(passwordError)}>{busy ? 'Creating account…' : 'Create account'}</button>
      </form>
      <p className="auth-switch">Already have an account? <Link to="/login">Sign in</Link></p>
    </AuthLayout>
  )
}

function OtpBoxes({ value, onChange }) {
  const update = (event) => onChange(event.target.value.replace(/\D/g, '').slice(0, 6))
  return (
    <label className="field otp-field">
      <span>Six-digit code</span>
      <input inputMode="numeric" pattern="\d{6}" maxLength="6" value={value} onChange={update} autoFocus required aria-label="Six-digit verification code" />
    </label>
  )
}

export function VerifyEmailPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState(sessionStorage.getItem('verification_email') || '')
  const [otpCode, setOtpCode] = useState('')
  const [busy, setBusy] = useState(false)
  const [cooldown, setCooldown] = useState(0)

  useEffect(() => {
    if (!cooldown) return undefined
    const timer = window.setInterval(() => setCooldown((value) => Math.max(0, value - 1)), 1000)
    return () => window.clearInterval(timer)
  }, [cooldown])

  const verify = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      await authService.verifyEmail({ email, otpCode })
      sessionStorage.removeItem('verification_email')
      toast.success('Email verified. You can now sign in.')
      navigate('/login')
    } catch (error) {
      toast.error(apiError(error, 'The code could not be verified'))
    } finally {
      setBusy(false)
    }
  }

  const resend = async () => {
    if (!email) return toast.error('Enter your email address')
    try {
      await authService.resendVerification(email)
      setCooldown(60)
      toast.success('A new code has been sent')
    } catch (error) {
      toast.error(apiError(error, 'A new code could not be sent'))
    }
  }

  return (
    <AuthLayout title="Verify your email" subtitle="Enter the code sent to your inbox. It is valid for 10 minutes.">
      <form className="auth-form" onSubmit={verify}>
        <label className="field"><span>Email address</span><input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></label>
        <OtpBoxes value={otpCode} onChange={setOtpCode} />
        <button className="button primary wide" disabled={busy || otpCode.length !== 6}>{busy ? 'Verifying…' : 'Verify account'}</button>
        <button className="button text wide" type="button" disabled={cooldown > 0} onClick={resend}>
          {cooldown ? `Send again in ${cooldown}s` : 'Send a new code'}
        </button>
      </form>
      <p className="auth-switch"><Link to="/login"><ArrowLeft size={15} /> Back to sign in</Link></p>
    </AuthLayout>
  )
}

export function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      const result = await authService.forgotPassword(email)
      sessionStorage.setItem('reset_email', email.trim().toLowerCase())
      toast.success(result.message)
      navigate('/verify-reset')
    } catch (error) {
      toast.error(apiError(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <AuthLayout title="Reset your password" subtitle="We’ll send a secure code if an active account matches your email.">
      <form className="auth-form" onSubmit={submit}>
        <label className="field"><span>Email address</span><input type="email" required autoFocus value={email} onChange={(e) => setEmail(e.target.value)} /></label>
        <button className="button primary wide" disabled={busy}>{busy ? 'Sending code…' : 'Send reset code'}</button>
      </form>
      <p className="auth-switch"><Link to="/login"><ArrowLeft size={15} /> Back to sign in</Link></p>
    </AuthLayout>
  )
}

export function VerifyResetPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState(sessionStorage.getItem('reset_email') || '')
  const [otpCode, setOtpCode] = useState('')
  const [busy, setBusy] = useState(false)
  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      const result = await authService.verifyResetOtp({ email, otpCode })
      sessionStorage.setItem('reset_email', email.trim().toLowerCase())
      sessionStorage.setItem('reset_token', result.resetToken)
      navigate('/reset-password')
    } catch (error) {
      toast.error(apiError(error, 'The code could not be verified'))
    } finally {
      setBusy(false)
    }
  }
  return (
    <AuthLayout title="Check your inbox" subtitle="Enter the six-digit password reset code.">
      <form className="auth-form" onSubmit={submit}>
        <label className="field"><span>Email address</span><input type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></label>
        <OtpBoxes value={otpCode} onChange={setOtpCode} />
        <button className="button primary wide" disabled={busy || otpCode.length !== 6}>{busy ? 'Checking code…' : 'Continue'}</button>
      </form>
    </AuthLayout>
  )
}

export function ResetPasswordPage() {
  const navigate = useNavigate()
  const email = sessionStorage.getItem('reset_email')
  const resetToken = sessionStorage.getItem('reset_token')
  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' })
  const [busy, setBusy] = useState(false)
  if (!email || !resetToken) return <Navigate to="/forgot-password" replace />

  const submit = async (event) => {
    event.preventDefault()
    if (form.newPassword !== form.confirmPassword) return toast.error('Passwords do not match')
    setBusy(true)
    try {
      await authService.resetPassword({ email, resetToken, ...form })
      sessionStorage.removeItem('reset_email')
      sessionStorage.removeItem('reset_token')
      toast.success('Password changed successfully')
      navigate('/login')
    } catch (error) {
      toast.error(apiError(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <AuthLayout title="Choose a new password" subtitle="Use a strong password you haven’t used before.">
      <form className="auth-form" onSubmit={submit}>
        <PasswordInput label="New password" autoComplete="new-password" value={form.newPassword} onChange={(e) => setForm({ ...form, newPassword: e.target.value })} />
        <PasswordInput label="Confirm new password" autoComplete="new-password" value={form.confirmPassword} onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })} />
        <button className="button primary wide" disabled={busy}>{busy ? 'Saving password…' : 'Save new password'}</button>
      </form>
    </AuthLayout>
  )
}

export function ChangePasswordPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [busy, setBusy] = useState(false)
  const submit = async (event) => {
    event.preventDefault()
    if (form.newPassword !== form.confirmPassword) return toast.error('Passwords do not match')
    setBusy(true)
    try {
      await authService.changePassword(form)
      toast.success('Password changed successfully')
      navigate('/profile')
    } catch (error) {
      toast.error(apiError(error))
    } finally {
      setBusy(false)
    }
  }
  return (
    <section className="content-narrow">
      <button className="back-link" onClick={() => navigate(-1)}><ArrowLeft size={16} />Back</button>
      <div className="panel form-panel">
        <div className="panel-heading"><h1>Change password</h1><p>Confirm your current password before choosing a new one.</p></div>
        <form className="stack-form" onSubmit={submit}>
          <PasswordInput label="Current password" value={form.currentPassword} onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} />
          <PasswordInput label="New password" autoComplete="new-password" value={form.newPassword} onChange={(e) => setForm({ ...form, newPassword: e.target.value })} />
          <PasswordInput label="Confirm new password" autoComplete="new-password" value={form.confirmPassword} onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })} />
          <div className="form-actions"><button className="button secondary" type="button" onClick={() => navigate(-1)}>Cancel</button><button className="button primary" disabled={busy}>{busy ? 'Saving…' : 'Change password'}</button></div>
        </form>
      </div>
    </section>
  )
}
