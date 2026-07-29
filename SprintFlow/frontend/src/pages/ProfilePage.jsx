import { KeyRound, Mail, Pencil, Phone, Save, UserRound, X } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { Link } from 'react-router-dom'
import { apiError } from '../api/client'
import { userService } from '../api/services'
import { ErrorState, Loading, PageHeader } from '../components/Ui'

export default function ProfilePage() {
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState(null)
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setError('')
      const data = await userService.profile()
      setProfile(data)
      setForm({ phone: data.phone || '', designation: data.designation || '', bio: data.bio || '' })
    } catch (requestError) {
      setError(apiError(requestError))
    }
  }, [])
  useEffect(() => { load() }, [load])

  const save = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      setProfile(await userService.updateProfile(form))
      setEditing(false)
      toast.success('Profile updated')
    } catch (requestError) {
      toast.error(apiError(requestError))
    } finally {
      setBusy(false)
    }
  }

  if (error) return <ErrorState message={error} retry={load} />
  if (!profile) return <Loading />

  return (
    <>
      <PageHeader eyebrow="Personal settings" title="Your profile" description="Keep your contact and team details up to date." />
      <div className="profile-layout">
        <section className="panel profile-summary">
          <span className="profile-avatar">{profile.initials}</span>
          <h2>{profile.name}</h2>
          <p>{profile.designation || 'SprintFlow team member'}</p>
          <div className="profile-contact"><Mail size={17} />{profile.email}</div>
          <div className="profile-contact"><Phone size={17} />{profile.phone || 'No phone added'}</div>
          <Link className="button secondary wide" to="/change-password"><KeyRound size={17} />Change password</Link>
        </section>
        <section className="panel form-panel">
          <div className="panel-heading row">
            <div><h2>Profile details</h2><p>This information helps your teammates know your role.</p></div>
            {!editing && <button className="button secondary" onClick={() => setEditing(true)}><Pencil size={16} />Edit</button>}
          </div>
          {editing ? (
            <form className="stack-form" onSubmit={save}>
              <label className="field"><span>Phone number</span><input value={form.phone} maxLength="25" onChange={(e) => setForm({ ...form, phone: e.target.value })} /></label>
              <label className="field"><span>Designation</span><input value={form.designation} maxLength="100" onChange={(e) => setForm({ ...form, designation: e.target.value })} /></label>
              <label className="field"><span>Short bio</span><textarea rows="6" maxLength="1000" value={form.bio} onChange={(e) => setForm({ ...form, bio: e.target.value })} /></label>
              <div className="form-actions">
                <button className="button secondary" type="button" onClick={() => { setEditing(false); load() }}><X size={16} />Cancel</button>
                <button className="button primary" disabled={busy}><Save size={16} />{busy ? 'Saving…' : 'Save profile'}</button>
              </div>
            </form>
          ) : (
            <dl className="detail-list">
              <div><dt><UserRound size={17} />Full name</dt><dd>{profile.name}</dd></div>
              <div><dt>Designation</dt><dd>{profile.designation || 'Not added'}</dd></div>
              <div><dt>Phone</dt><dd>{profile.phone || 'Not added'}</dd></div>
              <div className="full"><dt>Bio</dt><dd>{profile.bio || 'Tell your team a little about your work and responsibilities.'}</dd></div>
            </dl>
          )}
        </section>
      </div>
    </>
  )
}
