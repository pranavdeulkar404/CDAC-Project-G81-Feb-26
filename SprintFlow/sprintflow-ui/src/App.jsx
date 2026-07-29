import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import AppShell from './components/AppShell'
import ProtectedRoute from './components/ProtectedRoute'
import {
  ChangePasswordPage,
  ForgotPasswordPage,
  LoginPage,
  RegisterPage,
  ResetPasswordPage,
  VerifyEmailPage,
  VerifyResetPage,
} from './pages/AuthPages'
import DashboardPage from './pages/DashboardPage'
import NotificationsPage from './pages/NotificationsPage'
import ProfilePage from './pages/ProfilePage'
import { ProjectDetailsPage, ProjectFormPage, ProjectsPage } from './pages/ProjectsPages'
import { AccessDeniedPage, NotFoundPage } from './pages/StaticPages'
import UsersPage from './pages/UsersPage'
import { WorkItemDetailsPage, WorkItemFormPage, WorkItemsPage } from './pages/WorkItemPages'

const managerRoles = ['ADMIN', 'MANAGER']

function ProjectWorkRedirect({ type }) {
  const { id } = useParams()
  return <Navigate to={`/${type}?projectId=${id}`} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/verify-reset" element={<VerifyResetPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppShell />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/projects" element={<ProjectsPage />} />
          <Route path="/projects/:id" element={<ProjectDetailsPage />} />
          <Route path="/projects/:id/tasks" element={<ProjectWorkRedirect type="tasks" />} />
          <Route path="/projects/:id/bugs" element={<ProjectWorkRedirect type="bugs" />} />
          <Route path="/tasks" element={<WorkItemsPage type="tasks" />} />
          <Route path="/tasks/:id" element={<WorkItemDetailsPage type="tasks" />} />
          <Route path="/bugs" element={<WorkItemsPage type="bugs" />} />
          <Route path="/bugs/:id" element={<WorkItemDetailsPage type="bugs" />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/change-password" element={<ChangePasswordPage />} />
          <Route path="/access-denied" element={<AccessDeniedPage />} />

          <Route element={<ProtectedRoute roles={managerRoles} />}>
            <Route path="/projects/new" element={<ProjectFormPage />} />
            <Route path="/projects/:id/edit" element={<ProjectFormPage />} />
            <Route path="/tasks/new" element={<WorkItemFormPage type="tasks" />} />
            <Route path="/tasks/:id/edit" element={<WorkItemFormPage type="tasks" />} />
            <Route path="/bugs/new" element={<WorkItemFormPage type="bugs" />} />
            <Route path="/bugs/:id/edit" element={<WorkItemFormPage type="bugs" />} />
          </Route>

          <Route element={<ProtectedRoute roles={['ADMIN']} />}>
            <Route path="/users" element={<UsersPage />} />
          </Route>

          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
