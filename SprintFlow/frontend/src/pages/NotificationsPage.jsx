import { Bell, CheckCheck, Trash2 } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { useNavigate } from 'react-router-dom'
import { apiError } from '../api/client'
import { notificationService } from '../api/services'
import { ConfirmDialog, Empty, ErrorState, formatDate, Loading, PageHeader, Pagination } from '../components/Ui'

export default function NotificationsPage() {
  const navigate = useNavigate()
  const [data, setData] = useState(null)
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')
  const [removeId, setRemoveId] = useState(null)

  const load = useCallback(async () => {
    try {
      setError('')
      setData(await notificationService.list({ page, size: 15 }))
    } catch (requestError) {
      setError(apiError(requestError))
    }
  }, [page])

  useEffect(() => { load() }, [load])

  const openItem = async (item) => {
    if (!item.read) await notificationService.markRead(item.id)
    if (item.referenceType && item.referenceId) {
      navigate(`/${item.referenceType.toLowerCase()}s/${item.referenceId}`)
    } else {
      load()
    }
  }

  const markAll = async () => {
    await notificationService.markAllRead()
    toast.success('All notifications marked as read')
    load()
  }

  const remove = async () => {
    try {
      await notificationService.remove(removeId)
      setRemoveId(null)
      toast.success('Notification removed')
      load()
    } catch (requestError) {
      toast.error(apiError(requestError))
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Inbox"
        title="Notifications"
        description="A history of assignments, comments, and meaningful work updates."
        actions={<button className="button secondary" onClick={markAll}><CheckCheck size={17} />Mark all read</button>}
      />
      {error ? <ErrorState message={error} retry={load} /> : !data ? <Loading /> : (
        <section className="panel notification-list">
          {data.content.length === 0 ? <Empty title="You’re all caught up" description="New team updates will appear here." /> : data.content.map((item) => (
            <div key={item.id} className={`notification-row ${item.read ? '' : 'unread'}`}>
              <button className="notification-main" onClick={() => openItem(item)}>
                <span className="notification-icon"><Bell size={18} /></span>
                <span><strong>{item.message}</strong><small>{formatDate(item.createdAt, true)}</small></span>
                {!item.read && <i />}
              </button>
              <button className="icon-button" aria-label="Delete notification" onClick={() => setRemoveId(item.id)}><Trash2 size={17} /></button>
            </div>
          ))}
          <Pagination page={data.page} totalPages={data.totalPages} onPage={setPage} />
        </section>
      )}
      <ConfirmDialog open={Boolean(removeId)} title="Remove notification?" message="This notification will be removed from your history." confirmLabel="Remove" danger onConfirm={remove} onClose={() => setRemoveId(null)} />
    </>
  )
}
