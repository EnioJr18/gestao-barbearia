import { useEffect, useState, type ReactNode } from 'react'
import { Navigate } from 'react-router-dom'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const [hasToken, setHasToken] = useState(() =>
    Boolean(localStorage.getItem('token')),
  )

  useEffect(() => {
    const checkToken = () => setHasToken(Boolean(localStorage.getItem('token')))
    const handleStorage = (event: StorageEvent) => {
      if (event.key === 'token') checkToken()
    }

    window.addEventListener('storage', handleStorage)
    window.addEventListener('auth:logout', checkToken)

    return () => {
      window.removeEventListener('storage', handleStorage)
      window.removeEventListener('auth:logout', checkToken)
    }
  }, [])

  if (!hasToken) {
    return <Navigate to="/login" replace />
  }

  return children
}
