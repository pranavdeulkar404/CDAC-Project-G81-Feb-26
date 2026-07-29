import axios from 'axios'

const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

const api = axios.create({
  baseURL: API_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('sprintflow_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const path = window.location.pathname
    if (status === 401 && !path.startsWith('/login')) {
      localStorage.removeItem('sprintflow_token')
      localStorage.removeItem('sprintflow_user')
      window.location.assign('/login?session=expired')
    }
    if (status === 403 && path !== '/access-denied') {
      window.location.assign('/access-denied')
    }
    return Promise.reject(error)
  },
)

export function apiError(error, fallback = 'The request could not be completed') {
  const data = error.response?.data
  if (data?.fieldErrors) {
    const first = Object.values(data.fieldErrors)[0]
    if (first) return first
  }
  return data?.message || fallback
}

export default api
