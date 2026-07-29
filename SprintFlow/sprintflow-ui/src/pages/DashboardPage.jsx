import { AlertTriangle, ArrowRight, BellRing, Bug, CalendarClock, CheckCircle2, FolderKanban, ListChecks } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiError } from '../api/client'
import { dashboardService } from '../api/services'
import { Badge, Empty, ErrorState, formatDate, Loading, PageHeader } from '../components/Ui'
import { useAuth } from '../context/AuthContext'

const statusLabels = {
  TODO: 'To do',
  IN_PROGRESS: 'In progress',
  BLOCKED: 'Blocked',
  IN_REVIEW: 'In review',
  COMPLETED: 'Completed',
  OPEN: 'Open',
  RESOLVED: 'Resolved',
  VERIFIED: 'Verified',
  CLOSED: 'Closed',
  REOPENED: 'Reopened',
}

export default function DashboardPage() {
  const { user } = useAuth()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setError('')
    try {
      setData(await dashboardService.get())
    } catch (requestError) {
      setError(apiError(requestError))
    }
  }, [])

  useEffect(() => { load() }, [load])
  if (!data && !error) return <Loading />
  if (error) return <ErrorState message={error} retry={load} />

  const cards = [
    { label: 'Active projects', value: data.activeProjects, icon: FolderKanban, tone: 'blue', to: '/projects?status=ACTIVE' },
    { label: 'Assigned tasks', value: data.assignedTasks, icon: ListChecks, tone: 'violet', to: '/tasks' },
    { label: 'Open bugs', value: data.openAssignedBugs, icon: Bug, tone: 'orange', to: '/bugs' },
    { label: 'Overdue tasks', value: data.overdueTasks, icon: AlertTriangle, tone: 'red', to: '/tasks?overdue=true' },
  ]

  return (
    <>
      <PageHeader
        eyebrow="Workspace overview"
        title={`Good to see you, ${user.name.split(' ')[0]}`}
        description="Here’s what needs attention across your work today."
      />
      <section className="metric-grid">
        {cards.map(({ label, value, icon: Icon, tone, to }) => (
          <Link className="metric-card" to={to} key={label}>
            <span className={`metric-icon ${tone}`}><Icon size={21} /></span>
            <div><span>{label}</span><strong>{value}</strong></div>
            <ArrowRight size={17} />
          </Link>
        ))}
      </section>

      {(data.highPriorityBugs > 0 || data.unreadNotifications > 0) && (
        <section className="attention-strip">
          <div><AlertTriangle size={20} /><span><strong>{data.highPriorityBugs}</strong> high or critical bugs need attention</span></div>
          <div><BellRing size={20} /><span><strong>{data.unreadNotifications}</strong> unread updates</span></div>
        </section>
      )}

      <div className="dashboard-grid">
        <section className="panel">
          <div className="panel-heading row"><div><h2>My work by status</h2><p>Live counts for your assigned tasks.</p></div><Link to="/tasks">View tasks</Link></div>
          <StatusBars values={data.tasksByStatus} />
        </section>
        <section className="panel">
          <div className="panel-heading row"><div><h2>Upcoming due dates</h2><p>Your next five task deadlines.</p></div><CalendarClock size={22} /></div>
          {data.upcomingDueDates.length === 0 ? (
            <Empty title="No upcoming deadlines" description="Assigned tasks with due dates will appear here." />
          ) : (
            <div className="timeline-list">
              {data.upcomingDueDates.map((item) => (
                <Link to={`/tasks/${item.id}`} key={item.id}>
                  <span className="date-tile">{new Date(item.dueDate).getDate()}<small>{new Date(item.dueDate).toLocaleString('en', { month: 'short' })}</small></span>
                  <div><strong>{item.title}</strong><small>{item.projectTitle}</small></div>
                  <Badge value={item.priority} />
                </Link>
              ))}
            </div>
          )}
        </section>
        <section className="panel activity-panel">
          <div className="panel-heading row"><div><h2>Recent activity</h2><p>Your latest SprintFlow updates.</p></div><Link to="/notifications">View all</Link></div>
          {data.recentActivity.length === 0 ? (
            <Empty title="No recent activity" />
          ) : (
            <div className="activity-list">
              {data.recentActivity.map((item) => (
                <Link to={notificationLink(item)} key={item.id}>
                  <span className={`activity-dot ${item.read ? '' : 'unread'}`}><CheckCircle2 size={16} /></span>
                  <div><p>{item.message}</p><small>{formatDate(item.createdAt, true)}</small></div>
                </Link>
              ))}
            </div>
          )}
        </section>
        <section className="panel">
          <div className="panel-heading row"><div><h2>Assigned bugs by status</h2><p>Where your reported issues stand.</p></div><Bug size={22} /></div>
          <StatusBars values={data.bugsByStatus} />
        </section>
      </div>
    </>
  )
}

function StatusBars({ values }) {
  const entries = Object.entries(values)
  const max = Math.max(...entries.map(([, value]) => value), 1)
  return (
    <div className="status-bars">
      {entries.map(([status, value]) => (
        <div className="status-row" key={status}>
          <span>{statusLabels[status] || status.replaceAll('_', ' ')}</span>
          <div><i style={{ width: `${Math.max(value ? 8 : 0, (value / max) * 100)}%` }} /></div>
          <strong>{value}</strong>
        </div>
      ))}
    </div>
  )
}

function notificationLink(item) {
  if (!item.referenceType || !item.referenceId) return '/notifications'
  return `/${item.referenceType.toLowerCase()}s/${item.referenceId}`
}
