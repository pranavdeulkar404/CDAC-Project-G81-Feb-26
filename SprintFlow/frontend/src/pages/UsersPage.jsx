import { Search, Shield, UserCheck, UserX } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import { apiError } from '../api/client'
import { userService } from '../api/services'
import { Badge, Empty, ErrorState, formatDate, Loading, PageHeader, Pagination } from '../components/Ui'

export default function UsersPage() {
  const [data, setData] = useState(null)
  const [search, setSearch] = useState('')
  const [role, setRole] = useState('')
  const [page, setPage] = useState(0)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setError('')
      setData(await userService.list({ search, role: role || undefined, page, size: 10 }))
    } catch (requestError) {
      setError(apiError(requestError))
    }
  }, [search, role, page])
  useEffect(() => {
    const timer = window.setTimeout(load, 250)
    return () => window.clearTimeout(timer)
  }, [load])

  const updateRole = async (user, nextRole) => {
    try {
      await userService.updateRole(user.id, nextRole)
      toast.success(`${user.name} is now a ${nextRole.toLowerCase()}`)
      load()
    } catch (requestError) { toast.error(apiError(requestError)) }
  }
  const toggle = async (user) => {
    try {
      await userService.updateStatus(user.id, !user.accountEnabled)
      toast.success(`${user.name}'s account ${user.accountEnabled ? 'deactivated' : 'activated'}`)
      load()
    } catch (requestError) { toast.error(apiError(requestError)) }
  }

  return (
    <>
      <PageHeader eyebrow="Administration" title="People" description="Manage account access and team responsibilities." />
      <section className="panel table-panel">
        <div className="toolbar">
          <label className="search-box"><Search size={18} /><input placeholder="Search by name or email" value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }} /></label>
          <select aria-label="Filter by role" value={role} onChange={(e) => { setRole(e.target.value); setPage(0) }}>
            <option value="">All roles</option><option>ADMIN</option><option>MANAGER</option><option>MEMBER</option>
          </select>
        </div>
        {error ? <ErrorState message={error} retry={load} /> : !data ? <Loading /> : data.content.length === 0 ? <Empty title="No people found" /> : (
          <>
            <div className="table-wrap">
              <table>
                <thead><tr><th>Person</th><th>Role</th><th>Status</th><th>Joined</th><th><span className="sr-only">Actions</span></th></tr></thead>
                <tbody>{data.content.map((user) => (
                  <tr key={user.id}>
                    <td><div className="person-cell"><span className="avatar small">{initials(user.name)}</span><div><strong>{user.name}</strong><small>{user.email}</small></div></div></td>
                    <td><select aria-label={`Role for ${user.name}`} value={user.role} onChange={(e) => updateRole(user, e.target.value)}><option>ADMIN</option><option>MANAGER</option><option>MEMBER</option></select></td>
                    <td><Badge value={user.accountEnabled ? 'Active' : 'Deactivated'} tone={user.accountEnabled ? 'success' : 'neutral'} /></td>
                    <td>{formatDate(user.createdAt)}</td>
                    <td><button className="button text compact" onClick={() => toggle(user)}>{user.accountEnabled ? <UserX size={16} /> : <UserCheck size={16} />}{user.accountEnabled ? 'Deactivate' : 'Activate'}</button></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} onPage={setPage} />
          </>
        )}
      </section>
    </>
  )
}

const initials = (name) => name.split(' ').slice(0, 2).map((part) => part[0]).join('').toUpperCase()
