import {
  Bell, Bug, ChevronDown, FolderKanban, Gauge, KeyRound, LogOut,
  Menu, Settings, UserRound, UsersRound, X, ListChecks,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { notificationService } from '../api/services'
import { useAuth } from '../context/AuthContext'

const navItems = [
  { to: '/dashboard', label: 'Overview', icon: Gauge },
  { to: '/projects', label: 'Projects', icon: FolderKanban },
  { to: '/tasks', label: 'My tasks', icon: ListChecks },
  { to: '/bugs', label: 'My bugs', icon: Bug },
  { to: '/notifications', label: 'Notifications', icon: Bell },
]

export default function AppShell() {
  const { user, logout, isAdmin } = useAuth()
  const navigate = useNavigate()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(
    () => window.localStorage.getItem('sprintflow_sidebar_collapsed') === 'true',
  )
  const [isMobile, setIsMobile] = useState(
    () => window.matchMedia('(max-width: 840px)').matches,
  )
  const [userMenu, setUserMenu] = useState(false)
  const [unread, setUnread] = useState(0)
  const menuRef = useRef(null)

  useEffect(() => {
    const media = window.matchMedia('(max-width: 840px)')
    const handleChange = (event) => {
      setIsMobile(event.matches)
      if (!event.matches) setMobileOpen(false)
    }
    media.addEventListener('change', handleChange)
    return () => media.removeEventListener('change', handleChange)
  }, [])

  useEffect(() => {
    let active = true
    const loadCount = () => notificationService.unreadCount()
      .then((data) => active && setUnread(data.count))
      .catch(() => {})
    loadCount()
    const interval = window.setInterval(loadCount, 30000)
    return () => {
      active = false
      window.clearInterval(interval)
    }
  }, [])

  useEffect(() => {
    const close = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) setUserMenu(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const signOut = () => {
    logout()
    navigate('/login')
  }

  const setDesktopCollapsed = (collapsed) => {
    setSidebarCollapsed(collapsed)
    window.localStorage.setItem('sprintflow_sidebar_collapsed', String(collapsed))
  }

  const closeOrCollapseSidebar = () => {
    if (isMobile) {
      setMobileOpen(false)
    } else {
      setDesktopCollapsed(true)
    }
  }

  const toggleNavigation = () => {
    if (isMobile) {
      setMobileOpen(true)
    } else {
      setDesktopCollapsed(!sidebarCollapsed)
    }
  }

  return (
    <div className="app-frame">
      {mobileOpen && <button className="mobile-overlay" aria-label="Close navigation" onClick={() => setMobileOpen(false)} />}
      <aside className={`sidebar ${mobileOpen ? 'open' : ''} ${sidebarCollapsed && !isMobile ? 'collapsed' : ''}`}>
        {sidebarCollapsed && !isMobile ? (
          <button
            className="brand-square-button"
            onClick={() => setDesktopCollapsed(false)}
            aria-label="Expand navigation"
            title="Expand navigation"
          >
            <img src="/sprintflow-mark.png" alt="" />
          </button>
        ) : (
          <div className="brand">
            <img className="brand-wide-logo" src="/sprintflow-logo.png" alt="SprintFlow" />
            <button
              className="icon-button sidebar-close"
              onClick={closeOrCollapseSidebar}
              aria-label={isMobile ? 'Close navigation' : 'Collapse navigation'}
              title={isMobile ? 'Close navigation' : 'Collapse navigation'}
            >
              <X />
            </button>
          </div>
        )}
        <nav>
          <span className="nav-label">Workspace</span>
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => setMobileOpen(false)}
              aria-label={sidebarCollapsed && !isMobile ? label : undefined}
              title={sidebarCollapsed && !isMobile ? label : undefined}
            >
              <Icon size={19} />
              <span className="nav-text">{label}</span>
              {to === '/notifications' && unread > 0 && <span className="nav-count">{unread > 99 ? '99+' : unread}</span>}
            </NavLink>
          ))}
          {isAdmin && (
            <>
              <span className="nav-label nav-section">Administration</span>
              <NavLink
                to="/users"
                onClick={() => setMobileOpen(false)}
                aria-label={sidebarCollapsed && !isMobile ? 'People' : undefined}
                title={sidebarCollapsed && !isMobile ? 'People' : undefined}
              >
                <UsersRound size={19} /><span className="nav-text">People</span>
              </NavLink>
            </>
          )}
        </nav>
        <div className="sidebar-footer">
          <span>Signed in as</span>
          <strong>{user?.role?.toLowerCase()}</strong>
        </div>
      </aside>

      <div className={`app-content ${sidebarCollapsed && !isMobile ? 'sidebar-collapsed' : ''}`}>
        <header className="topbar">
          <button
            className="icon-button menu-toggle"
            onClick={toggleNavigation}
            aria-label={isMobile || sidebarCollapsed ? 'Open navigation' : 'Collapse navigation'}
            title={isMobile || sidebarCollapsed ? 'Open navigation' : 'Collapse navigation'}
          >
            <Menu />
          </button>
          <div className="topbar-spacer" />
          <button className="notification-button" aria-label={`${unread} unread notifications`} onClick={() => navigate('/notifications')}>
            <Bell size={20} />
            {unread > 0 && <span>{unread > 9 ? '9+' : unread}</span>}
          </button>
          <div className="user-menu" ref={menuRef}>
            <button className="user-trigger" onClick={() => setUserMenu((value) => !value)} aria-expanded={userMenu}>
              <span className="avatar">{user?.name?.split(' ').slice(0, 2).map((part) => part[0]).join('').toUpperCase()}</span>
              <span className="user-copy"><strong>{user?.name}</strong><small>{user?.email}</small></span>
              <ChevronDown size={16} />
            </button>
            {userMenu && (
              <div className="menu-popover">
                <button onClick={() => navigate('/profile')}><UserRound size={17} />Profile</button>
                <button onClick={() => navigate('/change-password')}><KeyRound size={17} />Change password</button>
                <button onClick={() => navigate('/profile')}><Settings size={17} />Preferences</button>
                <hr />
                <button onClick={signOut}><LogOut size={17} />Sign out</button>
              </div>
            )}
          </div>
        </header>
        <main className="main-content"><Outlet /></main>
      </div>
    </div>
  )
}
