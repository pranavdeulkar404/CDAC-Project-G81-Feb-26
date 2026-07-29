import { ArrowLeft, Bug, CalendarDays, Edit3, FolderKanban, ListChecks, Plus, Search, Trash2 } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { apiError } from '../api/client'
import { bugService, projectService, taskService } from '../api/services'
import { Badge, ConfirmDialog, Empty, ErrorState, formatDate, Loading, PageHeader, Pagination } from '../components/Ui'
import { useAuth } from '../context/AuthContext'

const statuses = ['PLANNED', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED']

export function ProjectsPage() {
  const { canManage } = useAuth()
  const [params] = useSearchParams()
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState(params.get('status') || '')
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [removeId, setRemoveId] = useState(null)

  const load = useCallback(async () => {
    try {
      setError('')
      setData(await projectService.list({ search, status: status || undefined, page, size: 9 }))
    } catch (requestError) {
      setError(apiError(requestError))
    }
  }, [search, status, page])

  useEffect(() => {
    const timer = window.setTimeout(load, 250)
    return () => window.clearTimeout(timer)
  }, [load])

  const remove = async () => {
    try {
      await projectService.remove(removeId)
      setRemoveId(null)
      toast.success('Project deleted')
      load()
    } catch (requestError) { toast.error(apiError(requestError)) }
  }

  return (
    <>
      <PageHeader
        eyebrow="Planning"
        title="Projects"
        description="See scope, progress, tasks, and reported issues in one place."
        actions={canManage && <Link className="button primary" to="/projects/new"><Plus size={17} />New project</Link>}
      />
      <div className="toolbar standalone">
        <label className="search-box"><Search size={18} /><input placeholder="Search projects" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }} /></label>
        <select aria-label="Filter by project status" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}>
          <option value="">All statuses</option>{statuses.map((item) => <option key={item}>{item}</option>)}
        </select>
      </div>
      {error ? <ErrorState message={error} retry={load} /> : !data ? <Loading /> : data.content.length === 0 ? (
        <Empty title="No projects found" description={search || status ? 'Try changing your search or filter.' : 'Create your first project to begin planning work.'} />
      ) : (
        <>
          <section className="project-grid">
            {data.content.map((project) => (
              <article className="project-card" key={project.id}>
                <Link className="project-card-main" to={`/projects/${project.id}`}>
                  <div className="card-top"><span className="project-symbol"><FolderKanban size={21} /></span><Badge value={project.status} /></div>
                  <h2>{project.title}</h2>
                  <p>{project.description}</p>
                  <div className="project-dates"><CalendarDays size={16} />{formatDate(project.startDate)} — {formatDate(project.endDate)}</div>
                  <div className="project-stats"><span><ListChecks size={16} />{project.taskCount} tasks</span><span><Bug size={16} />{project.bugCount} bugs</span></div>
                </Link>
                {canManage && (
                  <div className="card-actions">
                    <Link className="button text compact" to={`/projects/${project.id}/edit`}><Edit3 size={15} />Edit</Link>
                    <button className="button text compact danger-text" onClick={() => setRemoveId(project.id)}><Trash2 size={15} />Delete</button>
                  </div>
                )}
              </article>
            ))}
          </section>
          <Pagination page={data.page} totalPages={data.totalPages} onPage={setPage} />
        </>
      )}
      <ConfirmDialog open={Boolean(removeId)} title="Delete this project?" message="Only empty projects can be deleted. Projects with work should be archived." confirmLabel="Delete project" danger onConfirm={remove} onClose={() => setRemoveId(null)} />
    </>
  )
}

export function ProjectFormPage() {
  const { id } = useParams()
  const editing = Boolean(id)
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: '', description: '', startDate: new Date().toISOString().slice(0, 10), endDate: '', status: 'PLANNED' })
  const [loading, setLoading] = useState(editing)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (!editing) return
    projectService.get(id).then((data) => {
      setForm({
        title: data.title,
        description: data.description,
        startDate: data.startDate,
        endDate: data.endDate || '',
        status: data.status,
      })
    }).catch((error) => toast.error(apiError(error))).finally(() => setLoading(false))
  }, [editing, id])

  const submit = async (event) => {
    event.preventDefault()
    if (form.endDate && form.endDate < form.startDate) return toast.error('End date cannot be before start date')
    setBusy(true)
    try {
      const payload = { ...form, endDate: form.endDate || null }
      const saved = editing ? await projectService.update(id, payload) : await projectService.create(payload)
      toast.success(editing ? 'Project updated' : 'Project created')
      navigate(`/projects/${saved.id}`)
    } catch (error) {
      toast.error(apiError(error))
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <Loading label="Loading project…" />
  return (
    <section className="content-narrow">
      <button className="back-link" onClick={() => navigate(-1)}><ArrowLeft size={16} />Back to projects</button>
      <div className="panel form-panel">
        <div className="panel-heading"><span className="eyebrow">{editing ? 'Update plan' : 'Start planning'}</span><h1>{editing ? 'Edit project' : 'Create a project'}</h1><p>Define the outcome, timeline, and current state of this work.</p></div>
        <form className="stack-form" onSubmit={submit}>
          <label className="field"><span>Project title</span><input required maxLength="180" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} /></label>
          <label className="field"><span>Description</span><textarea required rows="6" maxLength="4000" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
          <div className="form-grid">
            <label className="field"><span>Start date</span><input type="date" required value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></label>
            <label className="field"><span>Target end date</span><input type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></label>
          </div>
          <label className="field"><span>Status</span><select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>{statuses.map((item) => <option key={item}>{item}</option>)}</select></label>
          <div className="form-actions"><button className="button secondary" type="button" onClick={() => navigate(-1)}>Cancel</button><button className="button primary" disabled={busy}>{busy ? 'Saving…' : editing ? 'Save changes' : 'Create project'}</button></div>
        </form>
      </div>
    </section>
  )
}

export function ProjectDetailsPage() {
  const { id } = useParams()
  const { canManage } = useAuth()
  const navigate = useNavigate()
  const [project, setProject] = useState(null)
  const [tasks, setTasks] = useState([])
  const [bugs, setBugs] = useState([])
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setError('')
      const [projectData, taskData, bugData] = await Promise.all([
        projectService.get(id),
        taskService.list({ projectId: id, page: 0, size: 5 }),
        bugService.list({ projectId: id, page: 0, size: 5 }),
      ])
      setProject(projectData)
      setTasks(taskData.content)
      setBugs(bugData.content)
    } catch (requestError) {
      setError(apiError(requestError))
    }
  }, [id])
  useEffect(() => { load() }, [load])
  if (error) return <ErrorState message={error} retry={load} />
  if (!project) return <Loading label="Loading project…" />

  return (
    <>
      <button className="back-link" onClick={() => navigate('/projects')}><ArrowLeft size={16} />All projects</button>
      <PageHeader
        eyebrow="Project details"
        title={project.title}
        description={project.description}
        actions={canManage && <><Link className="button secondary" to={`/projects/${id}/edit`}><Edit3 size={16} />Edit</Link><Link className="button primary" to={`/tasks/new?projectId=${id}`}><Plus size={16} />Add task</Link></>}
      />
      <section className="detail-meta panel">
        <div><span>Status</span><Badge value={project.status} /></div>
        <div><span>Timeline</span><strong>{formatDate(project.startDate)} — {formatDate(project.endDate)}</strong></div>
        <div><span>Created by</span><strong>{project.createdBy.name}</strong></div>
        <div><span>Last updated</span><strong>{formatDate(project.updatedAt, true)}</strong></div>
      </section>
      <div className="two-column">
        <RelatedWork title="Tasks" icon={ListChecks} items={tasks} type="tasks" projectId={id} canManage={canManage} />
        <RelatedWork title="Bugs" icon={Bug} items={bugs} type="bugs" projectId={id} canManage={canManage} />
      </div>
    </>
  )
}

function RelatedWork({ title, icon: Icon, items, type, projectId, canManage }) {
  return (
    <section className="panel related-panel">
      <div className="panel-heading row"><div><h2><Icon size={20} />{title}</h2><p>Latest work in this project.</p></div>{canManage && <Link className="button text compact" to={`/${type}/new?projectId=${projectId}`}><Plus size={15} />Add</Link>}</div>
      {items.length === 0 ? <Empty title={`No ${type} yet`} /> : (
        <div className="related-list">{items.map((item) => (
          <Link to={`/${type}/${item.id}`} key={item.id}><div><strong>{item.title}</strong><small>{item.assignedTo?.name || 'Unassigned'}</small></div><Badge value={type === 'tasks' ? item.priority : item.severity} /></Link>
        ))}</div>
      )}
      <Link className="view-all-link" to={`/${type}?projectId=${projectId}`}>View all {type}</Link>
    </section>
  )
}
