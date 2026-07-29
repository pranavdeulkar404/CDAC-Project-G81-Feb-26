import { ArrowLeft, Bug, Calendar, Edit3, ListChecks, MessageSquare, Plus, Search, Send, Trash2, UserRound } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { apiError } from '../api/client'
import { bugService, commentService, projectService, taskService, userService } from '../api/services'
import { Badge, ConfirmDialog, Empty, ErrorState, formatDate, Loading, PageHeader, Pagination } from '../components/Ui'
import { useAuth } from '../context/AuthContext'

const TASK_STATUSES = ['TODO', 'IN_PROGRESS', 'BLOCKED', 'IN_REVIEW', 'COMPLETED']
const BUG_STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED', 'REOPENED']
const LEVELS = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']

const configFor = (type) => type === 'tasks'
  ? {
      singular: 'task',
      title: 'Tasks',
      eyebrow: 'Delivery',
      description: 'Plan, assign, and follow work through to completion.',
      icon: ListChecks,
      service: taskService,
      level: 'priority',
      statuses: TASK_STATUSES,
    }
  : {
      singular: 'bug',
      title: 'Bugs',
      eyebrow: 'Quality',
      description: 'Track reported issues from discovery through verification.',
      icon: Bug,
      service: bugService,
      level: 'severity',
      statuses: BUG_STATUSES,
    }

export function WorkItemsPage({ type }) {
  const config = configFor(type)
  const { canManage } = useAuth()
  const [query] = useSearchParams()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [level, setLevel] = useState('')
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [removeId, setRemoveId] = useState(null)
  const projectId = query.get('projectId') || undefined

  const load = useCallback(async () => {
    try {
      setError('')
      setData(await config.service.list({
        search,
        status: status || undefined,
        [config.level]: level || undefined,
        projectId,
        page,
        size: 10,
      }))
    } catch (requestError) {
      setError(apiError(requestError))
    }
  }, [config.level, config.service, search, status, level, projectId, page])

  useEffect(() => {
    const timer = window.setTimeout(load, 250)
    return () => window.clearTimeout(timer)
  }, [load])

  const remove = async () => {
    try {
      await config.service.remove(removeId)
      toast.success(`${capitalize(config.singular)} deleted`)
      setRemoveId(null)
      load()
    } catch (requestError) { toast.error(apiError(requestError)) }
  }

  return (
    <>
      <PageHeader
        eyebrow={config.eyebrow}
        title={config.title}
        description={config.description}
        actions={canManage && <Link className="button primary" to={`/${type}/new${projectId ? `?projectId=${projectId}` : ''}`}><Plus size={17} />New {config.singular}</Link>}
      />
      <section className="panel table-panel">
        <div className="toolbar">
          <label className="search-box"><Search size={18} /><input placeholder={`Search ${type}`} value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }} /></label>
          <select aria-label="Filter by status" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}><option value="">All statuses</option>{config.statuses.map((item) => <option key={item}>{item}</option>)}</select>
          <select aria-label={`Filter by ${config.level}`} value={level} onChange={(e) => { setLevel(e.target.value); setPage(0) }}><option value="">All {config.level} levels</option>{LEVELS.map((item) => <option key={item}>{item}</option>)}</select>
        </div>
        {error ? <ErrorState message={error} retry={load} /> : !data ? <Loading /> : data.content.length === 0 ? (
          <Empty title={`No ${type} found`} description="Try changing the filters, or add the first item." />
        ) : (
          <>
            <div className="table-wrap">
              <table>
                <thead><tr><th>{capitalize(config.singular)}</th><th>Project</th><th>{capitalize(config.level)}</th><th>Status</th><th>Assignee</th>{type === 'tasks' && <th>Due</th>}<th><span className="sr-only">Actions</span></th></tr></thead>
                <tbody>{data.content.map((item) => (
                  <tr key={item.id}>
                    <td><Link className="item-title" to={`/${type}/${item.id}`}><strong>{item.title}</strong><small>{truncate(item.description, 60)}</small></Link></td>
                    <td><Link to={`/projects/${item.projectId}`}>{item.projectTitle}</Link></td>
                    <td><Badge value={item[config.level]} /></td>
                    <td><Badge value={item.status} /></td>
                    <td>{item.assignedTo ? <span className="assignee"><span className="avatar tiny">{initials(item.assignedTo.name)}</span>{item.assignedTo.name}</span> : <span className="muted">Unassigned</span>}</td>
                    {type === 'tasks' && <td>{formatDate(item.dueDate)}</td>}
                    <td>{canManage && <button className="icon-button" aria-label={`Delete ${item.title}`} onClick={() => setRemoveId(item.id)}><Trash2 size={17} /></button>}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} onPage={setPage} />
          </>
        )}
      </section>
      <ConfirmDialog open={Boolean(removeId)} title={`Delete this ${config.singular}?`} message={`${capitalize(config.singular)}s with comments cannot be deleted. This action cannot be undone.`} confirmLabel={`Delete ${config.singular}`} danger onConfirm={remove} onClose={() => setRemoveId(null)} />
    </>
  )
}

export function WorkItemFormPage({ type }) {
  const config = configFor(type)
  const { id } = useParams()
  const editing = Boolean(id)
  const navigate = useNavigate()
  const [query] = useSearchParams()
  const initial = type === 'tasks'
    ? { title: '', description: '', priority: 'MEDIUM', status: 'TODO', dueDate: '', projectId: query.get('projectId') || '', assignedToId: '' }
    : { title: '', description: '', severity: 'MEDIUM', status: 'OPEN', projectId: query.get('projectId') || '', assignedToId: '' }
  const [form, setForm] = useState(initial)
  const [projects, setProjects] = useState([])
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const requests = [projectService.list({ page: 0, size: 100 }), userService.assignable()]
    if (editing) requests.push(config.service.get(id))
    Promise.all(requests).then(([projectData, userData, item]) => {
      setProjects(projectData.content)
      setUsers(userData)
      if (item) {
        setForm(type === 'tasks'
          ? { title: item.title, description: item.description, priority: item.priority, status: item.status, dueDate: item.dueDate || '', projectId: String(item.projectId), assignedToId: item.assignedTo?.id ? String(item.assignedTo.id) : '' }
          : { title: item.title, description: item.description, severity: item.severity, status: item.status, projectId: String(item.projectId), assignedToId: item.assignedTo?.id ? String(item.assignedTo.id) : '' })
      }
    }).catch((error) => toast.error(apiError(error))).finally(() => setLoading(false))
  }, [config.service, editing, id, type])

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    try {
      const payload = {
        ...form,
        projectId: Number(form.projectId),
        assignedToId: form.assignedToId ? Number(form.assignedToId) : null,
        ...(type === 'tasks' ? { dueDate: form.dueDate || null } : {}),
      }
      const result = editing ? await config.service.update(id, payload) : await config.service.create(payload)
      toast.success(editing ? `${capitalize(config.singular)} updated` : `${capitalize(config.singular)} created`)
      navigate(`/${type}/${result.id}`)
    } catch (error) {
      toast.error(apiError(error))
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <Loading label={`Loading ${config.singular}…`} />
  return (
    <section className="content-narrow">
      <button className="back-link" onClick={() => navigate(-1)}><ArrowLeft size={16} />Back</button>
      <div className="panel form-panel">
        <div className="panel-heading"><span className="eyebrow">{config.eyebrow}</span><h1>{editing ? `Edit ${config.singular}` : `Create a ${config.singular}`}</h1><p>{type === 'tasks' ? 'Define clear ownership, priority, and a target date.' : 'Describe the issue clearly so it can be reproduced and resolved.'}</p></div>
        <form className="stack-form" onSubmit={submit}>
          <label className="field"><span>Title</span><input required maxLength="180" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label>
          <label className="field"><span>Description</span><textarea required rows="7" maxLength="4000" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
          <div className="form-grid">
            <label className="field"><span>Project</span><select required value={form.projectId} onChange={(e) => setForm({ ...form, projectId: e.target.value })}><option value="">Select a project</option>{projects.map((item) => <option value={item.id} key={item.id}>{item.title}</option>)}</select></label>
            <label className="field"><span>Assign to</span><select value={form.assignedToId} onChange={(e) => setForm({ ...form, assignedToId: e.target.value })}><option value="">Unassigned</option>{users.map((user) => <option value={user.id} key={user.id}>{user.name} — {user.role.toLowerCase()}</option>)}</select></label>
            <label className="field"><span>{capitalize(config.level)}</span><select value={form[config.level]} onChange={(e) => setForm({ ...form, [config.level]: e.target.value })}>{LEVELS.map((item) => <option key={item}>{item}</option>)}</select></label>
            <label className="field"><span>Status</span><select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>{config.statuses.map((item) => <option key={item}>{item}</option>)}</select></label>
            {type === 'tasks' && <label className="field"><span>Due date</span><input type="date" value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} /></label>}
          </div>
          <div className="form-actions"><button className="button secondary" type="button" onClick={() => navigate(-1)}>Cancel</button><button className="button primary" disabled={busy}>{busy ? 'Saving…' : editing ? 'Save changes' : `Create ${config.singular}`}</button></div>
        </form>
      </div>
    </section>
  )
}

export function WorkItemDetailsPage({ type }) {
  const config = configFor(type)
  const { id } = useParams()
  const { user, canManage } = useAuth()
  const navigate = useNavigate()
  const [item, setItem] = useState(null)
  const [comments, setComments] = useState([])
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [confirmDelete, setConfirmDelete] = useState(false)

  const load = useCallback(async () => {
    try {
      setError('')
      const [itemData, commentData] = await Promise.all([config.service.get(id), config.service.comments(id)])
      setItem(itemData)
      setComments(commentData)
    } catch (requestError) {
      setError(apiError(requestError))
    }
  }, [config.service, id])
  useEffect(() => { load() }, [load])

  const changeStatus = async (status) => {
    try {
      setItem(await config.service.updateStatus(id, status))
      toast.success('Status updated')
    } catch (requestError) { toast.error(apiError(requestError)) }
  }
  const addComment = async (event) => {
    event.preventDefault()
    if (!message.trim()) return
    setBusy(true)
    try {
      const saved = await config.service.addComment(id, message)
      setComments([...comments, saved])
      setMessage('')
      toast.success('Comment added')
    } catch (requestError) { toast.error(apiError(requestError)) } finally { setBusy(false) }
  }
  const deleteComment = async (commentId) => {
    try {
      await commentService.remove(commentId)
      setComments(comments.filter((comment) => comment.id !== commentId))
      toast.success('Comment deleted')
    } catch (requestError) { toast.error(apiError(requestError)) }
  }
  const remove = async () => {
    try {
      await config.service.remove(id)
      toast.success(`${capitalize(config.singular)} deleted`)
      navigate(`/${type}`)
    } catch (requestError) { toast.error(apiError(requestError)); setConfirmDelete(false) }
  }

  if (error) return <ErrorState message={error} retry={load} />
  if (!item) return <Loading label={`Loading ${config.singular}…`} />
  const owner = type === 'tasks' ? item.createdBy : item.reportedBy

  return (
    <>
      <button className="back-link" onClick={() => navigate(`/${type}`)}><ArrowLeft size={16} />All {type}</button>
      <PageHeader
        eyebrow={`${item.projectTitle} · ${config.singular}`}
        title={item.title}
        description={item.description}
        actions={canManage && <><Link className="button secondary" to={`/${type}/${id}/edit`}><Edit3 size={16} />Edit</Link><button className="button danger-outline" onClick={() => setConfirmDelete(true)}><Trash2 size={16} />Delete</button></>}
      />
      <div className="work-detail-grid">
        <section className="panel">
          <div className="detail-meta vertical">
            <div><span>Status</span><select value={item.status} onChange={(e) => changeStatus(e.target.value)}>{config.statuses.map((status) => <option key={status}>{status}</option>)}</select></div>
            <div><span>{capitalize(config.level)}</span><Badge value={item[config.level]} /></div>
            <div><span>Project</span><Link to={`/projects/${item.projectId}`}><strong>{item.projectTitle}</strong></Link></div>
            <div><span>Assigned to</span><strong>{item.assignedTo?.name || 'Unassigned'}</strong></div>
            <div><span>{type === 'tasks' ? 'Created by' : 'Reported by'}</span><strong>{owner.name}</strong></div>
            {type === 'tasks' && <div><span>Due date</span><strong>{formatDate(item.dueDate)}</strong></div>}
            <div><span>Last updated</span><strong>{formatDate(item.updatedAt, true)}</strong></div>
          </div>
        </section>
        <section className="panel comments-panel">
          <div className="panel-heading row"><div><h2><MessageSquare size={20} />Discussion</h2><p>Keep decisions and progress with the work.</p></div><span className="comment-count">{comments.length}</span></div>
          <div className="comments">
            {comments.length === 0 ? <Empty title="No comments yet" description="Start the discussion with a helpful update." /> : comments.map((comment) => (
              <article key={comment.id} className="comment">
                <span className="avatar small">{initials(comment.author.name)}</span>
                <div><header><strong>{comment.author.name}</strong><time>{formatDate(comment.createdAt, true)}</time>{(comment.author.id === user.id || user.role === 'ADMIN') && <button className="icon-button" aria-label="Delete comment" onClick={() => deleteComment(comment.id)}><Trash2 size={14} /></button>}</header><p>{comment.message}</p></div>
              </article>
            ))}
          </div>
          <form className="comment-form" onSubmit={addComment}>
            <label><span className="sr-only">Add a comment</span><textarea rows="3" maxLength="2000" placeholder="Add a helpful update…" value={message} onChange={(e) => setMessage(e.target.value)} /></label>
            <button className="button primary" disabled={busy || !message.trim()}><Send size={16} />{busy ? 'Posting…' : 'Post comment'}</button>
          </form>
        </section>
      </div>
      <ConfirmDialog open={confirmDelete} title={`Delete this ${config.singular}?`} message={`${capitalize(config.singular)}s with comments cannot be deleted. This action cannot be undone.`} confirmLabel={`Delete ${config.singular}`} danger onConfirm={remove} onClose={() => setConfirmDelete(false)} />
    </>
  )
}

const capitalize = (value) => value.charAt(0).toUpperCase() + value.slice(1)
const truncate = (value, max) => value.length > max ? `${value.slice(0, max)}…` : value
const initials = (name) => name.split(' ').slice(0, 2).map((part) => part[0]).join('').toUpperCase()
