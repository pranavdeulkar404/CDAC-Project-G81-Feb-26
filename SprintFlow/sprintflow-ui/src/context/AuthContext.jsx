import { createContext, useContext, useMemo, useState } from 'react'
import { authService } from '../api/services'

const AuthContext = createContext(null)

function storedUser() {
  try {
    return JSON.parse(localStorage.getItem('sprintflow_user'))
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(storedUser)

  const login = async (credentials) => {
    const result = await authService.login(credentials)
    localStorage.setItem('sprintflow_token', result.token)
    localStorage.setItem('sprintflow_user', JSON.stringify(result.user))
    setUser(result.user)
    return result
  }

  const logout = () => {
    localStorage.removeItem('sprintflow_token')
    localStorage.removeItem('sprintflow_user')
    setUser(null)
  }

  const value = useMemo(
    () => ({
      user,
      login,
      logout,
      isAuthenticated: Boolean(user && localStorage.getItem('sprintflow_token')),
      canManage: user?.role === 'ADMIN' || user?.role === 'MANAGER',
      isAdmin: user?.role === 'ADMIN',
    }),
    [user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
