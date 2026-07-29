import api from './client'

const body = (promise) => promise.then((response) => response.data)

export const authService = {
  register: (data) => body(api.post('/auth/register', data)),
  verifyEmail: (data) => body(api.post('/auth/verify-email', data)),
  resendVerification: (email) => body(api.post('/auth/resend-verification-otp', { email })),
  login: (data) => body(api.post('/auth/login', data)),
  forgotPassword: (email) => body(api.post('/auth/forgot-password', { email })),
  verifyResetOtp: (data) => body(api.post('/auth/verify-reset-otp', data)),
  resetPassword: (data) => body(api.post('/auth/reset-password', data)),
  changePassword: (data) => body(api.post('/auth/change-password', data)),
}

export const projectService = {
  list: (params) => body(api.get('/projects', { params })),
  get: (id) => body(api.get(`/projects/${id}`)),
  create: (data) => body(api.post('/projects', data)),
  update: (id, data) => body(api.put(`/projects/${id}`, data)),
  remove: (id) => api.delete(`/projects/${id}`),
}

export const taskService = {
  list: (params) => body(api.get('/tasks', { params })),
  get: (id) => body(api.get(`/tasks/${id}`)),
  create: (data) => body(api.post('/tasks', data)),
  update: (id, data) => body(api.put(`/tasks/${id}`, data)),
  updateStatus: (id, status) => body(api.patch(`/tasks/${id}/status`, { status })),
  remove: (id) => api.delete(`/tasks/${id}`),
  comments: (id) => body(api.get(`/tasks/${id}/comments`)),
  addComment: (id, message) => body(api.post(`/tasks/${id}/comments`, { message })),
}

export const bugService = {
  list: (params) => body(api.get('/bugs', { params })),
  get: (id) => body(api.get(`/bugs/${id}`)),
  create: (data) => body(api.post('/bugs', data)),
  update: (id, data) => body(api.put(`/bugs/${id}`, data)),
  updateStatus: (id, status) => body(api.patch(`/bugs/${id}/status`, { status })),
  remove: (id) => api.delete(`/bugs/${id}`),
  comments: (id) => body(api.get(`/bugs/${id}/comments`)),
  addComment: (id, message) => body(api.post(`/bugs/${id}/comments`, { message })),
}

export const userService = {
  list: (params) => body(api.get('/users', { params })),
  assignable: () => body(api.get('/users/assignable')),
  updateRole: (id, role) => body(api.patch(`/users/${id}/role`, { role })),
  updateStatus: (id, enabled) => body(api.patch(`/users/${id}/status`, { enabled })),
  profile: () => body(api.get('/users/me/profile')),
  updateProfile: (data) => body(api.put('/users/me/profile', data)),
}

export const notificationService = {
  list: (params) => body(api.get('/notifications', { params })),
  unreadCount: () => body(api.get('/notifications/unread-count')),
  markRead: (id) => body(api.patch(`/notifications/${id}/read`)),
  markAllRead: () => body(api.patch('/notifications/read-all')),
  remove: (id) => api.delete(`/notifications/${id}`),
}

export const dashboardService = {
  get: () => body(api.get('/dashboard')),
}

export const commentService = {
  remove: (id) => api.delete(`/comments/${id}`),
}
